# Roadmap verificable derivado de la auditoría del 25-10-2026

Entrada: [auditoría integral](REPOSITORY_AUDIT_2026-10-25.md), evidencia de `master` `615f11a…`, issue [#71](https://github.com/vypdev/vaultstadio/issues/71). Este documento propone trabajo posterior; una fila no significa funcionalidad implementada. Mantener las mejoras ya preservadas en `eea796b` y evitar una reescritura general.

## Orden de trabajo

Las primeras tres prioridades son seguridad de datos, arranque del cliente y verificación de CI. P0 debe cerrarse antes de usar datos privados; las tareas de UI y CI pueden avanzar independientemente después de fijar los contratos. Cada bloque debe convertirse en una issue admitida y una PR acotada mediante Copilot. La Action crea y mantiene su rama; no se crean ramas remotas alternativas.

| Orden / ID | Prioridad | Entrega concreta | Dependencias | Evidencia necesaria para aceptar |
|---|---|---|---|---|
| 0 / R00 | Hecho | Preservar las cinco mejoras locales, ejecutar baseline y publicar auditoría/roadmap | #71 | Hashes originales idénticos, commit remoto `eea796b`, resultados y runner reproducible; reconciliación local/remoto sin WIP perdido |
| 1 / R01 | P0 | Política única de autorización para storage, versiones y colaboración | SDD-0003/0004/0007/0009/0015 | Matriz de propietario, destinatario con read/write, usuario ajeno, anónimo y admin para cada operación; denegación antes del efecto, comprobada en HTTP y DB |
| 2 / R02 | P0 | Cuotas y validación antes de escribir; compensación de efectos parciales | R01, SDD-0004/0005 | Límites exactos, concurrencia, cuota al copiar/restaurar, nombres inválidos y fallos de metadatos; bytes y filas consistentes tras cada rechazo |
| 3 / R03 | P1 | Arranque web/desktop y contrato URL/DTO del cliente | SDD-0002/0013 | MIME único, serializador sin colisión, Koin real y recorrido visible de login + archivos en web y desktop; URLs generadas contra rutas reales |
| 4 / R04 | P1 | Todos los módulos y arranques reales en CI; cobertura con denominador completo | R03 para smoke de UI | Reconciliar el inventario actual por revisión, ejecutar todas las suites aplicables y negativos que den rojo; publicar agregados completos; hito [#84](https://github.com/vypdev/vaultstadio/issues/84) y gates nuevos mediante revisión explícita |
| 5 / R05 | P1 | Respuestas públicas de shares tipadas y flujo de enlace completo | R01, SDD-0006 | Abrir enlace, contraseña, expiración, revoke y límite de descarga con contenido real; error semántico sin trazas internas |
| 6 / R06 | P1 | Sync transaccional, respuesta tipada, dispositivo y replay | R01/R02, SDD-0008 | Respuesta serializable después de persistir, retry sin duplicados, dispositivo desactivado sin efectos, push/pull y conflictos reales de dos clientes |
| 7 / R07 | P1 | Versionado conectado al ciclo de archivos y recuperación de bytes | R01/R02, SDD-0007 | Crear historial al modificar; descarga binaria de versión; restore modifica el archivo actual y mantiene recuperable el anterior; compensación en fallo |
| 8 / R08 | P1 | S3/MINIO arrancables y contrato de autenticación verificable | R01/R02, SDD-0004/0005 | Configuración de ambos enums, auth registrada y firmas probadas; proveedor S3 local aislado con round-trip real y tests negativos de acceso |
| 9 / R09 | P1 | iOS compilable y matriz real de capacidades móviles | R03, SDD-0013 | Compilar entrada/framework y ejecutar simulador; implementar o deshabilitar con explicación cada stub; instalar y ensayar APK en dispositivo/emulador separado |
| 10 / R10 | P1 | Despliegue y configuración coherentes | R03/R08, SDD-0014/0015 | Helm con DB/Service o DB externa validada, GHCR correcto, nombres de env correctos, versión única, readiness acotada y smoke de artefactos publicados |
| 11 / R11 | P2 | Contratos de WebDAV, paginación, chunks y reinicio | R01/R02, SDD-0002/0005 | COPY devuelve bytes en Destination; validación 4xx; completado repetible; cancelación/reinicio/expiración sin temporales huérfanos |
| 12 / R12 | P2 | SDDs trazables y documentación de uso ejecutable | Cada bloque actualiza su SDD al implementarse | ID requisito → ID escenario → test/caso ejecutado → evidencia/commit → guía; comandos de Quick Start ensayados desde copia limpia |
| 13 / R13 | P2 | Límites arquitectónicos y separación de responsabilidades grandes | R04/R12 | Grafo sin ciclos y imports verificados por CI; refactors por capacidad con mismos contratos; no mezclar cambios funcionales y movimientos masivos |
| 14 / R14 | P2 | Terminar capacidades avanzadas y retirar éxitos simulados | R01/R04/R06/R07/R10 | Federación entre dos instancias, OT con convergencia/reconexión, plugins realmente cargados, sesiones/2FA conectadas; AI externo solo con autorización y presupuesto |

R01 y R02 requieren diseño y casos de abuso antes de modificar código. R03 y R04 pueden tener PRs separadas. R05–R11 deben conservar pruebas de regresión de los fallos de la auditoría. No agrupar todas las reparaciones de producto en una única PR de auditoría.

## Primera iteración: criterios verificables

| Caso | Acción y comprobación | Fallo de origen |
|---|---|---|
| ACL-01 | B intenta crear carpeta y subir bytes en carpeta privada de A; 403/404, sin filas ni archivos nuevos | A01 |
| ACL-02 | B pide historial, versión, descarga, diff, delete y cleanup de A; no obtiene nombre, contenido ni efecto | A01 |
| ACL-03 | B intenta join/get/apply/save/comment de colaboración de A; no obtiene sesión ni modifica estado | A01 |
| ACL-04 | Repetir operaciones permitidas para propietario y destinatario de share con permiso mínimo; rechazar permiso insuficiente | A01 |
| QUOTA-01 | Upload con bytes restantes −1, exactos y +1; solo los casos permitidos dejan bytes y registro | A02 |
| QUOTA-02 | Dos uploads concurrentes compiten por la última cuota; consumo final nunca supera el límite | A02 |
| QUOTA-03 | Fallo DB después de escribir contenido y fallo de contenido antes de DB; estado recuperable y limpieza acotada | A02 |
| BOOT-01 | Abrir imagen publicada en navegador sin sesión ni caché; se ve login y no hay excepción de composición | A03/A04 |
| BOOT-02 | Desktop con preferencias desechables; se ve login y funciona el mismo recorrido de archivos | A04 |
| CONTRACT-01 | Serializar/deserializar un DTO con `Instant` usando la misma factoría de cliente que producción, en JVM y Wasm | A04 |
| FLOW-01 | Login, carpeta, upload, preview, descarga con hash, rename, move, trash, restore y logout desde UI, confirmando efectos por API | A03/A04 |
| CI-01 | Un test deliberadamente fallido en un módulo antes omitido provoca rojo; restaurar el fixture y obtener verde | Alcance parcial de CI |

Esta lista es el inventario inicial de 12 escenarios, no un sustituto de las variantes de la matriz de ACL. El SDD debe expandir cada combinación aplicable, asignar un ID al caso y derivar el presupuesto total de esa lista antes de marcarlo listo. La evidencia de un assert sobre una constante o un mock no acredita BOOT ni FLOW.

## Segunda iteración: integridad y protocolos

Para sync, registrar el cursor, aplicar una mutación real, cortar la respuesta después del commit y repetir la petición. Verificar que no se duplican registros, que otro cliente recibe el cambio una sola vez y que un dispositivo desactivado no puede escribir. Los conflictos y errores deben conservar estado y un resultado tipado; no basta con evitar el 500 de serialización.

Para versiones, modificar un archivo dos veces con contenidos diferentes y comprobar tanto el historial como los bytes de cada versión. Restaurar una anterior debe cambiar la descarga actual, conservar la anterior y aplicar cuota/autorización. Inyectar un fallo entre actualización de bytes y metadatos y demostrar recuperación. El endpoint actual que devuelve información de `storageKey` no es una descarga binaria validada.

Para shares y WebDAV, verificar efectos posteriores al éxito HTTP. Abrir la información de un enlace, descargar con contraseña, alcanzar el límite, revocar y reintentar. Ejecutar COPY/MOVE con Destination distinto, Overwrite F/T y permisos negativos; comprobar exactamente el nombre y los bytes del destino. Evitar mapas heterogéneos que se mutan correctamente pero no pueden serializarse al responder.

Para S3, diferenciar el adaptador de almacenamiento del protocolo compatible expuesto por VaultStadio. Cada uno requiere su propio contrato. No declarar compatibilidad con AWS CLI hasta probar firmas, canonicalización, permisos y errores con un cliente real y un proveedor aislado.

## Tercera iteración: operación y plataformas

Helm debe rechazar una configuración incompleta o generar todas las dependencias que referencia. Ensayar arranque desde volumen vacío y desde datos existentes. Un presupuesto inicial propuesto para `/ready` es un resultado explícito en ≤2 segundos con dependencia caída, compatible con las probes configuradas; el límite debe acordarse y probarse. La restauración local de esta auditoría es un primer caso positivo, no una garantía de RPO/RTO: definir ambos y ejecutar una recuperación con tamaño representativo.

La tabla de plataformas debe registrar para cada capacidad: implementada, parcial, deshabilitada o verificada, con comando, artefacto, dispositivo y commit. Un destino Gradle presente o una compilación compartida verde no son soporte operativo. Mantener iOS/Android en estado parcial hasta cerrar file picker, descarga, medios, persistencia, permisos y los flujos de sesión en ejecución real.

Separar estado durable de caches: uploads incompletos, locks, cursor de sync, presencia y eventos deben declarar qué sobrevive a un reinicio y qué necesita afinidad o almacenamiento compartido al escalar. Probar cancelación y expiración con inspección de temporales. Medir memoria y latencia con concurrencia controlada antes de prometer archivos de decenas de GB o múltiples réplicas.

## Cobertura y controles propuestos

La base medida es backend 37,68 % líneas / 15,94 % ramas y frontend 19,32 % / 2,81 %. Primero corregir el alcance de medición y la selección de tests; después acordar objetivos graduales, con especial atención a la lógica que decide permisos y produce efectos.

1. Todas las suites de cada módulo deben ejecutarse y fallar de forma visible. Eliminar `|| true` de rutas de tests; no usar un nuevo baseline para esconder regresiones.
2. Para cambios de lógica, proponer inicialmente ≥80 % de líneas y ≥70 % de ramas del cambio, junto con todos los escenarios de riesgo aplicables. Los umbrales requieren revisión y no se aplican silenciosamente en esta PR.
3. Medir por capacidad y por clase de prueba: policy, contrato de adapter, HTTP integrado, arranque, UI y recuperación. Mantener separados los casos comunes ejecutados en varias plataformas.
4. Dar prioridad a `StorageRoutes`, `WebDAVHandlers`, `S3Handlers`, colaboración/persistencia y `FilesViewModel`, que combinan alto riesgo con ausencia de cobertura efectiva.
5. Conservar resultados y fallos de las pruebas de producto como artifacts de CI. Una página HTML de Nginx o un puerto abierto no bastan como smoke de frontend.

## Definition of Done de cada entrega

La issue debe registrar el contrato y los límites que cambian, su SDD, el inventario de casos y la migración/rollback cuando haya datos persistidos. La PR debe contener una reproducción del fallo anterior, pruebas del comportamiento corregido, casos negativos y evidencia del artefacto que se va a usar. La revisión de Copilot y todos los checks aplicables deben quedar resueltos antes de promover la entrega.

Tras publicar, verificar que el checkout local y su ref remoto apuntan al mismo commit, que cada mejora previa sigue en historial y que no queda WIP fuera de la PR. Las promociones a `develop`/`master`, despliegues y cambios de ciclo de vida requieren la autorización correspondiente; la Action conserva la propiedad de las ramas gestionadas. No cerrar una capacidad como `Implemented` mientras existan decisiones de readiness o escenarios de aceptación fallidos.

## Evidencia adicional de #80 — 2026-10-26

Los 29 campos mantenidos tienen contratos de interacción; el candidato supera 1.315 casos desktop (incluidos 12 de anchura y edición del formulario real) y 1.191 Wasm, con dogfooding autónomo de login, edición y carpetas en Web/macOS. La comprobación independiente del backend supera 1.976 casos sin fallos ni omisiones. Véase [informe de accesibilidad](verification/FRONTEND_ACCESSIBILITY_80.md), con commits, denominadores y controles negativos. La cobertura completa de líneas sigue en 29,17% para desktop y 39,82% para backend; no se considera cerrada R04 ni la plataforma lista. El falso positivo de anchura y su sugerencia regresiva se han documentado en [vypdev/copilot#720](https://github.com/vypdev/copilot/issues/720), sin modificar Copilot ni el formulario correcto.

R03/SDD-0012 conserva un hallazgo adicional en [#82](https://github.com/vypdev/vaultstadio/issues/82): la búsqueda real `Web final` devuelve nombres no relacionados después de estabilizarse. El campo permite escribir/borrar, pero falta verificar y corregir el filtrado, sugerencias y contrato API con casos de nombres coincidentes/no coincidentes, aislamiento por propietario, Unicode, paginación y cancelación de respuestas antiguas. No mezclar esta reparación semántica con el adaptador de entrada de #80.

La revisión posterior de #80 detectó un fallo real de Enter multilínea con callback IME: reproducido, corregido y verificado con toda la selección Wasm. Los falsos positivos del nombre del SDK y del cargador inicial se reportaron con fuentes oficiales y evidencia ejecutada en [copilot#721](https://github.com/vypdev/copilot/issues/721) y [copilot#722](https://github.com/vypdev/copilot/issues/722). La CI técnica completa de `cdd8ac3` pasó; cada nuevo candidato mantiene su propia CI/revisión pendiente hasta verificarse.

R04: la inspección de logs de `063c8c1` rechazó una verificación de firma Codecov fallida aunque el job terminara verde. El candidato aísla GPG por ejecución, fija CLI11.3.1 y propaga errores; ocho controles de aislamiento, seis del wrapper oficial y firma/checksum reales con controles manipulados respaldan la corrección. La nueva configuración requiere su propia CI; las pruebas de producto y el artefacto Web no cambian.

## R01b checkpoint — issue #74

El candidato de permisos de versiones conserva los SDDs previos y separa facade, autorización, lecturas, mutaciones, diff, handlers y adapters. La validación local ejecutó 348 casos afectados, incluidos los 72 de matriz de servicio y 91 HTTP contra DI productiva, PostgreSQL migrado y bytes reales, sin fallos, errores ni omisiones. `core/version` mide 272/272 líneas y 90/90 ramas; no es cobertura global. Véanse [informe y límites](verification/VERSION_OWNERSHIP_74.md). El paquete local pasa 15 peticiones reales: siete superficies ajenas devuelven403 y el propietario conserva metadatos/bytes; los 237 archivos montados coinciden con el paquete y los volúmenes permanecen. CI final, revisión de Copilot y adopción quedan pendientes hasta su evidencia concreta.

Dos secuencias de propietario reprodujeron pérdida de bytes compartidos entre versiones/archivo actual; el guard conservador ya pasa los negativos de referencias y fallo tardío. R07 tiene ahora la issue [#109](https://github.com/vypdev/vaultstadio/issues/109), con restauración real del archivo, descarga binaria, snapshots, cuota, concurrencia y compensación. No se declara cerrada la recuperación. #89–#91 siguen bloqueando la promoción general.

La revisión de #110 amplía R01b con limpieza de blobs de retención desde la instantánea previa: un negativo real reproducido y diez casos nuevos pasan. Referencias actuales/históricas preservan bytes y las claves duplicadas se borran una vez. La compensación durable tras un fallo posterior al commit de metadatos permanece explícita en #109; no se promete atomicidad entre DB y proveedor.

La purga secuencial ya excluye el lote completo al contar referencias; cinco regresiones nuevas elevan la selección a348 casos (197core,140API,11SQL), todos verdes. Los dos avisos SQL contradicen el binario Exposed1.0.0 y se reportan en [copilot#736](https://github.com/vypdev/copilot/issues/736). La aceptación operacional de setup continúa en [#111](https://github.com/vypdev/vaultstadio/issues/111) después del cierre automático de#85; la propuesta de ciclo por etapas está en [copilot#735](https://github.com/vypdev/copilot/issues/735).

## R01c checkpoint — issue #89

Contrato previo al código y candidato localf35140d (producciónb0c60a6):2735 casos backend,480 nuevos, cero fallos/errores/omisiones; puertos mínimos, membresía, escrituras de documento y comentarios SQL atómicos, rechazo de snapshots obsoletos y sockets vinculados a archivo/participante/conexión. [Informe y cobertura completa del módulo](verification/COLLABORATION_AUTHORIZATION_89.md):382/395 líneas y183/207 ramas, con OT incluido. El paquete actualizado completa/reanuda40 peticiones HTTP y nueve frames de dos clientes nativos, con denegación de membresía antigua, reply correcto, bytes conservados y rollback. CI/revisión/adopción del head final pendientes.

El cliente de colaboración usa una URL/mensajes incompatibles con el servidor y no incorpora su autenticación. No se declara funcional por pasar el backend. [#112 / R14](https://github.com/vypdev/vaultstadio/issues/112) concreta ese recorrido, bootstrap, guardado canónico, replay/streams y capacidades durables. El orden autorizado sigue con#90 (shares) y#91 (paginación) antes de promover master; R07/#109, setup/#111, búsqueda/#82 y el resto del roadmap conservan su alcance.

La excepción autorizada para#110 integró bc8ceed en develop0363822, con el mismo árbol probado. La CI automática posterior pasa4761 casos (2255backend,1315desktop,1191browser), sin fallos/errores/omisiones; protecciones intactas, master615f11a y VERIFICATION_SETUP=false. Se añadió el nuevo aviso obsoleto de rama ajena al reporte existente [copilot#723](https://github.com/vypdev/copilot/issues/723#issuecomment-6431631706), sin modificar Copilot.
