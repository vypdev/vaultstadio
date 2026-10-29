# Roadmap verificable derivado de la auditoría del 25-10-2026

Entrada: [auditoría integral](REPOSITORY_AUDIT_2026-10-25.md), evidencia de `master` `615f11a…`, issue [#71](https://github.com/vypdev/vaultstadio/issues/71). Este documento propone trabajo posterior; una fila no significa funcionalidad implementada. Mantener las mejoras ya preservadas en `eea796b` y evitar una reescritura general.

Actualización de planificación: el [roadmap del 29-10-2026](PRODUCT_ROADMAP_2026-10-29.md)
incorpora las 30 decisiones de producto de #124 y el punto de partida posterior a #123.
Mantiene los checkpoints siguientes como evidencia histórica y conserva #116, #82/#115,
#109/#112/#111 como reparaciones acotadas antes de los hitos nuevos.

## Orden de trabajo

Las primeras tres prioridades son seguridad de datos, arranque del cliente y verificación de CI. P0 debe cerrarse antes de usar datos privados; las tareas de UI y CI pueden avanzar independientemente después de fijar los contratos. Cada bloque debe convertirse en una issue admitida y una PR acotada mediante Copilot. La Action crea y mantiene su rama; no se crean ramas remotas alternativas.

| Orden / ID | Prioridad | Entrega concreta | Dependencias | Evidencia necesaria para aceptar |
|---|---|---|---|---|
| 0 / R00 | Hecho | Preservar las cinco mejoras locales, ejecutar baseline y publicar auditoría/roadmap | #71 | Hashes originales idénticos, commit remoto `eea796b`, resultados y runner reproducible; reconciliación local/remoto sin WIP perdido |
| 1 / R01 | P0 | Política única de autorización para storage, versiones y colaboración | SDD-0003/0004/0007/0009/0015 | Matriz de propietario, destinatario con read/write, usuario ajeno, anónimo y admin para cada operación; denegación antes del efecto, comprobada en HTTP y DB |
| 2 / R02 | P0 | Cuotas y validación antes de escribir; compensación de efectos parciales | R01, SDD-0004/0005 | Límites exactos, concurrencia, cuota al copiar/restaurar, nombres inválidos y fallos de metadatos; bytes y filas consistentes tras cada rechazo |
| 3 / R03 | P1 | Arranque web/desktop y contrato URL/DTO del cliente | SDD-0002/0013 | MIME único, serializador sin colisión, Koin real y recorrido visible de login + archivos en web y desktop; URLs generadas contra rutas reales |
| 4 / R04 | P1 | Todos los módulos y arranques reales en CI; cobertura con denominador completo | R03 para smoke de UI | Reconciliar el inventario actual por revisión, ejecutar todas las suites aplicables y negativos que den rojo; publicar agregados completos; infraestructura [#84/#118 adoptada](verification/COMPLETE_VERIFICATION_84.md), incremento crítico [#119](verification/CRITICAL_COVERAGE_119.md) y gates nuevos mediante revisión explícita |
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

Contrato previo al código y candidato locala29139c:2740 casos backend,485 nuevos, cero fallos/errores/omisiones; puertos mínimos, membresía, escrituras de documento y comentarios SQL atómicos, rechazo de snapshots obsoletos y sockets vinculados a archivo/participante/conexión. [Informe y cobertura completa del módulo](verification/COLLABORATION_AUTHORIZATION_89.md):382/395 líneas y183/207 ramas, con OT incluido. El paquete actualizado completa35 peticiones HTTP y nueve frames de dos clientes nativos, con denegación de membresía antigua, reply correcto, bytes conservados y rollback. CI/revisión/adopción del head final pendientes.

El cliente de colaboración usa una URL/mensajes incompatibles con el servidor y no incorpora su autenticación. No se declara funcional por pasar el backend. [#112 / R14](https://github.com/vypdev/vaultstadio/issues/112) concreta ese recorrido, bootstrap, guardado canónico, replay/streams y capacidades durables. El orden autorizado sigue con#90 (shares) y#91 (paginación) antes de promover master; R07/#109, setup/#111, búsqueda/#82 y el resto del roadmap conservan su alcance.

La excepción autorizada para#110 integró bc8ceed en develop0363822, con el mismo árbol probado. La CI automática posterior pasa4761 casos (2255backend,1315desktop,1191browser), sin fallos/errores/omisiones; protecciones intactas, master615f11a y VERIFICATION_SETUP=false. Se añadió el nuevo aviso obsoleto de rama ajena al reporte existente [copilot#723](https://github.com/vypdev/copilot/issues/723#issuecomment-6431631706), sin modificar Copilot.

R01c incorpora además el límite inclusivo del historial, reproducido antes del arreglo, y la captura SQL de peticiones delimitada con marcas PostgreSQL: conserva las aserciones de autorización y evita mezclar consultas de preparación. La reclamación de test fallido por llamada indirecta está documentada en [vypdev/copilot#738](https://github.com/vypdev/copilot/issues/738), con coVerify y evidencia de ejecución. CI/revisión/adopción del head final se registran en la PR#113; #90–#91 todavía requieren su implementación oficial.

### R01c adoption — PR #113

La excepción autorizada integró el headb7522dd en developeac7185. La [CI automática40470296193](https://github.com/vypdev/vaultstadio/actions/runs/40470296193) pasa5246 ejecuciones (2740backend,1315desktop,1191browser), todas sin fallos/errores/omisiones. El checkout de integración057c4b2 conserva el árbol0ee6b067 del merge. [#89 está cerrada con evidencia final](https://github.com/vypdev/vaultstadio/issues/89#issuecomment-6437095881), protecciones intactas y master615f11a. El recorrido completo de colaboración sigue en#112; no se declara funcional por cerrar la autorización. Continúan#90 y#91; no se promueve master con sus fallos pendientes.


## R05 checkpoint — issue #90

El [candidato03aa855 y su evidencia](verification/PUBLIC_SHARE_ACCESS_90.md) completan el acceso público a archivos:2981backend+1316desktop+1192browser=5489 casos sin fallos/errores/omisiones;241 casos backend nuevos y11 escenarios migrados conservados. Metadatos no consumen descargas, SQL admite una sola transferencia para el último cupo y el navegador descarga bytes y nombres Unicode correctos sin ayuda manual. El dogfooding detectó y corrigió también el Origin null del formulario y la fuga del token en los logs del proxy. Los cinco tests reales de Nginx se añaden al job existente, sin build ni workflow nuevo.

Cobertura global parcial:8558/16755 líneas y1823/6001 ramas, sin exclusiones nuevas; política28/28líneas y70/70ramas, servicio36/36 y19/20. Permanecen pendientes CI/revisión/adopción de la PR vinculada a#90 y el hito#91. Los permisos internos/heredados y de carpetas conservan su alcance propio;#109/#112/#111/#82 y el resto del roadmap siguen abiertos. No se promueve master ni se declara operativa toda la plataforma con este checkpoint.

### R05 adoption — PR #114

La excepción autorizada integra4e6d707 en developc9f1ac5. La [CI posterior40493531498](https://github.com/vypdev/vaultstadio/actions/runs/40493531498) confirma5489ejecuciones, sin fallos/errores/omisiones y con el mismo árbol9a49fbe1; protecciones intactas. R05/#90 queda adoptado. Se inicia#91 exclusivamente en su rama publicada por Copilot sobre ese padre; siguen abiertos el resto del roadmap, la aceptación global y master.


## R11 selection checkpoint — issue #91

El [candidato1499c6b](verification/STORAGE_SELECTION_91.md) valida paginación/orden antes de acceder a datos, conserva errores del proveedor y calcula metadatos int64 sin desbordamiento. Pasan3247backend+1324desktop+1200browser=5771ejecuciones, sin fallos/errores/omisiones;266casos backend nuevos y8KMP por plataforma, con cobertura global8958/16877líneas y1934/6042ramas sin exclusiones nuevas. Navegador real: login autónomo, ordenación,400visible por inyección local delimitada y recuperación200conRefresh; datos y reversión03aa855 preservados. Quedan pendientes CI/revisión/adopción de su PR.

La inspección amplía el backlog sin mezclar implementaciones: [#115](https://github.com/vypdev/vaultstadio/issues/115) posee filtros avanzados antes de paginar, cuentas globales y búsqueda real de contenido; [#116](https://github.com/vypdev/vaultstadio/issues/116) posee el origen de API Web fijado a localhost. #82 conserva nombre/prefijo; #84 cobertura global; #109/#112/#111 sus hitos propios. La plataforma completa y master no quedan certificados por este checkpoint.

El [suplemento de revisión63e12a1](verification/STORAGE_SELECTION_91_REVIEW.md) añade dos fallos PostgreSQL reales y eleva el inventario local a5773(3249backend), todos0, sin cambiar producción. La firma del JAR real contradice el nuevo falso positivo de herencia Exposed; recurrencia documentada en copilot#736. La CI del primer head de117pasa5771; la del nuevo head y la reconciliación de Copilot siguen siendo pasos de aceptación independientes.

## R04 implementation checkpoint — issue #84

La rama oficial se ha recuperado con autorización mediante avance directo de `25a2429` a `816e673`, conservando los 75 commits heredados. El contrato previo está en los SDD-0001/0013/0014 (commit `be76a1d`) y fija un mínimo de 40 controles de riesgo. El adaptador descubre módulos/tareas reales, conserva errores, vincula XML y datos JVM a fuentes/clases y produce dos informes canónicos con denominador completo. La [guía de tests](TESTING.md) sustituye las recetas antiguas de raíz Gradle inexistente y listas parciales.

Las fixtures de proceso no son tests de producto ni certifican CI. Los negativos Gradle/Wasm reales, las 5.773 ejecuciones completas, los gates y lint ya tienen evidencia local en el [informe R04](verification/COMPLETE_VERIFICATION_84.md). Permanecen pendientes la publicación, revisión de Copilot y CI del candidato final. La recuperación no autoriza un merge ni promoción a master; Copilot, protecciones y activación del setup no cambian. R04 sigue abierto durante esta implementación.


## R04 adoption — PR #118

La excepción autorizada integró `e774fd6` en develop `dcd7eab`. La [CI posterior 40531576901](https://github.com/vypdev/vaultstadio/actions/runs/40531576901) pasa 5.773 ejecuciones sin fallos, errores ni omisiones; su árbol 607ce09e coincide con develop y los 132 controles del verificador pasan. [#84 está cerrada con evidencia](https://github.com/vypdev/vaultstadio/issues/84#issuecomment-6446358313). Esta adopción corresponde al verificador completo; la cobertura global y la preparación de toda la plataforma siguen siendo trabajo incremental.

## R04 critical coverage checkpoint — issue #119

El [informe y la matriz de riesgos](verification/CRITICAL_COVERAGE_119.md) registran 6,091 ejecuciones completas sobre `85aca01`, sin fallos, errores ni omisiones. Las pruebas críticas utilizan autenticación productiva, PostgreSQL migrado, bytes reales, un servidor HTTP loopback y puertos de dominio controlados según la capa. Se sustituyen ocho tests vacíos, se corrigen las carreras de refresh/logout, el manejo HTTP/cancelación, la privacidad de enlaces públicos y la propiedad temporal de cargas, actividad y resultados parciales. Los SDDs se ampliaron antes del código; no cambian Copilot ni las protecciones. CI de la PR final, revisión y adopción requieren su propia evidencia.

Siguiente aceptación funcional: #78 con URLs reales y descarga/subida desde el cliente renderizado; después #82/#115 para búsqueda y filtros. #109/#112/#116/#111 mantienen recuperación, colaboración, origen Web y setup. La cobertura restante de servicios/adapters y los límites de responsabilidades de R13 continúan medidos, sin presentar este incremento como preparación global ni promoción a master. Borradores originales y datos locales preservados; la rama oficial debe mantenerse alineada por commits y push normales.

El cierre de #119 añade cinco regresiones comunes para cancelación durante lectura de cuerpos y timeout real. Cuatro negativos posteriores al checkpoint 6.081 reprodujeron un fallo que sus porcentajes por sí solos no detectaban; la política compartida corrige todos los adaptadores. La nueva pasada 6.091 y sus métricas están en el informe, con CI/revisión de su ref final pendientes. El falso positivo de dominio vacío se contrastó ejecutando el validador y se reportó en [copilot#749](https://github.com/vypdev/copilot/issues/749); no se alteró Copilot.

## Incremento #78 — aceptación de transferencias Web y macOS

El [informe de transferencias](verification/AUTHENTICATED_TRANSFERS_78.md) y sus 72 familias de riesgos conservan la primera aceptación de 6.355 ejecuciones sobre `4128d03`, ampliada por la pasada final indicada más abajo, sin fallos, errores ni omisiones. Se verificaron subida y descarga desde la aplicación real, menús de detalle/grid/lista, selección y carpetas ZIP, Unicode, archivos vacíos, fallo/reintento, cancelación y selectores nativos, con igualdad de bytes y SHA-256. Los controles conservan los límites explícitos y distinguen los callbacks productivos, puertos de prueba y resultados de sistema. La CI/revisión de la PR final y su adopción siguen siendo decisiones con evidencia independiente.

La selección real de directorios Web conserva un defecto anterior, ahora detallado en [#121](https://github.com/vypdev/vaultstadio/issues/121). El ZIP recursivo se probó con carpetas creadas por la UI y subidas ordinarias; no certifica ese picker. Tras la aceptación de #78, priorizar #82/#115 (búsqueda/filtros), #116 (origen Web) y #121 (selección de directorios), continuando después #109/#112/#111 y la cobertura de servicios/adapters y separación de responsabilidades de R13. Android/iOS, otros escritorios y preparación global mantienen su propia aceptación; no se promueve master ni se alteran Copilot o protecciones.

La revisión de #78 añade el rechazo de rutas ZIP con prefijo de unidad, 23 controles directos de segmentos (22/22 ramas), regresiones HTTP y del escritor, y un presupuesto Mocha finito para el caso completo de 32 MiB. La pasada final registra 6,386 ejecuciones sobre `ae5f22a`; el informe conserva los 6.355 anteriores y sus fallos posteriores como evidencia histórica. El backend actualizado también rechaza C: desde la UI y mantiene las descargas válidas binarias y ZIP con igualdad de bytes. CI/revisión/adopción del ref final siguen separadas; #759/#760 de Copilot documentan las limitaciones de enlace y evidencia sin modificarlo.


## Incremento #121 — selección de directorios Web

El [informe de selección de carpetas](verification/FOLDER_SELECTION_121.md) conserva el contrato previo,96 familias de riesgos,148 métodos nuevos y220 ejecuciones por plataforma. El candidato `d3816d6` pasa6.606 tests completos (3.388backend/1.664desktop/1.554Wasm), sin fallos, errores ni omisiones, con denominadores completos44/52 y gates existentes intactos. La política crítica de rutas/límites cubre33/33líneas y48/48ramas; la cobertura global continúa parcial y medida. Se descompone el adaptador Web y la coordinación de preparación sin crear nuevos archivos de producción monolíticos.

El dogfooding conserva raíz y jerarquía Unicode, archivos vacíos, rechazo seguro, cancelación de la aplicación/resultados tardíos, reintento al destino original, selección individual/múltiple y carga por bloques. Los13 archivos creados desde la UI mantienen tamaño y SHA-256. Una superposición real del buscador sobre los menús se reprodujo y corrigió con pruebas de renderer y componente antes de verificar el paquete corregido. Los nueve recorridos pasan con su alcance documentado. Se verifican además el selector físico de macOS en una ventana Chrome aislada, cancelación nativa sin mutaciones, selección y confirmación de tres archivos, subida3/3 y descarga binaria con el mismo SHA-256; los13 archivos acumulados conservan bytes y jerarquía. La CI40725080774 pasa6.569 tests y sus cinco artefactos/1.194XML originales están contrastados. También pasa la CI40729907715 sobrea23287c con6.569 tests y sus cinco artefactos originales contrastados. La revisión real detectó tres puntos: se aclara Chrome/macOS y se separan mensajes de archivo/carpeta con8 casos nuevos, mapeo38/38líneas y12/12ramas, y fallo de lectura real sin mutar los13 archivos/8 carpetas. El tercer hallazgo se refuta con el JavaScript generado y15 tests de navegador; se documenta en Copilot#764. Copilot cierra esos tres hallazgos y detecta Swing fuera de EDT: tras contrato previo, se corrige el ciclo del diálogo con9 controles de escritorio y lectura IO separada; el login y Cancel reales no mutan datos, sin certificar selección positiva dentro del diálogo Swing. El intento40738396952 conserva43 fallos de inicialización porque OrbStack se detuvo; se reanudan los mismos contenedores y hashes, sin reset. La suite local posterior pasa6.594; también la CI40745555932 sobree138c2c, con los cinco artefactos originales y1.197XML contrastados. Copilot detecta entonces lectura nativa antes de validar límites: tras contrato previo, se separan metadatos y lecturas acotadas, con12 casos reales de filesystem (7 fallos/5 controles antes y48 controles focalizados en verde después), sin cambiar los umbrales de selección ordinaria/grande. La pasada local completa d3816d6 pasa6.606 y1.199XML; conserva el fallo anterior por Docker/ENOSPC y la repetición tras recuperar espacio. El login y Cancel del paquete nativo nuevo no mutan los13 archivos/8 carpetas; no se certifica selección física positiva en Swing. El candidato nuevo requiere su propia CI, revisión y adopción; la revisión de borrador omitida no se considera favorable.

Se conservan los30 archivos previos, PostgreSQL, las dos propuestas SDD originales de#74 y la reversión ejecutable. Copilot, protecciones, master y VERIFICATION_SETUP no cambian. Próximo orden funcional: #116 (origen de API Web configurable), #82/#115 (búsqueda y filtros), después #109/#112/#111 y mayor cobertura/separación de responsabilidades de servicios y adaptadores. Otros navegadores, hardware/plataformas móviles y la preparación global requieren aceptación propia; este incremento no los declara operativos.
