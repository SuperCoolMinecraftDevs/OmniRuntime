package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuestCallTest {

    private GuestCall guest;

    @BeforeEach
    void loadGreeter() throws IOException {
        guest = new GuestCall(Instance.builder(greeterModule()).build());
    }

    private static WasmModule greeterModule() throws IOException {
        try (InputStream wasm = GuestCallTest.class.getResourceAsStream("/greeter.wasm")) {
            assertNotNull(wasm, "greeter.wasm is missing. Build it with cargo, see guests/greeter.");
            return Parser.parse(wasm);
        }
    }

    @Test
    void passesAStringInAndReadsOneBack() {
        assertEquals("Hello, Elchi!", guest.callWithStringReturn("greet", "Elchi"));
    }

    @Test
    void handlesAnEmptyString() {
        assertEquals("Hello, !", guest.callWithStringReturn("greet", ""));
    }

    @Test
    void handlesTextOutsideAscii() {
        assertEquals("Hello, Zurich cafe!", guest.callWithStringReturn("greet", "Zurich cafe"));
        assertEquals(
                "Hello, " + "\u00e4\u00f6\u00fc" + "!",
                guest.callWithStringReturn("greet", "\u00e4\u00f6\u00fc"));
    }

    @Test
    void handlesAStringLargerThanOnePageOfMemory() {
        String long_ = "a".repeat(100_000);

        assertEquals("Hello, " + long_ + "!", guest.callWithStringReturn("greet", long_));
    }

    @Test
    void survivesRepeatedCallsWithoutRunningOutOfMemory() {
        for (int i = 0; i < 2_000; i++) {
            assertEquals("Hello, " + i + "!", guest.callWithStringReturn("greet", String.valueOf(i)));
        }
    }

    @Test
    void passesBytesIn() {
        byte[] data = "the quick brown fox".getBytes(StandardCharsets.UTF_8);

        int expected = 0;
        for (byte b : data) {
            expected = expected * 31 + (b & 0xFF);
        }

        assertEquals(expected, guest.callWithBytesArgument("checksum", data));
    }

    @Test
    void passesEmptyBytesIn() {
        assertEquals(0, guest.callWithBytesArgument("checksum", new byte[0]));
    }

    @Test
    void reportsAMissingExportByName() {
        GuestAbiException thrown =
                assertThrows(GuestAbiException.class, () -> guest.callWithStringReturn("nope", "x"));

        assertTrue(thrown.getMessage().contains("nope"), thrown.getMessage());
    }
}
