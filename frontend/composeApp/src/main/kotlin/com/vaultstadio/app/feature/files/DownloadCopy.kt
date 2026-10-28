package com.vaultstadio.app.feature.files

import com.vaultstadio.app.core.resources.Language
import com.vaultstadio.app.feature.files.transfer.DownloadProblem

internal data class DownloadCopy(
    val fetching: String,
    val saving: String,
    val saved: String,
    val handed: String,
    val canceled: String,
    val canceledSaving: String,
    val retry: String,
    val authentication: String,
    val accessDenied: String,
    val notFound: String,
    val tooLarge: String,
    val invalidSelection: String,
    val invalidName: String,
    val contentChanged: String,
    val network: String,
    val unavailable: String,
    val saveFailed: String,
    val unsupported: String,
) {
    fun problem(problem: DownloadProblem): String = when (problem) {
        DownloadProblem.AUTHENTICATION -> authentication
        DownloadProblem.ACCESS_DENIED -> accessDenied
        DownloadProblem.NOT_FOUND -> notFound
        DownloadProblem.TOO_LARGE -> tooLarge
        DownloadProblem.INVALID_SELECTION -> invalidSelection
        DownloadProblem.INVALID_NAME -> invalidName
        DownloadProblem.CONTENT_CHANGED -> contentChanged
        DownloadProblem.NETWORK -> network
        DownloadProblem.UNAVAILABLE -> unavailable
        DownloadProblem.SAVE_FAILED -> saveFailed
        DownloadProblem.UNSUPPORTED -> unsupported
    }
}

