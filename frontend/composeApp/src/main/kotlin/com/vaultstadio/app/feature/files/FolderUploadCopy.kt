package com.vaultstadio.app.feature.files

import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.domain.upload.FolderSelectionProblem

internal data class FolderUploadCopy(
    val choose: String,
    val hint: String,
    val preparing: String,
    val queued: String,
    val canceled: String,
    val unsupported: String,
    val invalid: String,
    val limits: String,
    val unreadable: String,
) {
    fun problem(problem: FolderSelectionProblem): String = when (problem) {
        FolderSelectionProblem.UNSUPPORTED -> unsupported
        FolderSelectionProblem.INVALID_PATH, FolderSelectionProblem.CONFLICTING_PATHS -> invalid
        FolderSelectionProblem.TOO_LARGE,
        FolderSelectionProblem.TOO_MANY_ENTRIES,
        FolderSelectionProblem.TOO_DEEP,
        -> limits
        FolderSelectionProblem.READ_FAILED -> unreadable
    }
}

internal fun folderUploadCopy(language: Language): FolderUploadCopy = when (language) {
    Language.ENGLISH -> FolderUploadCopy(
        "Upload folder",
        "Up to 32 MiB, 256 items, 16 levels. Empty folders are omitted.",
        "Preparing folder…",
        "Folder added to uploads",
        "No files selected. Empty folders are omitted.",
        "Directory selection is unavailable here. Try a browser with directory support.",
        "The selection contains unsafe or conflicting paths. Rename the entries and select again.",
        "Folder limit exceeded. Choose a folder within 32 MiB, 256 items and 16 levels.",
        "The selected content could not be read. Nothing was added. Select it again.",
    )
    Language.SPANISH -> FolderUploadCopy(
        "Subir carpeta",
        "Hasta 32 MiB, 256 elementos y 16 niveles. Se omiten las carpetas vacías.",
        "Preparando carpeta…",
        "Carpeta añadida a las subidas",
        "Sin archivos seleccionados. Se omiten las carpetas vacías.",
        "La selección de carpetas no está disponible aquí. Prueba un navegador compatible.",
        "La selección contiene rutas no seguras o en conflicto. Renombra los elementos y selecciona de nuevo.",
        "Se supera el límite de carpetas. Elige hasta 32 MiB, 256 elementos y 16 niveles.",
        "No se pudo leer el contenido. No se añadió nada. Selecciona de nuevo.",
    )
    Language.FRENCH -> FolderUploadCopy(
        "Importer un dossier",
        "32 Mio, 256 éléments, 16 niveaux maximum. Les dossiers vides sont omis.",
        "Préparation du dossier…",
        "Dossier ajouté aux transferts",
        "Aucun fichier sélectionné. Les dossiers vides sont omis.",
        "La sélection de dossiers est indisponible ici. Essayez un navigateur compatible.",
        "Chemins non sûrs ou en conflit. Renommez les éléments et sélectionnez à nouveau.",
        "Limite dépassée : 32 Mio, 256 éléments et 16 niveaux maximum.",
        "Lecture impossible. Aucun ajout. Sélectionnez à nouveau.",
    )
    Language.GERMAN -> FolderUploadCopy(
        "Ordner hochladen",
        "Bis 32 MiB, 256 Einträge, 16 Ebenen. Leere Ordner werden ausgelassen.",
        "Ordner wird vorbereitet…",
        "Ordner zur Uploadliste hinzugefügt",
        "Keine Dateien ausgewählt. Leere Ordner werden ausgelassen.",
        "Ordnerauswahl hier nicht verfügbar. Einen unterstützten Browser verwenden.",
        "Unsichere oder widersprüchliche Pfade. Einträge umbenennen und erneut auswählen.",
        "Ordnerlimit überschritten: höchstens 32 MiB, 256 Einträge und 16 Ebenen.",
        "Inhalt nicht lesbar. Nichts hinzugefügt. Erneut auswählen.",
    )
    Language.PORTUGUESE -> FolderUploadCopy(
        "Enviar pasta",
        "Até 32 MiB, 256 itens e 16 níveis. As pastas vazias são omitidas.",
        "A preparar pasta…",
        "Pasta adicionada aos envios",
        "Nenhum ficheiro selecionado. As pastas vazias são omitidas.",
        "Seleção de pastas indisponível aqui. Tente um navegador compatível.",
        "Caminhos inseguros ou em conflito. Altere os nomes e selecione novamente.",
        "Limite excedido: até 32 MiB, 256 itens e 16 níveis.",
        "Não foi possível ler o conteúdo. Nada foi adicionado. Selecione novamente.",
    )
    Language.CHINESE -> FolderUploadCopy(
        "上传文件夹",
        "最多 32 MiB、256 个项目、16 层。忽略空文件夹。",
        "正在准备文件夹…",
        "文件夹已加入上传队列",
        "未选择文件。忽略空文件夹。",
        "此处不支持选择文件夹。请使用支持此功能的浏览器。",
        "所选路径不安全或存在冲突。请重命名后重新选择。",
        "超出限制：最多 32 MiB、256 个项目和 16 层。",
        "无法读取所选内容。未添加文件。请重新选择。",
    )
    Language.JAPANESE -> FolderUploadCopy(
        "フォルダーをアップロード",
        "最大 32 MiB、256 項目、16 階層。空のフォルダーは省略されます。",
        "フォルダーを準備中…",
        "フォルダーをアップロードに追加しました",
        "ファイル未選択。空のフォルダーは省略されます。",
        "この環境ではフォルダーを選択できません。対応するブラウザーをお試しください。",
        "安全でないパスまたは競合があります。名前を変更して選択し直してください。",
        "上限を超えています。32 MiB、256 項目、16 階層以内で選択してください。",
        "内容を読み込めませんでした。追加されていません。選択し直してください。",
    )
}
