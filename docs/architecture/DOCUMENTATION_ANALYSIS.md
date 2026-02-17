# Documentation Analysis: Suitability for Humans and AIs

This document analyses whether the current VaultStadio documentation is well-suited for both human readers and AI assistants to understand what the project offers (frontend and backend).

**Analysis date**: 2026-02-16

---

## Executive Summary

The documentation is **strong overall**: it has a clear index, layered architecture docs, a full API reference, and Cursor rules that point AIs to the right context. To make it **more apt** for both audiences, fix broken references, add a few structural improvements (TOC, single “project offer” summary, optional glossary), and keep cross-references and “last updated” consistent.

---

## Strengths (What Works Well)

### For Humans

| Aspect | Evidence |
|--------|----------|
| **Central navigation** | [INDEX.md](../INDEX.md) provides “I want to…” quick links, tables by audience (Developers, Operators, Frontend Devs), and a clear doc structure. |
| **Layered depth** | README → QUICK_START → ARCHITECTURE / FRONTEND_ARCHITECTURE / API allows readers to go from “what is this?” to “how do I run it?” to “how is it built?”. |
| **Visual architecture** | [ARCHITECTURE.md](ARCHITECTURE.md) (this directory) uses ASCII diagrams for backend, frontend, versioning, sync, federation, collaboration, WebDAV/S3. |
| **Concrete examples** | API.md and FRONTEND_FEATURES.md include HTTP snippets, Kotlin code, and component usage. |
| **Audience tagging** | INDEX tables label documents by audience (All, Developers, Operators, Frontend Devs, Plugin Devs). |
| **Single language** | All docs are in English, matching project rules. |

### For AIs

| Aspect | Evidence |
|--------|----------|
| **Explicit AI guidelines** | [AI_CODING_GUIDELINES.md](../development/AI_CODING_GUIDELINES.md) defines role, priorities, “what to avoid”, “what to always do”, and references to rules/architecture. |
| **Context-aware rules** | `.cursor/rules/*.mdc` use globs (e.g. `kotlin-backend/**`, `compose-frontend/**`) so the right rules load for the right files. |
| **Pointers to docs** | Each rule file references the main doc (e.g. ARCHITECTURE.md, API.md, FRONTEND_ARCHITECTURE.md). |
| **Stable structure** | Module layout, route tables, and component/ViewModel patterns are described in a parseable way (tables, code blocks, headings). |
| **INDEX “Cursor Rules” section** | INDEX.md explains which rule file applies where, helping AIs (and humans) know what context is available. |

### Backend and Frontend Coverage

- **Backend**: ARCHITECTURE.md (modules, design principles, plugins, Phase 6), API.md (full REST reference), vaultstadio-backend.mdc (stack, routes table, conventions).
- **Frontend**: FRONTEND_ARCHITECTURE.md (Decompose, components, ViewModels, platform expect/actual), FRONTEND_FEATURES.md, FRONTEND_COMPONENTS.md, FRONTEND_DEVELOPMENT.md, FRONTEND_TESTING.md, vaultstadio-frontend.mdc (structure, features table, platform table).

So both “what the backend offers” and “what the frontend offers” are interpretable by humans and AIs, with backend slightly more centralized (ARCHITECTURE + API) and frontend spread across several focused docs.

---

## Gaps and Risks

### 1. Broken reference: ACTION_PLAN.md

- **Issue**: ACTION_PLAN.md was referenced in four places but **the file does not exist**:
  - `.cursor/rules/vaultstadio.mdc`
  - `.cursor/rules/vaultstadio-core.mdc`
  - `.cursor/rules/vaultstadio-frontend.mdc`
  - `.cursor/rules/vaultstadio-backend.mdc`
- **Impact**: Humans and AIs following the rules get a dead link; core rules also mention “77 tasks” and “improvement roadmap” that cannot be verified.
- **Recommendation**: Either create `docs/ACTION_PLAN.md` (e.g. from existing plans or a “Future work” section) or remove/reword all references to ACTION_PLAN.md and the “77 tasks” in the rule files.

