package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Builds module files for tests, the way a packaging tool eventually will. */
final class TestModules {

    private TestModules() {}

    static byte[] base() throws IOException {
        try (InputStream wasm = TestModules.class.getResourceAsStream("/add.wasm")) {
            assertNotNull(wasm, "add.wasm is missing from the test resources");
            return wasm.readAllBytes();
        }
    }

    static byte[] withManifest(String manifest) throws IOException {
        return withSection(base(), ModuleManifest.SECTION_NAME, manifest.getBytes(StandardCharsets.UTF_8));
    }

    static Path writeModule(Path directory, String fileName, String manifest) throws IOException {
        Path file = directory.resolve(fileName);
        Files.write(file, withManifest(manifest));
        return file;
    }

    static String manifestFor(String identity) {
        return "identity: " + identity + "\nversion: 1.0.0\nabi: 1\n";
    }

    static byte[] withSection(byte[] module, String name, byte[] payload) throws IOException {
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
