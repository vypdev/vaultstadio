package com.vaultstadio.app.core.resources

/** Localized names for inputs that previously had hardcoded or absent names. */
enum class InputLabel(internal val translations: List<String>) {
    CURRENT_PASSWORD(
        listOf(
            "Current password",
            "Contraseña actual",
            "Mot de passe actuel",
            "Aktuelles Passwort",
            "Senha atual",
            "当前密码",
            "現在のパスワード",
        ),
    ),
    NEW_PASSWORD(
        listOf(
            "New password",
            "Nueva contraseña",
            "Nouveau mot de passe",
            "Neues Passwort",
            "Nova senha",
            "新密码",
            "新しいパスワード",
        ),
    ),
    REMOTE_USER(
        listOf(
            "Remote user ID",
            "ID de usuario remoto",
            "Identifiant utilisateur distant",
            "Entfernte Benutzer-ID",
            "ID de usuário remoto",
            "远程用户 ID",
            "リモートユーザー ID",
        ),
    ),
    REMOTE_INSTANCE(
        listOf(
            "Remote instance",
            "Instancia remota",
            "Instance distante",
            "Entfernte Instanz",
            "Instância remota",
            "远程实例",
            "リモートインスタンス",
        ),
    ),
    DISPLAY_NAME(
        listOf("Display name", "Nombre visible", "Nom d’affichage", "Anzeigename", "Nome de exibição", "显示名称", "表示名"),
    ),
    DOCUMENT_CONTENT(
        listOf(
            "Document content",
            "Contenido del documento",
            "Contenu du document",
            "Dokumentinhalt",
            "Conteúdo do documento",
            "文档内容",
            "文書の内容",
        ),
    ),
    QUOTA(
        listOf(
            "Storage quota",
            "Cuota de almacenamiento",
            "Quota de stockage",
            "Speicherkontingent",
            "Cota de armazenamento",
            "存储配额",
            "ストレージ容量",
        ),
    ),
    DEVICE_NAME(
        listOf(
            "Device name",
            "Nombre del dispositivo",
            "Nom de l’appareil",
            "Gerätename",
            "Nome do dispositivo",
            "设备名称",
            "デバイス名",
        ),
    ),
    SHOW_PASSWORD(
        listOf(
            "Show password",
            "Mostrar contraseña",
            "Afficher le mot de passe",
            "Passwort anzeigen",
            "Mostrar senha",
            "显示密码",
            "パスワードを表示",
        ),
    ),
    HIDE_PASSWORD(
        listOf(
            "Hide password",
            "Ocultar contraseña",
            "Masquer le mot de passe",
            "Passwort ausblenden",
            "Ocultar senha",
            "隐藏密码",
            "パスワードを非表示",
        ),
    ),
}

fun StringResources.inputLabel(label: InputLabel): String {
    val locale = when (this) {
        SpanishStrings -> 1
        FrenchStrings -> 2
        GermanStrings -> 3
        PortugueseStrings -> 4
        ChineseStrings -> 5
        JapaneseStrings -> 6
        else -> 0
    }
    return label.translations[locale]
}
