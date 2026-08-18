package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The host treats everything a guest returns as untrusted. These use a module written to break
 * the contract, so the checks are tested against a real attempt rather than assumed to work.
 */
class HostileModuleTest {

    private GuestCall hostile;

    @BeforeEach
    void loadHostileModule() throws IOException {
        try (InputStream wasm = HostileModuleTest.class.getResourceAsStream("/hostile.wasm")) {
            assertNotNull(wasm, "hostile.wasm is missing from the test resources");
            hostile = new GuestCall(Instance.builder(Parser.parse(wasm)).build());
        }
    }

    @Test
    void rejectsAPointerOutsideGuestMemory() {
        GuestAbiException thrown =
                assertThrows(GuestAbiException.class, () -> hostile.callWithStringReturn("far", "x"));

        assertTrue(thrown.getMessage().contains("past the end of its memory"), thrown.getMessage());
    }

    @Test
    void rejectsAnAbsurdLength() {
        GuestAbiException thrown =
                assertThrows(GuestAbiException.class, () -> hostile.callWithStringReturn("liar", "x"));

        assertTrue(thrown.getMessage().contains("past the end of its memory"), thrown.getMessage());
    }

    @Test
    void rejectsBytesThatAreNotValidText() {
        GuestAbiException thrown =
                assertThrows(GuestAbiException.class, () -> hostile.callWithStringReturn("mangled", "x"));

        assertTrue(thrown.getMessage().contains("UTF-8"), thrown.getMessage());
    }

    @Test
    void rejectsANegativeOffset() {
        assertThrows(GuestAbiException.class, () -> hostile.callWithStringReturn("negative", "x"));
    }
}
