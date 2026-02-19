/**
 * VaultStadio Plugin Configuration Tests
 *
 * Unit tests for PluginConfiguration DSL, FieldType, ConfigField, ConfigGroup, and builders.
 */

package com.vaultstadio.plugins.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PluginConfigurationTest {

    @Nested
    inner class FieldTypeTests {

        @Test
        fun `all field types exist`() {
            val types = FieldType.entries
            assertTrue(types.contains(FieldType.STRING))
            assertTrue(types.contains(FieldType.NUMBER))
            assertTrue(types.contains(FieldType.BOOLEAN))
            assertTrue(types.contains(FieldType.SELECT))
            assertTrue(types.contains(FieldType.PASSWORD))
        }
    }

    @Nested
    inner class ConfigFieldTests {

        @Test
        fun `ConfigField holds key label type`() {
            val f = ConfigField(
                key = "api_key",
                label = "API Key",
                type = FieldType.STRING,
            )
            assertEquals("api_key", f.key)
            assertEquals("API Key", f.label)
            assertEquals(FieldType.STRING, f.type)
            assertEquals("", f.description)
            assertNull(f.defaultValue)
            assertEquals(false, f.required)
            assertNull(f.validation)
            assertNull(f.options)
        }

        @Test
        fun `ConfigField with all options`() {
            val f = ConfigField(
                key = "mode",
                label = "Mode",
                type = FieldType.SELECT,
                description = "Select mode",
                defaultValue = "fast",
                required = true,
                validation = "oneOf:fast,slow",
                options = listOf(ConfigOption("fast", "Fast"), ConfigOption("slow", "Slow")),
            )
            assertEquals("oneOf:fast,slow", f.validation)
            assertEquals(2, f.options?.size)
        }
    }

    @Nested
    inner class ConfigOptionTests {

        @Test
        fun `ConfigOption value and label`() {
            val o = ConfigOption("val", "Label")
            assertEquals("val", o.value)
            assertEquals("Label", o.label)
        }
    }

    @Nested
    inner class ConfigGroupTests {

        @Test
        fun `ConfigGroup holds key label and fields`() {
            val g = ConfigGroup(
                key = "general",
                label = "General",
                fields = listOf(
                    ConfigField("name", "Name", FieldType.STRING),
                ),
            )
            assertEquals("general", g.key)
            assertEquals("General", g.label)
            assertEquals(1, g.fields.size)
            assertEquals("name", g.fields[0].key)
        }
    }

    @Nested
    inner class PluginConfigurationTests {

        @Test
        fun `empty configuration has no groups`() {
            val c = PluginConfiguration()
            assertTrue(c.groups.isEmpty())
            assertTrue(c.allFields.isEmpty())
        }

        @Test
        fun `allFields flattens group fields`() {
            val c = PluginConfiguration(
                groups = listOf(
                    ConfigGroup("g1", "G1", listOf(ConfigField("a", "A", FieldType.STRING))),
                    ConfigGroup("g2", "G2", listOf(ConfigField("b", "B", FieldType.BOOLEAN))),
                ),
            )
            assertEquals(2, c.allFields.size)
            assertEquals("a", c.allFields[0].key)
            assertEquals("b", c.allFields[1].key)
        }
    }

    @Nested
    inner class ConfigFieldBuilderTests {

        @Test
        fun `ConfigFieldBuilder build produces ConfigField`() {
            val b = ConfigFieldBuilder("key", "Label", FieldType.STRING)
            b.description = "desc"
            b.defaultValue = "default"
            b.required = true
            val f = b.build()
            assertEquals("key", f.key)
            assertEquals("Label", f.label)
            assertEquals(FieldType.STRING, f.type)
            assertEquals("desc", f.description)
            assertEquals("default", f.defaultValue)
            assertEquals(true, f.required)
        }

        @Test
        fun `ConfigFieldBuilder option adds to options`() {
            val b = ConfigFieldBuilder("sel", "Select", FieldType.SELECT)
            b.option("a", "Option A")
            b.option("b", "Option B")
            val f = b.build()
            assertNotNull(f.options)
            assertEquals(2, f.options!!.size)
            assertEquals("a", f.options!![0].value)
            assertEquals("b", f.options!![1].value)
        }
    }

    @Nested
    inner class ConfigGroupBuilderTests {

        @Test
        fun `ConfigGroupBuilder field adds field`() {
            val b = ConfigGroupBuilder("grp", "Group")
            b.field("f1", "Field 1", FieldType.STRING)
            b.field("f2", "Field 2", FieldType.NUMBER) {
                description = "number field"
            }
            val g = b.build()
            assertEquals(2, g.fields.size)
            assertEquals("f1", g.fields[0].key)
            assertEquals("f2", g.fields[1].key)
            assertEquals("number field", g.fields[1].description)
        }
    }

    @Nested
    inner class PluginConfigurationBuilderTests {

        @Test
        fun `pluginConfiguration DSL produces PluginConfiguration`() {
            val config = pluginConfiguration {
                group("general", "General") {
                    field("name", "Name", FieldType.STRING) {
                        description = "Plugin name"
                        required = true
                    }
                }
                group("advanced", "Advanced") {
                    field("timeout", "Timeout", FieldType.NUMBER) {
                        defaultValue = 30
                    }
                }
            }
            assertEquals(2, config.groups.size)
            assertEquals("general", config.groups[0].key)
            assertEquals("advanced", config.groups[1].key)
            assertEquals(2, config.allFields.size)
            assertEquals("name", config.allFields[0].key)
            assertEquals("timeout", config.allFields[1].key)
        }
    }
}
