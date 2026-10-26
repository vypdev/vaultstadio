"""Regression contracts for PR test completeness, event cost and publishing boundaries."""
from pathlib import Path
import json
import os
import re
import shlex
import subprocess
import tempfile
import unittest

import yaml

ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS = ROOT / '.github/workflows'


def workflow(name):
    # BaseLoader preserves GitHub's YAML 1.2 `on` key and expression strings.
    return yaml.load((WORKFLOWS / name).read_text(), Loader=yaml.BaseLoader)


def steps(document):
    return [s for job in document['jobs'].values() for s in job.get('steps', [])]


class CadenceContractTests(unittest.TestCase):
    def test_every_pr_update_keeps_all_tests_without_path_filters(self):
        for name in ['ci.yml', 'secret_scanning.yml']:
            with self.subTest(workflow=name):
                events = workflow(name)['on']
                self.assertNotIn('push', events)
                self.assertIn('workflow_dispatch', events)
                pr = events['pull_request']
                self.assertEqual(set(pr['branches']), {'main', 'master', 'develop'})
                self.assertNotIn('paths', pr)
                self.assertNotIn('paths-ignore', pr)
                if 'types' in pr:
                    self.assertIn('synchronize', pr['types'])
                    self.assertIn('opened', pr['types'])
                    self.assertIn('reopened', pr['types'])

    def test_cancellation_is_scoped_by_pr_and_workflow(self):
        groups = []
        for name in ['ci.yml', 'secret_scanning.yml', 'docker_client_contract.yml']:
            policy = workflow(name)['concurrency']
            self.assertEqual(policy['cancel-in-progress'], 'true')
            self.assertIn('github.event.pull_request.number || github.ref', policy['group'])
            groups.append(policy['group'])
        self.assertEqual(len(groups), len(set(groups)))

    def test_heavy_work_is_manual_or_staggered_weekly(self):
        schedules = []
        for name in ['security.yml', 'project_runtime_verification.yml', 'extended_verification.yml']:
            with self.subTest(workflow=name):
                events = workflow(name)['on']
                self.assertEqual(set(events), {'workflow_dispatch', 'schedule'} | ({'workflow_call'} if name == 'project_runtime_verification.yml' else set()))
                cron = events['schedule'][0]['cron'].split()
                self.assertEqual(cron[2:4], ['*', '*'])
                self.assertRegex(cron[4], r'^[0-6]$')
                schedules.append(tuple(cron))
        self.assertEqual(len(schedules), len(set(schedules)))

    def test_root_test_selectors_rerun_suites_without_recompiling_every_dependency(self):
        ci = workflow('ci.yml')
        for job, task, directory in [('backend', 'test', 'backend'), ('frontend', 'desktopTest', 'frontend'), ('frontend', 'wasmJsBrowserTest', 'frontend')]:
            with self.subTest(task=task):
                commands = [s for s in ci['jobs'][job]['steps'] if re.search(r'\./gradlew\s+' + task + r'\b', s.get('run', ''))]
                self.assertEqual(len(commands), 1)
                step = commands[0]
                self.assertEqual(step['working-directory'], directory)
                tokens = shlex.split(step['run'])
                self.assertIn('--rerun', tokens)
                self.assertNotIn('--rerun-tasks', tokens)
                self.assertNotIn('--tests', tokens)
                self.assertNotIn('continue-on-error', step)
                self.assertIn('--continue', tokens)

    def test_failed_test_command_cannot_be_reported_as_success(self):
        for job in ['backend', 'frontend']:
            for step in workflow('ci.yml')['jobs'][job]['steps']:
                if not re.search(r'\./gradlew (test|desktopTest|wasmJsBrowserTest)\b', step.get('run', '')):
                    continue
                with self.subTest(job=job, step=step['name']), tempfile.TemporaryDirectory() as raw:
                    directory = Path(raw)
                    wrapper = directory / 'gradlew'
                    wrapper.write_text('#!/bin/sh\nexit 42\n')
                    wrapper.chmod(0o700)
                    command = step['run']
                    # Exercise the actual workflow command, including its browser preflight.
                    result = subprocess.run(['bash', '-eo', 'pipefail', '-c', command], cwd=directory,
                                            env={'PATH': '/usr/bin:/bin', 'CHROME_BIN': '/usr/bin/true'},
                                            capture_output=True, text=True, timeout=5)
                    self.assertEqual(result.returncode, 42)

    def test_shared_runners_reject_fork_source(self):
        guard = "github.event.pull_request.head.repo.full_name == github.repository"
        for name in ['ci.yml', 'secret_scanning.yml', 'docker_client_contract.yml']:
            for job in workflow(name)['jobs'].values():
                if isinstance(job.get('runs-on'), dict):
                    self.assertIn(guard, job['if'])

    def test_coverage_gates_and_raw_results_remain_required(self):
        ci = workflow('ci.yml')
        commands = '\n'.join(s.get('run', '') for s in steps(ci))
        for gate in [':domain:storage:jacocoTestCoverageVerification', ':core:storage:jacocoTestCoverageVerification',
                     ':data:network:verifyInstantSerializerCoverage', ':core:ui:verifyInputSemanticsCoverage']:
            self.assertIn(gate, commands)
        for job in ['backend', 'frontend']:
            uploads = [s for s in ci['jobs'][job]['steps'] if 'upload-artifact@' in s.get('uses', '')]
            self.assertTrue(any('/test-results/' in s['with']['path'] for s in uploads))
            self.assertTrue(all(s['with']['if-no-files-found'] == 'error' for s in uploads))
        codecov = next(s for s in steps(ci) if s.get('uses', '').startswith('codecov/'))
        self.assertEqual(codecov['with']['fail_ci_if_error'], 'true')
        self.assertEqual(codecov['with']['skip_validation'], 'false')
        self.assertNotIn('continue-on-error', codecov)

    def test_manual_docker_validation_does_not_publish(self):
        docker = workflow('docker.yml')
        self.assertEqual(set(docker['on']), {'push', 'workflow_dispatch'})
        self.assertEqual(docker['on']['push'], {'tags': ['v*']})
        gate = "github.event_name == 'push' && startsWith(github.ref, 'refs/tags/v')"
        for s in steps(docker):
            if s.get('uses', '').startswith('docker/login-action@'):
                self.assertEqual(s['if'], gate)
            if s.get('uses', '').startswith('docker/build-push-action@'):
                self.assertEqual(s['with']['push'], '${{ ' + gate + ' }}')
                self.assertNotIn('continue-on-error', s)
        self.assertEqual(docker['jobs']['release']['if'], gate)
        backend = docker['jobs']['build-backend']['steps']
        smoke = next(s for s in backend if 'test_backend_runtime.sh' in s.get('run', ''))
        self.assertEqual(smoke['if'], "github.event_name == 'workflow_dispatch'")
        self.assertFalse(any('./gradlew' in s.get('run', '') for s in backend))

    def test_manual_environment_verification_cannot_replace_pr_tests(self):
        ci = workflow('ci.yml')
        scope = ci['on']['workflow_dispatch']['inputs']['project_environment']
        self.assertEqual(scope['default'], 'none')
        self.assertEqual(set(scope['options']), {'none', 'complete', 'inventory'})
        call = ci['jobs']['project-environment']
        self.assertIn("github.event_name == 'workflow_dispatch'", call['if'])
        self.assertEqual(call['uses'], './.github/workflows/project_runtime_verification.yml')
        self.assertNotIn('secrets', call)
        for name in ['backend', 'frontend', 'quality']:
            self.assertNotIn('project_environment', ci['jobs'][name].get('if', ''))
        native = workflow('project_runtime_verification.yml')
        self.assertIn("inputs.mode != 'inventory'", native['jobs']['native-linux']['if'])
        self.assertIn("github.event_name == 'workflow_dispatch'", native['jobs']['runner-inventory']['if'])

    def test_modified_application_workflows_use_major_actions(self):
        for name in ['ci.yml', 'security.yml', 'docker.yml', 'secret_scanning.yml',
                     'project_runtime_verification.yml', 'extended_verification.yml', 'docker_client_contract.yml']:
            for step in steps(workflow(name)):
                uses = step.get('uses', '')
                if uses and not uses.startswith('./'):
                    # Existing Trivy's upstream master selector has no official major alias.
                    if uses == 'aquasecurity/trivy-action@master':
                        continue
                    self.assertRegex(uses, r'^[^@]+@v\d+$')

    def test_candidate_runtime_placement_does_not_change_weekly_or_pr_cost(self):
        ci = workflow('ci.yml')
        placement = ci['on']['workflow_dispatch']['inputs']['project_environment_runner']
        self.assertEqual(placement['default'], 'hosted-linux')
        self.assertEqual(set(placement['options']), {'hosted-linux', 'macos-arm64'})
        native = workflow('project_runtime_verification.yml')
        self.assertEqual(native['on']['workflow_call']['inputs']['placement']['default'], 'hosted-linux')
        job = native['jobs']['native-linux']
        self.assertIn("inputs.placement == 'macos-arm64'", job['runs-on'])
        self.assertIn("|| '\"ubuntu-24.04\"'", job['runs-on'])
        sandbox = next(s for s in job['steps'] if 'provision_browser_sandbox.sh' in s.get('run', ''))
        self.assertEqual(sandbox['if'], "runner.os == 'Linux'")
        setup_index = next(i for i, s in enumerate(job['steps']) if s.get('uses') == './.github/actions/copilot-verification-setup')
        preparation = '\n'.join(s.get('run', '') for s in job['steps'][:setup_index])
        self.assertIn('inspect_verification_runner.py', preparation)
        self.assertIn('HOME=%s', preparation)
        self.assertIn('DOCKER_HOST=\\nDOCKER_CONTEXT=\\n', preparation)
        inventory = native['jobs']['runner-inventory']
        self.assertFalse(any(s.get('uses') == './.github/actions/copilot-verification-setup' for s in inventory['steps']))
        self.assertNotIn('secrets', ci['jobs']['project-environment'])

    def test_native_provenance_executes_setup_browser_outside_path_with_spaces(self):
        job = workflow('project_runtime_verification.yml')['jobs']['native-linux']
        provenance = next(s for s in job['steps'] if s.get('id') == 'provenance')
        self.assertEqual(provenance['env']['VERIFICATION_CHROME_BIN'], '${{ steps.runtime.outputs.chrome-path }}')
        setup = next(s for s in job['steps'] if s.get('id') == 'runtime')
        self.assertEqual(setup['timeout-minutes'], '5')
        with tempfile.TemporaryDirectory() as raw:
            directory = Path(raw)
            tools = directory / 'tools'
            tools.mkdir()
            for name, answer in [('git', 'verified-source'), ('java', 'openjdk version 17.0.17')]:
                command = tools / name
                command.write_text('#!/bin/sh\nprintf "%s\\n" ' + shlex.quote(answer) + '\n')
                command.chmod(0o700)
            browser = directory / 'Chrome for Testing'
            browser.write_text('#!/bin/sh\nprintf "%s\\n" "Google Chrome for Testing 155.0.8059.84"\n')
            browser.chmod(0o700)
            environment = dict(os.environ, PATH=str(tools) + os.pathsep + os.environ['PATH'],
                               VERIFICATION_SOURCE_SHA='verified-source', VERIFICATION_CHROME_BIN=str(browser),
                               RUNNER_TEMP=raw, GITHUB_ENV=str(directory / 'environment'))
            result = subprocess.run(['bash', '-eo', 'pipefail', '-c', provenance['run']],
                                    env=environment, capture_output=True, text=True, timeout=5)
            self.assertEqual(result.returncode, 0, result.stderr)
            record = json.loads((directory / 'verification-evidence/provenance.json').read_text())
            self.assertIn('155.0.8059.84', record['browser'])
            self.assertIn('CHROME_BIN=' + str(browser), (directory / 'environment').read_text())
            environment['VERIFICATION_CHROME_BIN'] = str(directory / 'missing browser')
            failure = subprocess.run(['bash', '-eo', 'pipefail', '-c', provenance['run']],
                                     env=environment, capture_output=True, text=True, timeout=5)
            self.assertNotEqual(failure.returncode, 0)


if __name__ == '__main__':
    unittest.main()
