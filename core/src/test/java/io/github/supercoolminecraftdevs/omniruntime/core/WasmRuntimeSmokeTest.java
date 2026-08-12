package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class WasmRuntimeSmokeTest {

    private static WasmModule addModule() throws IOException {
        try (InputStream wasm = WasmRuntimeSmokeTest.class.getResourceAsStream("/add.wasm")) {
            assertNotNull(wasm, "add.wasm is missing from the test resources");
            return Parser.parse(wasm);
        }
    }

    @Test
    void callsAnExportedFunction() throws IOException {
        Instance instance = Instance.builder(addModule()).build();
        ExportFunction add = instance.export("add");

        assertEquals(7, add.apply(3, 4)[0]);
        assertEquals(-1, add.apply(Integer.MAX_VALUE, Integer.MIN_VALUE)[0]);
    }

    @Test
    void wrapsIntegerOverflowInsteadOfTrapping() throws IOException {
        Instance instance = Instance.builder(addModule()).build();
        ExportFunction add = instance.export("add");

        assertEquals(Integer.MIN_VALUE, add.apply(Integer.MAX_VALUE, 1)[0]);
    }

    @Test
    void instancesDoNotShareState() throws IOException {
        WasmModule module = addModule();
        Instance first = Instance.builder(module).build();
        Instance second = Instance.builder(module).build();

        assertEquals(2, first.export("add").apply(1, 1)[0]);
        assertEquals(2, second.export("add").apply(1, 1)[0]);
    }

    @Test
    void rejectsSomethingThatIsNotAModule() {
        byte[] notWasm = "this is not a WebAssembly module".getBytes();

        assertThrows(RuntimeException.class, () -> Parser.parse(notWasm));
    }
}