internal fun downloadCopy(language: Language): DownloadCopy = when (language) {
    Language.ENGLISH -> DownloadCopy(
        fetching = "Downloading…",
        saving = "Choose where to save",
        saved = "File saved",
        handed = "Sent to the browser",
        canceled = "Download canceled",
        canceledSaving = "Cancel requested. Saving may already have finished; check the destination.",
        retry = "Retry",
        authentication = "Sign in again. Nothing was saved.",
        accessDenied = "You do not have access. Nothing was saved.",
        notFound = "Item not found. Refresh the list and retry.",
        tooLarge = "Download or archive limit exceeded. Choose fewer or smaller files. Nothing was saved.",
        invalidSelection = "Choose unique items or a smaller folder. Nothing was saved.",
        invalidName = "This filename cannot be saved safely. Rename the item and retry.",
        contentChanged = "Content changed or did not complete. Refresh and retry. Nothing was saved.",
        network = "Connection failed. Retry the download. Nothing was saved.",
        unavailable = "Download unavailable. Retry later. Nothing was saved.",
        saveFailed = "Saving failed. Check the destination and retry.",
        unsupported = "Saving is unavailable on this platform. Use Web or desktop.",
    )
    Language.SPANISH -> DownloadCopy(
        fetching = "Descargando…",
        saving = "Elige dónde guardar",
        saved = "Archivo guardado",
        handed = "Enviado al navegador",
        canceled = "Descarga cancelada",
        canceledSaving = "Se ha pedido cancelar. Puede que ya se haya guardado; comprueba el destino.",
        retry = "Reintentar",
        authentication = "Inicia sesión de nuevo. No se ha guardado nada.",
        accessDenied = "No tienes acceso. No se ha guardado nada.",
        notFound = "No se encuentra el archivo. Actualiza la lista y reintenta.",
        tooLarge = "Se supera el límite de descarga o ZIP. Elige menos archivos o más pequeños. " +
            "No se ha guardado nada.",
        invalidSelection = "Elige elementos distintos o una carpeta menor. No se ha guardado nada.",
        invalidName = "El nombre no se puede guardar de forma segura. Renombra el archivo y reintenta.",
        contentChanged = "El contenido cambió o quedó incompleto. Actualiza y reintenta. No se ha guardado nada.",
        network = "Falló la conexión. Reintenta la descarga. No se ha guardado nada.",
        unavailable = "Descarga no disponible. Reintenta más tarde. No se ha guardado nada.",
        saveFailed = "No se pudo guardar. Comprueba el destino y reintenta.",
        unsupported = "Guardar no está disponible en esta plataforma. Usa Web o escritorio.",
    )
    Language.FRENCH -> DownloadCopy(
        fetching = "Téléchargement…",
        saving = "Choisissez où enregistrer",
        saved = "Fichier enregistré",
        handed = "Transmis au navigateur",
        canceled = "Téléchargement annulé",
        canceledSaving = "Annulation demandée. Vérifiez si le fichier a déjà été enregistré.",
        retry = "Réessayer",
        authentication = "Reconnectez-vous. Aucun fichier enregistré.",
        accessDenied = "Accès refusé. Aucun fichier enregistré.",
        notFound = "Élément introuvable. Actualisez et réessayez.",
        tooLarge = "Limite de téléchargement ou ZIP dépassée. " +
            "Choisissez moins de fichiers ou des fichiers plus petits.",
        invalidSelection = "Choisissez des éléments distincts ou un dossier plus petit. Aucun fichier enregistré.",
        invalidName = "Nom de fichier non sûr. Renommez le fichier et réessayez.",
        contentChanged = "Contenu modifié ou incomplet. Actualisez et réessayez. Aucun fichier enregistré.",
        network = "Connexion interrompue. Réessayez. Aucun fichier enregistré.",
        unavailable = "Téléchargement indisponible. Réessayez plus tard.",
        saveFailed = "Échec de sauvegarde. Vérifiez la destination et réessayez.",
        unsupported = "Sauvegarde indisponible sur cette plateforme. Utilisez Web ou un ordinateur.",
    )
    Language.GERMAN -> DownloadCopy(
        fetching = "Wird heruntergeladen…",
        saving = "Speicherort auswählen",
        saved = "Datei gespeichert",
        handed = "An den Browser übergeben",
        canceled = "Download abgebrochen",
        canceledSaving = "Abbruch angefordert. Prüfen, ob die Datei bereits gespeichert wurde.",
        retry = "Erneut versuchen",
        authentication = "Erneut anmelden. Es wurde nichts gespeichert.",
        accessDenied = "Zugriff verweigert. Es wurde nichts gespeichert.",
        notFound = "Datei nicht gefunden. Liste aktualisieren und erneut versuchen.",
        tooLarge = "Download- oder ZIP-Limit überschritten. Weniger oder kleinere Dateien auswählen.",
        invalidSelection = "Eindeutige Elemente oder einen kleineren Ordner auswählen. Nichts gespeichert.",
        invalidName = "Unsicherer Dateiname. Datei umbenennen und erneut versuchen.",
        contentChanged = "Inhalt geändert oder unvollständig. Aktualisieren und erneut versuchen. Nichts gespeichert.",
        network = "Verbindung fehlgeschlagen. Download erneut versuchen. Nichts gespeichert.",
        unavailable = "Download nicht verfügbar. Später erneut versuchen.",
        saveFailed = "Speichern fehlgeschlagen. Ziel prüfen und erneut versuchen.",
        unsupported = "Speichern hier nicht verfügbar. Web oder Desktop verwenden.",
    )
    Language.PORTUGUESE -> DownloadCopy(
        fetching = "A transferir…",
        saving = "Escolha onde guardar",
        saved = "Ficheiro guardado",
        handed = "Enviado ao navegador",
        canceled = "Transferência cancelada",
        canceledSaving = "Cancelamento pedido. Verifique se o ficheiro já foi guardado.",
        retry = "Tentar novamente",
        authentication = "Inicie sessão novamente. Nada foi guardado.",
        accessDenied = "Sem acesso. Nada foi guardado.",
        notFound = "Ficheiro não encontrado. Atualize e tente novamente.",
        tooLarge = "Limite de transferência ou ZIP excedido. Escolha menos ficheiros ou ficheiros menores.",
        invalidSelection = "Escolha elementos distintos ou uma pasta menor. Nada foi guardado.",
        invalidName = "Nome inseguro. Mude o nome do ficheiro e tente novamente.",
        contentChanged = "Conteúdo alterado ou incompleto. Atualize e tente novamente. Nada foi guardado.",
        network = "Falha na ligação. Tente novamente. Nada foi guardado.",
        unavailable = "Transferência indisponível. Tente mais tarde.",
        saveFailed = "Falha ao guardar. Verifique o destino e tente novamente.",
        unsupported = "Guardar indisponível nesta plataforma. Use Web ou computador.",
    )
    Language.CHINESE -> DownloadCopy(
        fetching = "正在下载…",
        saving = "选择保存位置",
        saved = "文件已保存",
        handed = "已交给浏览器",
        canceled = "下载已取消",
        canceledSaving = "已请求取消。保存可能已完成，请检查目标位置。",
        retry = "重试",
        authentication = "请重新登录。未保存任何文件。",
        accessDenied = "没有访问权限。未保存任何文件。",
        notFound = "找不到文件。请刷新列表后重试。",
        tooLarge = "超过下载或压缩包限制。请选择更少或更小的文件。",
        invalidSelection = "请选择不重复的项目或较小的文件夹。未保存任何文件。",
        invalidName = "文件名不安全。请重命名后重试。",
        contentChanged = "内容已更改或不完整。请刷新后重试。未保存任何文件。",
        network = "连接失败。请重试下载。未保存任何文件。",
        unavailable = "暂时无法下载。请稍后重试。",
        saveFailed = "保存失败。请检查目标位置后重试。",
        unsupported = "此平台暂不支持保存。请使用网页或桌面版。",
    )
    Language.JAPANESE -> DownloadCopy(
        fetching = "ダウンロード中…",
        saving = "保存先を選択",
        saved = "保存しました",
        handed = "ブラウザーに渡しました",
        canceled = "ダウンロードを中止しました",
        canceledSaving = "中止を要求しました。保存済みの可能性があるため保存先を確認してください。",
        retry = "再試行",
        authentication = "再ログインしてください。保存されていません。",
        accessDenied = "アクセス権がありません。保存されていません。",
        notFound = "ファイルがありません。更新して再試行してください。",
        tooLarge = "ダウンロードまたはZIPの上限を超えました。ファイル数やサイズを減らしてください。",
        invalidSelection = "重複のない項目か小さいフォルダーを選択してください。保存されていません。",
        invalidName = "安全でないファイル名です。名前を変更して再試行してください。",
        contentChanged = "内容が変更されたか不完全です。更新して再試行してください。保存されていません。",
        network = "接続に失敗しました。再試行してください。保存されていません。",
        unavailable = "現在ダウンロードできません。後で再試行してください。",
        saveFailed = "保存に失敗しました。保存先を確認して再試行してください。",
        unsupported = "この環境では保存できません。Web版かデスクトップ版を使用してください。",
    )
}
