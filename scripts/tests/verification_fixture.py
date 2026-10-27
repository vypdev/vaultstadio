"""Disposable CLI producer fixture; never presented as real Gradle/product evidence."""
import shutil
import subprocess
import tempfile
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
FAKE_GRADLE = r'''#!/usr/bin/env python3
import json,os,sys
from pathlib import Path
root=Path.cwd(); args=sys.argv[1:]; out=root/'build/verification'
out.mkdir(parents=True,exist_ok=True)
trace=root.parent/'build/trace.txt';trace.parent.mkdir(exist_ok=True)
with trace.open('a') as f:f.write(root.name+' '+' '.join(args)+'\n')
def inventory():
 tasks=[]
 for directory in sorted(root.glob('module*')):
  suites=['backend']if root.name=='backend'else['desktop','browser']
  for suite in suites:
   name={'backend':'test','desktop':'desktopTest','browser':'wasmJsBrowserTest'}[suite]
   task=f':{directory.name}:{name}';token=task.replace(':','_')
   tasks.append(dict(path=task,suite=suite,project=':'+directory.name,directory=str(directory),
    sources=[str(directory/'src/test')],xml=str(directory/f'build/test-results/{name}'),
    marker=str(out/f'task-events/{token}.json'),testHome=str(out/f'test-home/{token}'),
    execution=None if suite=='browser'else str(directory/f'build/jacoco/{name}.exec')))
 return dict(schemaVersion=1,root=str(root),tasks=tasks,production=[dict(project=':module1',sources=[str(root/'module1/src/main')],classes=[str(root/'module1/build/classes')])])
model=inventory()
if 'verificationInventory'in args:
 if os.environ.get('FIXTURE_FAIL_INVENTORY'):
  print('inventory-original-failure',file=sys.stderr);sys.exit(42)
 mode=os.environ.get('FIXTURE_MODEL')
 if mode=='empty':model['tasks']=[]
 if mode=='duplicate':model['tasks'].append(model['tasks'][0])
 if mode=='wrong-root':model['root']='/wrong/root'
 if mode=='additional':
  for task in model['tasks']:task['suite']='additional'
 if mode=='missing-population':model['production']=[]
 if mode=='unsupported':model['tasks'][0]['suite']='unsupported'
 (out/'inventory.json').write_text(json.dumps(model));sys.exit(0)
if any(a.startswith('detekt')for a in args):
 print('lint-original-diagnostic',file=sys.stderr);sys.exit(int(os.environ.get('FIXTURE_LINT_EXIT','0')))
if 'verificationCoverage'in args:
 c=''.join(f'<counter type="{t}" missed="3" covered="2"/>'for t in ['LINE','BRANCH','INSTRUCTION'])
 p=out/'coverage/coverage.xml';p.parent.mkdir(parents=True,exist_ok=True)
 p.write_text('<report><package name="p"><class name="p/C">'+c+'</class></package>'+c+'</report>')
 if os.environ.get('FIXTURE_COVERAGE_MUTATE_CLASS'):(root/'module1/build/classes/C.class').write_bytes(b'changed')
 if os.environ.get('FIXTURE_COVERAGE_MUTATE_SOURCE'):(root.parent/'mutated-source.kt').write_text('changed')
 sys.exit(int(os.environ.get('FIXTURE_COVERAGE_EXIT','0')))
code=0
for task in model['tasks']:
 if task['path']not in args:continue
 suite=task['suite'];failed=os.environ.get('FIXTURE_FAIL')in[suite,root.name]
 if os.environ.get('FIXTURE_MISSING')==suite:continue
 xml=Path(task['xml']);xml.mkdir(parents=True,exist_ok=True)
 failure='<failure message="deliberate assertion"/>'if failed else ''
 (xml/'TEST-Case.xml').write_text(f'<testsuite tests="1" failures="{int(failed)}" errors="0" skipped="0"><testcase name="case" classname="Suite">{failure}</testcase></testsuite>')
 if not failed:
  p=Path(task['marker']);p.parent.mkdir(parents=True,exist_ok=True)
  p.write_text(json.dumps(dict(task=task['path'],completed=True,runtime={'fixture':'not real runtime'})))
 if task['execution']:
  p=Path(task['execution']);p.parent.mkdir(parents=True,exist_ok=True)
  if os.environ.get('FIXTURE_NO_EXECUTION')!=suite:p.write_bytes(b'fixture-exec')
  classes=Path(task['directory'])/'build/classes';classes.mkdir(parents=True,exist_ok=True)
  (classes/'C.class').write_bytes(b'fixture-class-not-bytecode')
 if os.environ.get('FIXTURE_MUTATE_SOURCE')==suite:(root.parent/'mutated-source.kt').write_text('changed')
 if failed:
  print('original-'+suite+'-failure',file=sys.stderr);code=42
sys.exit(code)
'''


def create():
    temporary = tempfile.TemporaryDirectory()
    root = Path(temporary.name)
    (root/'scripts').mkdir()
    shutil.copytree(REPOSITORY/'scripts/verification', root/'scripts/verification', ignore=shutil.ignore_patterns('__pycache__'))
    shutil.copyfile(REPOSITORY/'scripts/run-tests.sh', root/'scripts/run-tests.sh')
    shutil.copyfile(REPOSITORY/'Makefile', root/'Makefile')
    for area in ['backend', 'frontend']:
        directory = root/area
        directory.mkdir()
        shutil.copyfile(REPOSITORY/area/'Makefile', directory/'Makefile')
        wrapper = directory/'gradlew'
        wrapper.write_text(FAKE_GRADLE)
        wrapper.chmod(0o700)
        add_module(root, area, 'module1')
    (root/'.gitignore').write_text('**/build/\n**/__pycache__/\n')
    subprocess.run(['git', 'init', '-q'], cwd=root, check=True)
    subprocess.run(['git', 'add', '.'], cwd=root, check=True)
    subprocess.run(['git', '-c', 'user.name=Fixture', '-c', 'user.email=fixture@audit.invalid', 'commit', '-qm', 'fixture'], cwd=root, check=True)
    return temporary, root


def add_module(root, area, name):
    source = root/area/name/'src/test/Case.kt'
    source.parent.mkdir(parents=True)
    source.write_text('// disposable CLI fixture source, not a product test\n')