### 2. No single “what this project offers” page

- **Issue**: There is no one document that summarises in one place: product purpose, main user-facing features, and main technical capabilities (backend + frontend).
- **Impact**: New contributors and AIs must infer “what the project offers” from README + ARCHITECTURE + INDEX. Slower onboarding and risk of inconsistent summaries.
- **Recommendation**: Add a short **docs/PROJECT_OVERVIEW.md** (or expand README with a “What VaultStadio offers” section) listing: target users, core vs advanced features, and one-paragraph backend/frontend/plugin summaries with links to ARCHITECTURE, API, and FRONTEND_ARCHITECTURE.

### 3. API.md lacks a table of contents

- **Issue**: API.md is long (2000+ lines) and has no TOC at the top.
- **Impact**: Humans cannot quickly jump to “Storage”, “AI”, “Sync”, etc.; AIs may need to search or read large chunks.
- **Recommendation**: Add a TOC after the overview (anchor links to each major section) and optionally a one-page “Endpoints by area” table (Auth, Storage, Share, Search, Admin, AI, Version, Sync, Federation, etc.) with section anchors.

### 4. Minor inconsistency: AppViewModel in FRONTEND_ARCHITECTURE

- **Issue**: In FRONTEND_ARCHITECTURE.md, the “Example: Loading Files” and “Error Handling” sections still refer to “AppViewModel”, while KNOWN_ISSUES.md states the monolithic AppViewModel was split into per-screen ViewModels.
- **Impact**: Confusion for humans and AIs about current state management.
- **Recommendation**: Update those sections to use the current pattern (e.g. FilesViewModel and the relevant Component) and add a short note that the legacy AppViewModel was replaced.

### 5. No central glossary

- **Issue**: Terms such as StorageItem, ShareLink, PluginContext, ChildStack, MainDestination, ApiResponse, Either are used across docs but not defined in one place.
- **Impact**: New readers and AIs may need to search multiple files to interpret domain and technical terms.
- **Recommendation** (optional): Add **docs/GLOSSARY.md** with short definitions and links to ARCHITECTURE, API, or FRONTEND_ARCHITECTURE where the concept is explained. INDEX.md can link to it under “Architecture & Design” or “Getting Started”.

### 6. “Last updated” and versioning

- **Issue**: Only KNOWN_ISSUES.md has a “Last Updated” date; other docs do not.
- **Impact**: Hard to know if a doc reflects the current codebase; AIs cannot prioritise “freshest” content.
- **Recommendation**: Add “Last updated: YYYY-MM-DD” (or “Doc version”) to key docs (ARCHITECTURE, FRONTEND_ARCHITECTURE, API, INDEX) and update when making material changes.

---

## Recommendations Summary

| Priority | Action | Audience |
|----------|--------|----------|
| High | Fix or remove ACTION_PLAN.md references and “77 tasks” in .cursor rules | Humans + AIs |
| High | Resolve AppViewModel wording in FRONTEND_ARCHITECTURE.md | Humans + AIs |
| Medium | Add PROJECT_OVERVIEW.md or README “What we offer” section | Humans + AIs |
| Medium | Add TOC and “Endpoints by area” to API.md | Humans + AIs |
| Low | Add GLOSSARY.md and link from INDEX | Humans + AIs |
| Low | Add “Last updated” to main docs | Humans + AIs |

---

## Conclusion

The documentation is **already apt** for humans and AIs to interpret what the project offers on both frontend and backend: INDEX gives navigation and audience-oriented structure, ARCHITECTURE and FRONTEND_ARCHITECTURE explain design and modules, API.md is a complete REST reference, and Cursor rules plus AI_CODING_GUIDELINES give AIs clear behaviour and context. The main improvements are: **fixing the broken ACTION_PLAN.md references**, **aligning FRONTEND_ARCHITECTURE with the current ViewModel architecture**, and **adding a single “what we offer” entry point and an API TOC**. Optional enhancements (glossary, “last updated”) would further improve interpretability for both humans and AIs.
