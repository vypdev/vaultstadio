# Auditoría integral de VaultStadio — 25 de octubre de 2026

Issue de trabajo: [#71](https://github.com/vypdev/vaultstadio/issues/71). Commit de producto auditado: `615f11adf5272bd1d4626f9571b31e691eeee062` (`master`). Las mejoras locales anteriores se conservaron íntegramente en `eea796b14c979445108db1afb56ca59b104cd66e`; no modifican código de producción. La rama parte de `develop`, cuyo árbol era idéntico al de ese `master`.

## Dictamen

El backend permite un recorrido básico real de archivos, pero el producto completo **todavía no es operativo para uso general ni está listo para producción**. Hay fallos reproducidos de autorización y cuotas; web y desktop no llegan al login; S3/MINIO no arrancan; iOS no compila su entrada de aplicación. La CI verde de master valida sus jobs, no estos recorridos de producto.

La arquitectura es un monolito modular razonable, con separaciones útiles y sin ciclos en las dependencias Gradle declaradas examinadas. Su principal problema actual es la coherencia de contratos entre módulos: autorización, DTOs, configuración, efectos persistidos, capacidades de plataforma y composición real.

El plan de corrección verificable está en [AUDIT_ROADMAP_2026-10-25.md](AUDIT_ROADMAP_2026-10-25.md). No se ha cambiado el comportamiento del producto para ocultar los fallos de esta auditoría.

## Alcance y método

Se ejecutaron todas las suites JVM del backend y desktop del frontend, no solamente los módulos seleccionados por CI. Se instrumentaron los módulos de producción compilados con JaCoCo, sin excluir rutas sin tests. Se ejecutaron también tests Wasm en navegador, compilaciones Android/iOS y reglas de calidad. La cobertura indicada es de JVM/desktop; no mide líneas de código nativo iOS/Android ni se incrementa con las pruebas HTTP de los contenedores.

El dogfooding utilizó las dos imágenes publicadas de master, verificadas por su etiqueta OCI `org.opencontainers.image.revision=615f11adf5272bd1d4626f9571b31e691eeee062`, PostgreSQL 16 y volúmenes exclusivos. Todos los puertos se enlazaron a loopback. Se usaron cuentas `.invalid`, contenido sintético y configuración sin credenciales de proveedores. No se llamó a proveedores AI ni se desplegó nada en un entorno remoto.

El Mac es ARM. Las imágenes de master solo ofrecían `linux/amd64`; se probaron con emulación. El fallo de arranque desktop se confirmó también de forma nativa en JVM, por lo que no depende de esa emulación.

Se guardaron logs completos, XML de tests, informes HTML/XML de cobertura, capturas y copias de recuperación fuera de Git. En [audits/2026-10-25](audits/2026-10-25/) se versionan únicamente resultados saneados y la captura del fallo web. Los tokens, contraseñas de prueba y dumps no forman parte de la PR.

## Resultados ejecutados

| Verificación | Resultado | Límite de la evidencia |
|---|---|---|
| Backend completo, antes de recuperar las mejoras | 1.756 tests; 0 fallos, errores o skips | JVM; incluye Testcontainers y tests de módulos fuera de CI |
| Backend completo, con las mejoras preservadas | **1.762 tests; 0 fallos, errores o skips** | 6 casos locales recuperados |
| Frontend desktop completo, antes de recuperar las mejoras | 1.127 tests; 0 fallos, errores o skips | JVM/desktop |
| Frontend desktop completo, con las mejoras preservadas | **1.130 tests; 0 fallos, errores o skips** | 3 casos locales recuperados |
| `:composeApp:wasmJsBrowserTest` | **1.070 tests; 0 fallos, errores o skips** | Muchos casos son los mismos tests comunes ejecutados en otro destino; no son un recorrido del arranque de producción |
| `:androidApp:assembleDevDebug` | APK generado | No se verificó en un dispositivo Android; `testDevDebugUnitTest` devuelve `NO-SOURCE` |
| `:composeApp:compileKotlinIosSimulatorArm64` | Pasa | Compilación del código compartido |
| `:iosApp:compileKotlinIosSimulatorArm64` | **Falla** | `IosModule.kt` importa `TokenStorage` desde un paquete obsoleto |
| Backend `detektMain` y frontend `detektDesktopMain` | Comandos terminan correctamente | Hay avisos; éxito del comando no significa ausencia de deuda |
| `check-no-fqn.sh` y catálogo SDD | Pasan; 16 capacidades | El validador SDD comprueba estructura y existencia de rutas, no que se cumplan los contratos |
| `helm lint` / `helm template` | Renderizan correctamente | El render carece del PostgreSQL/Service que referencia la configuración por defecto; no equivale a despliegue funcional |
| API básica, ejecución confirmada | **81 comprobaciones: 73 pasan, 8 fallan** | Los fallos se conservan como resultados, sin rebajar expectativas para obtener verde |
| API avanzada, con tipos de entrada válidos | **21 comprobaciones: 11 pasan, 10 fallan** | Incluye efectos persistidos después del error y replay |
| Administración con cuenta desechable autorizada | **7 comprobaciones: 7 pasan** | Rol `ADMIN` temporal, restaurado a `USER`; acceso administrativo denegado de nuevo al finalizar |
| Copia, interrupción de DB y recuperación | **5 comprobaciones: 4 pasan, 1 falla** | `/ready` excede 40 segundos durante la caída de DB |
| Backend sobre DB y volumen restaurados | Readiness, login y descarga correctos | El SHA-256 del archivo restaurado coincide con el original; no es una prueba de restauración de producción a escala |

Los nueve tests recuperados son dos comprobaciones de constantes HTTP, cuatro de propagación de errores de repositorio y tres de traducciones. Se conserva el trabajo completo, pero esos casos no aumentan la cobertura de líneas agregada y no sustituyen tests de comportamiento integrado.

## Cobertura real y alcance de CI

| Área | Líneas cubiertas / totales | Cobertura de líneas | Ramas cubiertas / totales | Cobertura de ramas |
|---|---:|---:|---:|---:|
| Backend | 6.018 / 15.971 | **37,68 %** | 852 / 5.346 | **15,94 %** |
| Frontend JVM/desktop | 4.008 / 20.748 | **19,32 %** | 359 / 12.782 | **2,81 %** |

La CI actual selecciona ocho proyectos de tests backend y `:composeApp:desktopTest` en frontend. En el baseline omite **453 tests backend y 57 frontend: 510 existentes**. Con las mejoras recuperadas omite **457 + 60 = 517 tests**. Ejecutar `:core:test` no ejecuta los tests de todos sus subproyectos. La existencia de dependencias transitivas tampoco incluye automáticamente sus clases en un informe JaCoCo de otro módulo.

Los informes subidos a Codecov tienen un denominador parcial. `codecov.yml` configura una tolerancia del proyecto (`status.project`), aunque su comentario la describe como cobertura del patch. Debe acordarse una migración explícita al denominador completo, conservando los controles actuales hasta revisarla.

Ejemplos de líneas sin cobertura en el agregado: `S3Handlers.kt` 384, `WebDAVHandlers.kt` 334, `CollaborationRoutes.kt` 282, `AIHandlers.kt` 271 y `StorageRoutes.kt` 258. `ExposedCollaborationRepository.kt` tiene 396 líneas sin cubrir y 1 cubierta. En frontend, `FilePreview.kt` tiene 408 sin cubrir, `LoginScreen.kt` 318 y el `FilesViewModel` de `:feature:files` 315, con 0 cubiertas en esos tres archivos.

Además, `scripts/run-tests.sh` oculta cualquier fallo frontend con `|| true`, y `frontend/Makefile` hace lo mismo con Wasm. Estos caminos pueden informar éxito incompleto. No se usaron para decidir el resultado de esta auditoría.

## Dogfooding: qué funciona

- Registro, login, rechazo de contraseña incorrecta, consulta del usuario y cuota, refresh con rotación, rechazo del refresh anterior, logout e invalidación de sesión.
- Crear carpetas y subir, descargar, renombrar, mover, copiar, marcar, enviar a papelera y restaurar archivos propios. Se comprobaron bytes, no solo códigos HTTP.
- Denegación de lectura, descarga y rename del archivo mediante las rutas normales de almacenamiento a otro usuario; las excepciones de autorización se detallan abajo.
- Upload por chunks, rechazo de completado incompleto, ensamblado y descarga con bytes idénticos. El retry del completado no cumple la idempotencia declarada.
- Registro/listado/pull de dispositivos con la cabecera requerida; el push y su política de efectos fallan en los casos avanzados.
- WebDAV MKCOL, PUT, GET, MOVE y PROPFIND. COPY informa éxito pero el contenido no aparece en el destino solicitado.
- Consulta administrativa de usuarios/estadísticas/plugins y proveedores AI. Sin proveedor AI activo, chat devuelve un error explícito y no ejecuta una alternativa.
- Liveness tras una caída de DB; recuperación de readiness tras reiniciar DB; restauración de datos y archivo desde copias en un segundo backend.

## Defectos confirmados y contratos incumplidos

| ID | Prioridad | Evidencia observada | Implicación y SDD propietario |
|---|---|---|---|
| A01 | P0 | Una segunda cuenta crea una carpeta bajo una carpeta privada ajena: HTTP 201. Historial de versiones ajeno revela `itemName`: HTTP 200. Otra cuenta se une a la sesión de colaboración: HTTP 201. | Autorización inconsistente entre capacidades. SDD-0003/0004/0007/0009/0015. No se afirma acceso arbitrario al sistema de archivos. |
| A02 | P0 | Con cuota DB de 4 bytes se acepta un upload de 10 bytes, HTTP 201, y aumenta el número de registros. | La cuota no limita el consumo; falta control antes de guardar bytes y metadatos. SDD-0004, aceptación 2. |
| A03 | P1 | El archivo Wasm recibe dos cabeceras `Content-Type: application/wasm`; el navegador rechaza el MIME combinado y muestra pantalla vacía. | La imagen frontend puede estar healthy sin cargar la aplicación. SDD-0013/0014. |
| A04 | P1 | Eliminando solo el MIME duplicado en el contenedor diagnóstico, web falla al crear `AuthRepository`. Desktop falla porque el descriptor personalizado `kotlin.time.Instant` colisiona con `InstantSerializer`. | El cliente no llega al login. Web muestra el fallo de composición; desktop identifica la causa interna del serializador. SDD-0013. |
| A05 | P1 | GET de enlace público o protegido con la contraseña correcta devuelve 400: no se encuentra serializador para la respuesta. La descarga del enlace simple sí devuelve los bytes. | Compartir tiene un recorrido de información roto; no puede darse por funcional con el test del token aislado. SDD-0002/0006. |
| A06 | P1 | Push de sync con `changeType=MODIFY` persiste el cambio y luego devuelve 500 por serializar un mapa heterogéneo. Repetirlo añade otro registro. Un dispositivo desactivado también persiste un cambio antes del 500 (recuento 11 → 12). | Error después del commit, replay no idempotente y control de dispositivo ausente en push. SDD-0005/0008/0015. |
| A07 | P1 | Una subida nueva no aparece en el pull de sync; el historial de la subida normal está vacío. Las llamadas productivas a `createVersion` no incluyen el recorrido normal de upload. | Hay servicios/modelos, pero falta integrar mutaciones normales con seguimiento y versionado. SDD-0007/0008. |
| A08 | P1 | S3 y MINIO fallan al arrancar: `Authentication configuration with the name s3-signature was not found`. | No se alcanza una prueba real de proveedor S3. Además `StorageConfig` solo crea `s3` para el enum `S3`; el branch `MINIO` espera esa configuración. SDD-0004/0005/0014. |
| A09 | P1 | La entrada iOS falla por referencias no resueltas a `repository.TokenStorage`. Selección/descarga de archivos, drag/drop, medios y persistencia iOS contienen stubs o memoria volátil. | Compilar el módulo compartido no acredita una aplicación iOS funcional. SDD-0013. |
| A10 | P2 | WebDAV COPY responde 201, pero GET del destino solicitado devuelve 404. El handler no pasa el nombre de destino a `CopyItemInput`. | Falso éxito de protocolo. SDD-0002/0005. |
| A11 | P2 | `limit=-1` devuelve 500 de base de datos; un nombre de carpeta con `../` se acepta como metadato. | Falta validación semántica. No se comprobó una fuga de archivos fuera del volumen: el almacenamiento de bytes usa claves generadas. SDD-0002/0004/0015. |
| A12 | P2 | Repetir el completado de un upload ya terminado devuelve 404 de sesión. | Incumple la aceptación de idempotencia 2 de SDD-0005; no se observó pérdida de los bytes ya completados. |
| A13 | P2 | `/ready` no devuelve un resultado dentro de 40 segundos con DB caída; se recupera al reiniciarla. | Falta un presupuesto acotado de comprobación de dependencias. SDD-0014. |
| A14 | P1 | Helm por defecto referencia `*-postgresql:5432`, pero no genera PostgreSQL ni su Service ni declara una dependencia de chart. Imágenes por defecto no apuntan al GHCR verificado. | `helm lint` no prueba que el despliegue arranque. SDD-0014. |

P0 significa que debe resolverse antes de introducir datos privados o abrir el servicio a usuarios; P1 bloquea un recorrido principal o una capacidad anunciada; P2 es un contrato o recuperación incompletos que deben cerrarse con una prueba específica.

## Funcionalidades incompletas y límites de verificación

El backend usa tokens opacos de sesión almacenados por hash, aunque documentación/configuración todavía habla de JWT. `JWT_SECRET` tiene defaults de desarrollo inconsistentes y Helm utiliza el alias `VAULTSTADIO_JWT_SECRET`, que no lee `AppConfig`. No se atribuye a este hallazgo una falsificación de JWT: el mecanismo de sesión realmente comprobado es otro. También deben reconciliarse las opciones de rate limit con su conexión efectiva al servidor.

En frontend, las sesiones activas e historial de login devuelven listas vacías construidas localmente; revocar una sesión devuelve `NOT_IMPLEMENTED`; la configuración 2FA es fija y su pantalla indica que el alta no está disponible. No deben presentarse esos resultados vacíos como consultas reales completadas.

La federación saliente crea un registro pending y tiene el comentario explícito de que el envío HTTP se haría en producción; ese método no lo ejecuta. No se verificó interoperabilidad entre dos instancias ni convergencia OT entre varios clientes. La liveness del endpoint de federación no acredita ninguna de esas propiedades.

No se efectuaron llamadas a AI externo ni se verificaron facturación, credenciales de proveedores o calidad de modelos. Las listas de plugins/proveedores del contenedor probado estaban vacías. Los tests de los cuatro plugins pasan, pero no se ha demostrado carga de sus JARs y sus efectos en la imagen desplegada. No se ha ensayado producción, un clúster Kubernetes, dispositivos físicos, estrés de archivos de decenas de GB, TLS público o un proveedor S3 real. La descarga de las imágenes MinIO configuradas no estuvo disponible; A08 se reproduce antes de necesitar ese proveedor.

Los límites de memoria y concurrencia merecen una prueba separada: el upload normal y `S3StorageBackend.store` leen el contenido en `ByteArray`; locks, sesiones de chunks y bus de eventos tienen estado en memoria. No hay evidencia aquí de seguridad multi-réplica, resistencia a reinicios de transferencias incompletas ni una garantía de consumo acotado para cargas grandes.

## Estado y mejora de los SDDs

El catálogo contiene **12 `As-built baseline`, 4 `Proposed` y 0 `Implemented`**. Conserva como verificación original septiembre de 2026 (`603d84a…`). Esa clasificación ya advierte que una baseline no certifica readiness; no debe convertirse automáticamente en `Implemented` porque pase el validador.

Esta auditoría añade un checkpoint parcial a los SDDs y enlaces de evidencia al catálogo, manteniendo sus estados y la verificación original. Los fallos anteriores demuestran que varias aceptaciones declaradas no se cumplen actualmente. Los SDDs son una base de gobierno útil, pero todavía no son contratos ejecutables completos.

Las secciones 6.1/6.2 y la matriz de trazabilidad repiten texto genérico en las 16 capacidades. Faltan identificadores estables de requisitos/casos, DTOs y códigos exactos, permisos por operación, comportamiento después de un efecto parcial, presupuestos de tiempo/tamaño y enlaces a tests concretos. Los mínimos numéricos de casos no están derivados de un inventario verificable. El roadmap establece cómo convertir esas intenciones en aceptación demostrable, empezando por autorización, cuotas y transferencia.

Hay documentación de uso desactualizada: Quick Start conserva comandos Gradle raíz eliminados, `/swagger-ui` frente al `/swagger` real y la promesa de que el primer usuario será administrador. La cuenta inicial en una DB vacía se creó como `USER`. README/Helm conservan URLs `yourusername`; las guías deben distinguir compilación, soporte parcial y funcionalidad verificada.

## Arquitectura y archivos grandes

El inventario de código productivo rastreado contiene 884 archivos Kotlin y 74.045 líneas. El análisis de declaraciones `project(...)` en 45 archivos Gradle backend y 53 frontend no encontró ciclos. El barrido de imports de framework/adaptadores en `backend/domain`, `backend/application` y `frontend/domain` no encontró los patrones prohibidos examinados. Es un control estático delimitado: no demuestra que Koin resuelva ni que toda dependencia dinámica respete la política.

Las separaciones domain/core/application/infrastructure/api y domain/data/feature/app permiten trabajar por capacidad. Conviene conservarlas y añadir reglas ejecutables de dependencia, autorización y composición. La cantidad de módulos y wrappers delegados introduce coste de wiring; se debe justificar con ownership y pruebas, sin promover una reescritura general.

| Archivo | Líneas | Responsabilidades que conviene separar después de los bloqueos |
|---|---:|---|
| `FilePreview.kt` | 681 | Selección por tipo, render y estados de medios; actualmente sin cobertura de líneas |
| `VideoMetadataPlugin.kt` | 675 | Ejecución de herramienta, parseo y mapeo de metadatos |
| `ExposedCollaborationRepository.kt` | 664 | Persistencia de sesiones, documentos, operaciones y presencia |
| `PluginManager.kt` | 607 | Descubrimiento, carga, ciclo de vida y registro de capacidades |
| `S3Handlers.kt` | 602 | Traducción de muchas operaciones de protocolo y errores |
| `FederationService.kt` | 594 | Descubrimiento, confianza, identidades, shares y actividades |
| `UserService.kt` | 577 | Registro, sesiones, refresh, perfiles y políticas de usuario |
| `MultipartUploadManager.kt` / `FileVersionService.kt` | 570 cada uno | Estado de transferencias / historial, retención, restore y diff |

Son candidatos a división por responsabilidad, no evidencia de que cualquier archivo largo sea un monolito defectuoso. Las traducciones y DTOs deben evaluarse por separado del código con decisiones y efectos. La prioridad es cerrar contratos y transacciones; fragmentar archivos sin tests no resuelve los defectos observados.

## Reproducción

Desde una copia limpia de la rama, con Java 17, SDK Android y Docker disponibles, usando los wrappers de cada proyecto:

```sh
cd backend
./gradlew test --continue --no-build-cache --init-script ../scripts/audit/aggregate-coverage.init.gradle auditJacocoAggregate
cd ../frontend
./gradlew desktopTest --continue --no-build-cache --init-script ../scripts/audit/aggregate-coverage.init.gradle auditJacocoAggregate
./gradlew :composeApp:wasmJsBrowserTest :androidApp:assembleDevDebug :androidApp:testDevDebugUnitTest :composeApp:compileKotlinIosSimulatorArm64 :iosApp:compileKotlinIosSimulatorArm64 --continue --no-build-cache
```

Los informes agregados quedan en `backend/build/reports/audit/aggregate/` y `frontend/build/reports/audit/aggregate/`. La instrumentación no modifica los umbrales de CI. Para repetir la batería HTTP, arrancar primero una instancia **desechable** enlazada a loopback y ejecutar desde raíz:

```sh
python3 scripts/audit/local-api-smoke.py --base-url http://127.0.0.1:8080 --output-dir /tmp/vaultstadio-audit-new-run
```

El directorio de salida debe ser nuevo y estar fuera del repositorio. El runner crea cuentas y archivos sintéticos, guarda la sesión con permisos privados para pruebas adicionales, sanea tokens de los resultados y devuelve código distinto de cero si hay fallos. No es un comando contra producción ni limpia automáticamente los fixtures. Los controles avanzados y de recuperación de esta ejecución están descritos arriba y conservan resultados saneados; la mutación temporal de rol se ejecutó solo tras autorización específica.

## Cierre de la iteración anterior y conservación

La [issue #62](https://github.com/vypdev/vaultstadio/issues/62) quedó actualizada con los resultados finales de master: [CI 40182624866, intento 2](https://github.com/vypdev/vaultstadio/actions/runs/40182624866), [Docker 40182624885](https://github.com/vypdev/vaultstadio/actions/runs/40182624885), [Security 40182624893](https://github.com/vypdev/vaultstadio/actions/runs/40182624893) y [observador Copilot 40188166382](https://github.com/vypdev/vaultstadio/actions/runs/40188166382). Los 27 checks aplicables terminaron correctamente; el release se omitió por su condición de rama/tag. El retry de CI resolvió un fallo transitorio DNS al subir un artefacto, después de pasar los tests. Las issues #60 y #62 están cerradas al 100 %.

Las cinco modificaciones locales originales se compararon por SHA-256, se guardaron en parche, tar, bundle verificado y ref de recuperación, y se publicaron sin alterar su contenido en `eea796b`. Se actualizó el master local mediante fast-forward. La auditoría utiliza exclusivamente la rama exacta creada por la Action para #71; no se creó, forzó, renombró o eliminó una rama remota gestionada.

Los defectos de producto y los trabajos del roadmap son trabajo nuevo de #71 y siguientes, no pendientes ocultos de la consolidación de dependencias #62. La aceptación de la auditoría consiste en pruebas reproducibles y un plan explícito; no en declarar todos los recorridos verdes cuando no lo están.
