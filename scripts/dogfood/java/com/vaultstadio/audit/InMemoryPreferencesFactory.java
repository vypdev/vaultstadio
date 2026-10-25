package com.vaultstadio.audit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/** Test-only JVM provider: GUI dogfooding must never replace a real desktop session. */
public final class InMemoryPreferencesFactory implements PreferencesFactory {
    private final Preferences user = new MemoryPreferences(null, "");
    private final Preferences system = new MemoryPreferences(null, "");

    public InMemoryPreferencesFactory() {
        System.err.println("VaultStadio audit: in-memory Java preferences active");
    }

    @Override
    public Preferences userRoot() {
        return user;
    }

    @Override
    public Preferences systemRoot() {
        return system;
    }

    /** Validate isolation before launching the application with this provider. */
    public static void main(String[] arguments) {
        if (!InMemoryPreferencesFactory.class.getName().equals(
                System.getProperty("java.util.prefs.PreferencesFactory"))) {
            throw new IllegalStateException("Explicit audit preferences provider is required");
        }
        Preferences root = Preferences.userRoot();
        if (!(root instanceof MemoryPreferences)) {
            throw new IllegalStateException("Refusing to write to non-audit preferences");
        }
        Preferences node = root.node("vaultstadio-audit-probe");
        node.put("probe", "only-in-memory");
        if (!"only-in-memory".equals(node.get("probe", null))) {
            throw new IllegalStateException("Audit preferences round trip failed");
        }
        node.remove("probe");
        if (node.get("probe", null) != null) {
            throw new IllegalStateException("Audit preferences removal failed");
        }
        System.out.println("PASS: isolated in-memory preferences");
    }

    private static final class MemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new ConcurrentHashMap<>();
        private final Map<String, MemoryPreferences> children = new ConcurrentHashMap<>();

        private MemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
        }

        @Override protected void putSpi(String key, String value) { values.put(key, value); }
        @Override protected String getSpi(String key) { return values.get(key); }
        @Override protected void removeSpi(String key) { values.remove(key); }
        @Override protected void removeNodeSpi() { values.clear(); children.clear(); }
        @Override protected String[] keysSpi() { return values.keySet().toArray(new String[0]); }
        @Override protected String[] childrenNamesSpi() { return children.keySet().toArray(new String[0]); }
        @Override protected AbstractPreferences childSpi(String name) {
            return children.computeIfAbsent(name, key -> new MemoryPreferences(this, key));
        }
        @Override protected void syncSpi() { }
        @Override protected void flushSpi() { }
    }
}
