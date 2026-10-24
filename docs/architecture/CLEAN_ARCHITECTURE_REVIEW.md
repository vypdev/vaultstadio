# Clean Architecture Review

**Last updated**: 2026-02-19

This document reviews VaultStadio against [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) principles and suggests targeted improvements. It complements [ARCHITECTURE.md](ARCHITECTURE.md) and [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

---

## Clean Architecture Principles (Summary)

1. **Dependency rule**: Source code dependencies point only inward. Inner layers (domain, application) do not depend on outer layers (API, infrastructure, UI).
2. **Entities**: Enterprise business rules; stable, framework-agnostic.
3. **Use cases (application layer)**: Application-specific business rules; orchestrate entities and ports; one use case per application operation.
4. **Interface adapters**: Presenters, controllers, gateways; convert between external formats (HTTP, DTOs) and internal formats (domain, use-case I/O).
5. **Frameworks and drivers**: Ktor, Compose, Exposed, Koin; only the outermost layer.

---

## Current State vs Clean Architecture

### Backend

| Aspect | Current state | Clean Architecture ideal |
|--------|----------------|---------------------------|
| **Domain** | `core/domain`: models, repository interfaces, services, events. | Entities + repository (and other) ports. No services in “entity” ring. |
| **Application layer** | **Use cases** in `api/application/usecase/*`: storage, auth, share, activity, user, admin, metadata, chunkedupload, health, version, sync, plugin, ai. Routes resolve use cases via Koin and call them; no services injected in `configureRouting` for these areas. | Use cases between routes and domain; routes depend only on use-case interfaces and DTOs. |
| **API (interface adapters)** | Routes depend on use-case interfaces and DTOs. Domain → DTO mapping (e.g. `toResponse()`) remains in routes/handlers. WebDAV/S3 and optionally Federation/Collaboration still receive services in Routing. | Routes depend only on use-case interfaces and DTOs; mapping (domain ↔ DTO) lives in a boundary/adapter layer. |
| **Infrastructure** | `infrastructure` implements core repository interfaces and storage backends. **core** does not depend on infrastructure. | Same idea: infrastructure implements ports defined by core. |
| **Core dependencies** | Core has `implementation(libs.bundles.exposed)` in `build.gradle.kts`; no Exposed imports in core **main** source. | Core should have zero dependency on Exposed or any ORM; only interfaces (e.g. `TransactionManager`) in core. |

**Conclusion (backend)**  
- Dependency direction is correct: **api** and **infrastructure** depend on **core**; core defines repositories and services.  
- **Done**: Application (use-case) layer in place; routes use Koin for use cases. Remaining gaps: (1) Domain → DTO mapping still in routes/handlers. (2) WebDAV/S3 and optionally Federation/Collaboration still take services in Routing. (3) Core’s Exposed dependency should be removed if unused in main code (or moved to test-only).

---

### Frontend

| Aspect | Current state | Clean Architecture ideal |
|--------|----------------|---------------------------|
| **Domain** | `domain/model`, `domain/upload`: frontend-specific models (duplicated from backend). | Domain models in a stable, shared or app-domain layer. |
| **Application layer** | **Use cases** in `domain/usecase`: interfaces + `*Impl` depending on repository interfaces. ViewModels depend on use cases only. | Same idea: use cases orchestrate repositories; UI depends on use cases. |
| **Interface adapters** | ViewModels and screens use domain models and use-case results. | Presenters/ViewModels depend on use-case output types, not network types. |
| **Data** | Repositories (interface in `data/repository`, impl in `data/`); services wrap API clients; API client in `data/api`, `data/network`. | Repositories implement ports; use cases depend on repository interfaces only. |
| **Result type** | Domain: `Result<T>`. Data: `ApiResult<T>`. Repositories map ApiResult→Result at boundary via `toResult()`. | ✅ Aligned: two types; explicit mapping data→domain. |

**Conclusion (frontend)**  
- Use-case → repository abstraction and ViewModel → use-case dependency are aligned with Clean Architecture.  
- **Result** in domain, **ApiResult** in data (both real types); repositories map ApiResult→Result with `toResult()` so domain never sees ApiResult. Remaining gap: domain models duplicated from backend; no shared contract (see “Shared module” below).

---

### Shared Module

- There is **no** Gradle shared KMP module. “Shared” in the docs refers to code shared across platforms inside `composeApp/commonMain`.  
- Backend and frontend define their own DTOs/domain models; the API contract is implicit (backend routes + DTOs vs frontend API client + domain).  
- A shared module could hold API request/response DTOs (and optionally client interfaces) to make the contract explicit and reduce drift.

---

## Improvement Points (Prioritised)

### High impact

1. **Backend: Introduce an application (use-case) layer** ✅ *Done*  
   - A thin layer exists in `kotlin-backend/api/src/main/kotlin/com/vaultstadio/api/application/usecase/` (storage, auth, share, activity, user, admin, metadata, chunkedupload, health, version, sync, plugin, ai).  
   - Use cases depend on domain services/repositories; return `Either<Error, T>` or domain types. Routes resolve use cases via Koin and call them; mapping (domain → DTO) remains in routes.  
   - **Benefit**: Routes are thin adapters; orchestration lives in use cases; domain types stay in core.

2. **Frontend: Move `ApiResult` to domain (or shared kernel)** ✅ *Done*  
   - **Domain** defines a neutral **`Result<T>`** in `domain.result`; use-case interfaces and ViewModels use only `Result<DomainModel>`.  
   - **Data** defines **`ApiResult<T>`** in `data.network` (real sealed class); BaseApi and services return `ApiResult<DTO>` or `ApiResult<T>`.  
   - At the boundary, repository implementations map **ApiResult → Result** via `data.mapper.toResult()` (and optionally `toResult(transform)` for DTO→domain). So `ApiResult<MyClassDTO>` becomes `Result<MyClass>` when crossing into domain.  
   - **Benefit**: Two explicit types; no typealias; clean data→domain boundary with explicit mapping.

3. **Backend: Centralise domain → DTO mapping**  
   - Group all `toResponse()` and request → domain mapping in a single api-boundary package or module (e.g. `api/boundary` or `api/mapper`).  
   - Routes only call use cases and pass pre-built DTOs to `respond`.  
   - **Benefit**: Clear API contract; easier to evolve DTOs and domain independently.

### Medium impact

4. **Backend: Remove Exposed from core**  
   - Confirm no main source in `core` uses Exposed; if only tests need it, use `testImplementation(libs.bundles.exposed)` in core or keep Exposed only in infrastructure.  
   - **Benefit**: Core stays framework-agnostic and depends only on interfaces (e.g. `TransactionManager`).

5. **Shared API contract (optional)**  
   - Introduce a KMP `shared` (or `contract`) module with API request/response DTOs (and optionally client interfaces).  
   - Backend uses it for contract tests or documentation; frontend uses it for the API client and mapping to app-domain models.  
   - **Benefit**: Single source of truth for the API surface; less duplication and drift between backend and frontend.

### Lower priority

6. **Backend: Split “domain” to entities vs application**  
   - Clean Architecture separates “entities” (pure business objects) from “use cases” (application logic). Today core mixes domain models, repository interfaces, and services.  
   - Option: keep models and repository interfaces in `core/domain`; move services into `core/application` and treat them as use-case-like components that depend only on repositories and domain types.  
   - **Benefit**: Clearer boundary between “what the system is” (entities) and “what the system does” (application services/use cases).

7. **Frontend: Reduce domain model duplication**  
   - If a shared module is introduced, consider sharing a minimal set of types (ids, enums, error codes) and keep full domain models in each app where needed.  
   - **Benefit**: Less drift and fewer bugs when API or domain evolves.

---

## Suggested Dependency Flow (Target)

**Backend (conceptual)**  
- **api** → application (use cases) → **core** (domain: entities, repository interfaces; optional application services).  
- **api** does not reference core domain types in route handlers; only DTOs and use-case interfaces.  
- **infrastructure** → **core** (implements repositories and other ports).  
- **core** has no dependency on Exposed, Ktor, or infrastructure.

**Frontend (conceptual)**  
- **ui** (screens, ViewModels) → **domain** (use cases, models, result type).  
- **domain** (use cases) → repository interfaces only; result type lives in domain or shared kernel.  
- **data** (repository implementations, API client, services) → **domain** (implements repository interfaces; maps API → domain models and domain result type).

---

## References

- [ARCHITECTURE.md](ARCHITECTURE.md) – Current system and module layout.  
- [KNOWN_ISSUES.md](KNOWN_ISSUES.md) – Resolved and current technical debt.  
- [AI_CODING_GUIDELINES.md](../development/AI_CODING_GUIDELINES.md) – Clean architecture and separation of concerns.  
- Uncle Bob’s [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html).
