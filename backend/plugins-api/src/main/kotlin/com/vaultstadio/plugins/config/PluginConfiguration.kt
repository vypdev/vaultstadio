/**
 * VaultStadio Plugin Configuration DSL
 *
 * Provides a type-safe DSL for defining plugin configuration schemas.
 */

package com.vaultstadio.plugins.config

import kotlinx.serialization.Serializable

/**
 * Field types for configuration.
 */
@Serializable
enum class FieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    SELECT,
    MULTISELECT,
    PASSWORD,
    PATH,
    URL,
}

/**
 * Configuration field definition.
 */
@Serializable
data class ConfigField(
    val key: String,
    val label: String,
    val type: FieldType,
    val description: String = "",
    val defaultValue: String? = null,
    val required: Boolean = false,
    val validation: String? = null,
    val options: List<ConfigOption>? = null,
)

/**
 * Option for SELECT/MULTISELECT fields.
 */
@Serializable
data class ConfigOption(
    val value: String,
    val label: String,
)

/**
 * Configuration group.
 */
@Serializable
data class ConfigGroup(
    val key: String,
    val label: String,
    val fields: List<ConfigField> = emptyList(),
)

/**
 * Plugin configuration schema.
 */
@Serializable
data class PluginConfiguration(
    val groups: List<ConfigGroup> = emptyList(),
) {
    val allFields: List<ConfigField>
        get() = groups.flatMap { it.fields }
}

/**
 * DSL builder for configuration fields.
 */
class ConfigFieldBuilder(
    val key: String,
    val label: String,
    val type: FieldType,
) {
    var description: String = ""
    var defaultValue: Any? = null
    var required: Boolean = false
    var validation: String? = null
    var options: MutableList<ConfigOption> = mutableListOf()

    fun option(value: String, label: String) {
        options.add(ConfigOption(value, label))
    }

    fun build(): ConfigField = ConfigField(
        key = key,
        label = label,
        type = type,
        description = description,
        defaultValue = defaultValue?.toString(),
        required = required,
        validation = validation,
        options = options.takeIf { it.isNotEmpty() },
    )
}

/**
 * DSL builder for configuration groups.
 */
class ConfigGroupBuilder(
    val key: String,
    val label: String,
) {
    private val fields = mutableListOf<ConfigField>()

    fun field(key: String, label: String, type: FieldType, block: ConfigFieldBuilder.() -> Unit = {}) {
        val builder = ConfigFieldBuilder(key, label, type)
        builder.block()
        fields.add(builder.build())
    }

    fun build(): ConfigGroup = ConfigGroup(
        key = key,
        label = label,
        fields = fields,
    )
}

/**
 * DSL builder for plugin configuration.
 */
class PluginConfigurationBuilder {
    private val groups = mutableListOf<ConfigGroup>()

    fun group(key: String, label: String, block: ConfigGroupBuilder.() -> Unit) {
        val builder = ConfigGroupBuilder(key, label)
        builder.block()
        groups.add(builder.build())
    }

    fun build(): PluginConfiguration = PluginConfiguration(groups)
}

/**
 * DSL entry point for plugin configuration.
 */
fun pluginConfiguration(block: PluginConfigurationBuilder.() -> Unit): PluginConfiguration {
    val builder = PluginConfigurationBuilder()
    builder.block()
    return builder.build()
}
