package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ManifestInWasmFileTest {

    @Test
    void readsAManifestCarriedInsideTheModule() throws IOException {
        WasmModule module = Parser.parse(withManifest("""
                identity: elchi.greeter
                version: 2.1.0
                abi: 1
                requires: fs:read modules/elchi.greeter
                """));

        ModuleManifest manifest = ModuleManifest.readFrom(module);

        assertEquals("elchi.greeter", manifest.identity().toString());
        assertEquals("2.1.0", manifest.version());
        assertEquals(1, manifest.requiredCapabilities().size());
    }

    @Test
    void leavesTheModuleRunnable() throws IOException {
        WasmModule module = Parser.parse(withManifest("identity: elchi.greeter\nversion: 1.0.0\nabi: 1\n"));

        assertEquals(
                7,
                com.dylibso.chicory.runtime.Instance.builder(module).build().export("add").apply(3, 4)[0]);
    }

    @Test
    void rejectsAModuleWithNoManifest() throws IOException {
        WasmModule module = Parser.parse(addModuleBytes());

        ManifestException thrown = assertThrows(ManifestException.class, () -> ModuleManifest.readFrom(module));

        assertTrue(thrown.getMessage().contains(ModuleManifest.SECTION_NAME), thrown.getMessage());
    }

    @Test
    void rejectsAManifestThatIsNotValidText() throws IOException {
        byte[] wasm = appendCustomSection(
                addModuleBytes(), ModuleManifest.SECTION_NAME, new byte[] {(byte) 0xC3, (byte) 0x28});

        WasmModule module = Parser.parse(wasm);

        ManifestException thrown = assertThrows(ManifestException.class, () -> ModuleManifest.readFrom(module));

        assertTrue(thrown.getMessage().contains("UTF-8"), thrown.getMessage());
    }

    private static byte[] withManifest(String manifest) throws IOException {
        return appendCustomSection(
                addModuleBytes(), ModuleManifest.SECTION_NAME, manifest.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] addModuleBytes() throws IOException {
        try (InputStream wasm = ManifestInWasmFileTest.class.getResourceAsStream("/add.wasm")) {
            assertNotNull(wasm, "add.wasm is missing from the test resources");
            return wasm.readAllBytes();
        }
    }

    /**
     * Appends a custom section, the way a packaging tool would. Kept here rather than in the host,
     * because the host only ever reads them.
     */
    private static byte[] appendCustomSection(byte[] module, String name, byte[] payload) throws IOException {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        byte[] rawName = name.getBytes(StandardCharsets.UTF_8);
        writeUnsigned(content, rawName.length);
        content.write(rawName);
        content.write(payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(module);
        out.write(0);
        writeUnsigned(out, content.size());
        out.write(content.toByteArray());
        return out.toByteArray();
    }

    private static void writeUnsigned(ByteArrayOutputStream out, int value) {
        int remaining = value;
        do {
            int b = remaining & 0x7F;
            remaining >>>= 7;
            out.write(remaining == 0 ? b : b | 0x80);
        } while (remaining != 0);
    }
}
