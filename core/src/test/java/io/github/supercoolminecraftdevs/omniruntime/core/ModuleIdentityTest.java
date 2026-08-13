package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ModuleIdentityTest {

    @Test
    void splitsNamespaceFromName() {
        ModuleIdentity identity = ModuleIdentity.parse("elchi.greeter");

        assertEquals("elchi", identity.namespace());
        assertEquals("greeter", identity.name());
        assertEquals("elchi.greeter", identity.toString());
    }

    @Test
    void usesTheFullIdentityAsTheDirectoryName() {
        assertEquals("elchi.greeter", ModuleIdentity.parse("elchi.greeter").directoryName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.b", "elchi.greeter", "elchi.world-edit", "team7.tool2", "elchi.a1-b2-c3"})
    void acceptsValidIdentities(String identity) {
        assertEquals(identity, ModuleIdentity.parse(identity).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "greeter",
        "elchi.greeter.extra",
        ".greeter",
        "elchi.",
        "Elchi.greeter",
        "elchi.Greeter",
        "elchi.greeter plugin",
        "elchi.greeter_plugin",
        "elchi.greeter/",
        "elchi..greeter",
        "1elchi.greeter",
        "-elchi.greeter",
        "elchi.greeter-",
        "elchi.gre@ter"
    })
    void rejectsInvalidIdentities(String identity) {
        assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse(identity));
    }

    @Test
    void rejectsPartsLongerThanTheLimit() {
        String tooLong = "e" + "a".repeat(64);

        assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse(tooLong + ".greeter"));
        assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse("elchi." + tooLong));
    }

    @Test
    void rejectsPathTraversalAttempts() {
        assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse("../etc.passwd"));
        assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse("elchi.\\..\\grants"));
    }

    @Test
    void explainsWhyAnIdentityWasRejected() {
        IllegalArgumentException uppercase =
                assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse("Elchi.greeter"));
        assertTrue(uppercase.getMessage().contains("lowercase"), uppercase.getMessage());

        IllegalArgumentException noNamespace =
                assertThrows(IllegalArgumentException.class, () -> ModuleIdentity.parse("greeter"));
        assertTrue(noNamespace.getMessage().contains("namespace"), noNamespace.getMessage());
    }

    @Test
    void comparesByValue() {
        assertEquals(ModuleIdentity.parse("elchi.greeter"), ModuleIdentity.parse("elchi.greeter"));
        assertEquals(
                ModuleIdentity.parse("elchi.greeter").hashCode(),
                ModuleIdentity.parse("elchi.greeter").hashCode());
        assertNotEquals(ModuleIdentity.parse("elchi.greeter"), ModuleIdentity.parse("script.greeter"));
    }
}
