package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModuleManifestTest {

    private static final String MINIMAL = """
            identity: elchi.greeter
            version: 1.0.0
            abi: 1
            """;

    @Test
    void readsTheMinimalManifest() {
        ModuleManifest manifest = ModuleManifest.parse(MINIMAL);

        assertEquals("elchi.greeter", manifest.identity().toString());
        assertEquals("1.0.0", manifest.version());
        assertEquals(1, manifest.abiVersion());
        assertTrue(manifest.capabilities().isEmpty());
    }

    @Test
    void readsCapabilityRequests() {
        ModuleManifest manifest = ModuleManifest.parse(MINIMAL + """
                requires: fs:read modules/elchi.greeter
                optional: net:https api.example.com
                """);

        List<CapabilityRequest> required = manifest.requiredCapabilities();
        assertEquals(1, required.size());
        assertEquals("fs:read", required.get(0).name());
        assertEquals("modules/elchi.greeter", required.get(0).scope());
        assertTrue(required.get(0).isScoped());

        List<CapabilityRequest> optional = manifest.optionalCapabilities();
        assertEquals(1, optional.size());
        assertEquals("net:https", optional.get(0).name());
        assertFalse(optional.get(0).required());
    }

    @Test
    void readsAnUnscopedRequest() {
        ModuleManifest manifest = ModuleManifest.parse(MINIMAL + "requires: server:events\n");

        assertFalse(manifest.requiredCapabilities().get(0).isScoped());
    }

    @Test
    void ignoresBlankLinesAndComments() {
        ModuleManifest manifest = ModuleManifest.parse("""
                # what this module is

                identity: elchi.greeter

                version: 1.0.0
                # built against
                abi: 1
                """);

        assertEquals("elchi.greeter", manifest.identity().toString());
    }

    @Test
    void toleratesWindowsLineEndings() {
        ModuleManifest manifest = ModuleManifest.parse("identity: elchi.greeter\r\nversion: 1.0.0\r\nabi: 1\r\n");

        assertEquals("1.0.0", manifest.version());
    }

    @Test
    void rejectsAMissingIdentity() {
        ManifestException thrown =
                assertThrows(ManifestException.class, () -> ModuleManifest.parse("version: 1.0.0\nabi: 1\n"));

        assertTrue(thrown.getMessage().contains("identity"), thrown.getMessage());
    }

    @Test
    void rejectsAMissingVersion() {
        assertThrows(ManifestException.class, () -> ModuleManifest.parse("identity: elchi.greeter\nabi: 1\n"));
    }

    @Test
    void rejectsAMissingAbiVersion() {
        assertThrows(ManifestException.class, () -> ModuleManifest.parse("identity: elchi.greeter\nversion: 1.0.0\n"));
    }

    @Test
    void rejectsAnAbiVersionThisHostDoesNotSpeak() {
        ManifestException thrown = assertThrows(
                ManifestException.class,
                () -> ModuleManifest.parse("identity: elchi.greeter\nversion: 1.0.0\nabi: 99\n"));

        assertTrue(thrown.getMessage().contains("99"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains(String.valueOf(ModuleManifest.SUPPORTED_ABI_VERSION)), thrown.getMessage());
    }

    @Test
    void rejectsAnAbiVersionThatIsNotANumber() {
        assertThrows(
                ManifestException.class,
                () -> ModuleManifest.parse("identity: elchi.greeter\nversion: 1.0.0\nabi: latest\n"));
    }

    @Test
    void rejectsAnUnknownKeyRatherThanIgnoringIt() {
        ManifestException thrown =
                assertThrows(ManifestException.class, () -> ModuleManifest.parse(MINIMAL + "identtiy: typo\n"));

        assertTrue(thrown.getMessage().contains("identtiy"), thrown.getMessage());
    }

    @Test
    void rejectsARepeatedKey() {
        ManifestException thrown =
                assertThrows(ManifestException.class, () -> ModuleManifest.parse(MINIMAL + "version: 2.0.0\n"));

        assertTrue(thrown.getMessage().contains("more than once"), thrown.getMessage());
    }

    @Test
    void rejectsAnEmptyValue() {
        assertThrows(ManifestException.class, () -> ModuleManifest.parse("identity:\nversion: 1.0.0\nabi: 1\n"));
    }

    @Test
    void rejectsALineWithNoColon() {
        assertThrows(ManifestException.class, () -> ModuleManifest.parse(MINIMAL + "this is not a setting\n"));
    }

    @Test
    void rejectsAnInvalidIdentity() {
        assertThrows(ManifestException.class, () -> ModuleManifest.parse("identity: Greeter\nversion: 1.0.0\nabi: 1\n"));
    }

    @Test
    void reportsTheLineNumberOfTheProblem() {
        ManifestException thrown = assertThrows(
                ManifestException.class,
                () -> ModuleManifest.parse("identity: elchi.greeter\nversion: 1.0.0\nabi: 1\nnonsense\n"));

        assertTrue(thrown.getMessage().contains("Line 4"), thrown.getMessage());
    }

    @Test
    void rejectsACapabilityNameThatIsNotLowercase() {
        assertThrows(ManifestException.class, () -> ModuleManifest.parse(MINIMAL + "requires: FS:read /tmp\n"));
    }
}
