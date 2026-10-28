package com.vaultstadio.app.feature.files

import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.domain.upload.FolderSelectionProblem

internal fun uploadSelectionProblemCopy(
    language: Language,
    action: UploadAction,
    problem: FolderSelectionProblem,
): String {
    if (action == UploadAction.Folder) return folderUploadCopy(language).problem(problem)
    val copy = when (language) {
        Language.ENGLISH -> FileSelectionProblemCopy(
            "File selection is unavailable here. Try an environment with file selection support.",
            "The selected files could not be read. Nothing was added. Select the files again.",
            "The selected files could not be prepared. Nothing was added. Select the files again.",
        )
        Language.SPANISH -> FileSelectionProblemCopy(
            "La selección de archivos no está disponible aquí. Prueba un entorno compatible.",
            "No se pudieron leer los archivos. No se añadió nada. Selecciona los archivos de nuevo.",
            "No se pudieron preparar los archivos. No se añadió nada. Selecciona los archivos de nuevo.",
        )
        Language.FRENCH -> FileSelectionProblemCopy(
            "La sélection de fichiers est indisponible ici. Essayez un environnement compatible.",
            "Impossible de lire les fichiers sélectionnés. Aucun ajout. Sélectionnez à nouveau les fichiers.",
            "Impossible de préparer les fichiers sélectionnés. Aucun ajout. Sélectionnez à nouveau les fichiers.",
        )
        Language.GERMAN -> FileSelectionProblemCopy(
            "Die Dateiauswahl ist hier nicht verfügbar. Eine unterstützte Umgebung verwenden.",
            "Die ausgewählten Dateien konnten nicht gelesen werden. Nichts hinzugefügt. Dateien erneut auswählen.",
            "Die ausgewählten Dateien konnten nicht vorbereitet werden. Nichts hinzugefügt. Dateien erneut auswählen.",
        )
        Language.PORTUGUESE -> FileSelectionProblemCopy(
            "A seleção de ficheiros não está disponível aqui. Tente um ambiente compatível.",
            "Não foi possível ler os ficheiros. Nada foi adicionado. Selecione os ficheiros novamente.",
            "Não foi possível preparar os ficheiros. Nada foi adicionado. Selecione os ficheiros novamente.",
        )
        Language.CHINESE -> FileSelectionProblemCopy(
            "此处无法选择文件。请使用支持文件选择的环境。",
            "无法读取所选文件。未添加文件。请重新选择文件。",
            "无法准备所选文件。未添加文件。请重新选择文件。",
        )
        Language.JAPANESE -> FileSelectionProblemCopy(
            "この環境ではファイルを選択できません。対応する環境をお試しください。",
            "選択したファイルを読み込めませんでした。追加されていません。ファイルを選択し直してください。",
            "選択したファイルを準備できませんでした。追加されていません。ファイルを選択し直してください。",
        )
    }
    return when (problem) {
        FolderSelectionProblem.UNSUPPORTED -> copy.unavailable
        FolderSelectionProblem.READ_FAILED -> copy.unreadable
        else -> copy.preparationFailed
    }
}

private data class FileSelectionProblemCopy(
    val unavailable: String,
    val unreadable: String,
    val preparationFailed: String,
)
