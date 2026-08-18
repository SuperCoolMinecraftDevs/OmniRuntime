package io.github.supercoolminecraftdevs.omniruntime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleLoaderTest {

    private final ModuleLoader loader = new ModuleLoader();

    private static Registration noop(String description, List<String> log) {
        return new Registration() {
            @Override
            public String description() {
                return description;
            }

            @Override
            public void revoke() {
                log.add(description);
            }
        };
    }

    @Test
    void loadsAModuleFromTheDirectory(@TempDir Path modules) throws IOException {
        TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter"));

        ModuleLoader.LoadOutcome outcome = loader.loadAll(modules);

        assertEquals(1, outcome.loaded().size());
        assertTrue(outcome.failures().isEmpty());
        assertEquals("elchi.greeter", outcome.loaded().get(0).identity().toString());
        assertEquals(ModuleState.LOADED, outcome.loaded().get(0).state());
    }

    @Test
    void ignoresFilesThatAreNotModules(@TempDir Path modules) throws IOException {
        TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter"));
        Files.writeString(modules.resolve("notes.txt"), "nothing to see here");
        Files.createDirectory(modules.resolve("elchi.greeter"));

        ModuleLoader.LoadOutcome outcome = loader.loadAll(modules);

        assertEquals(1, outcome.loaded().size());
        assertTrue(outcome.failures().isEmpty());
    }

    @Test
    void returnsNothingWhenTheDirectoryDoesNotExist(@TempDir Path parent) {
        ModuleLoader.LoadOutcome outcome = loader.loadAll(parent.resolve("missing"));

        assertTrue(outcome.loaded().isEmpty());
        assertTrue(outcome.failures().isEmpty());
    }

    @Test
    void oneBadModuleDoesNotStopTheOthers(@TempDir Path modules) throws IOException {
        TestModules.writeModule(modules, "elchi.good.omni", TestModules.manifestFor("elchi.good"));
        Files.write(modules.resolve("elchi.broken.omni"), "this is not a wasm file".getBytes());
        TestModules.writeModule(modules, "script.alsogood.omni", TestModules.manifestFor("script.alsogood"));

        ModuleLoader.LoadOutcome outcome = loader.loadAll(modules);

        assertEquals(2, outcome.loaded().size());
        assertEquals(1, outcome.failures().size());
        assertEquals("elchi.broken.omni", outcome.failures().get(0).file().getFileName().toString());
    }

    @Test
    void reportsAModuleWithNoManifest(@TempDir Path modules) throws IOException {
        Files.write(modules.resolve("elchi.bare.omni"), TestModules.base());

        ModuleLoader.LoadOutcome outcome = loader.loadAll(modules);

        assertEquals(1, outcome.failures().size());
        assertTrue(outcome.failures().get(0).reason().contains(ModuleManifest.SECTION_NAME));
    }

    @Test
    void refusesAFileWhoseNameDisagreesWithItsManifest(@TempDir Path modules) throws IOException {
        TestModules.writeModule(modules, "something-else.omni", TestModules.manifestFor("elchi.greeter"));

        ModuleLoader.LoadOutcome outcome = loader.loadAll(modules);

        assertEquals(1, outcome.failures().size());
        assertTrue(outcome.failures().get(0).reason().contains("elchi.greeter.omni"), outcome.failures().get(0).reason());
    }

    @Test
    void refusesTwoFilesClaimingTheSameIdentity(@TempDir Path modules) throws IOException {
        TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter"));
        Files.write(
                modules.resolve("elchi.greeter.omni.disabled"),
                TestModules.withManifest(TestModules.manifestFor("elchi.greeter")));
        Files.copy(modules.resolve("elchi.greeter.omni"), modules.resolve("aaa.omni"));

        ModuleLoader.LoadOutcome outcome = loader.loadAll(modules);

        assertEquals(1, outcome.loaded().size());
        assertEquals(1, outcome.failures().size());
    }

    @Test
    void startsAndStopsAModule(@TempDir Path modules) throws IOException {
        LoadedModule module = loader.load(
                TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter")));

        module.start();
        assertEquals(ModuleState.RUNNING, module.state());

        assertTrue(module.stop().isEmpty());
        assertEquals(ModuleState.STOPPED, module.state());
    }

    @Test
    void takesBackEverythingWhenStopped(@TempDir Path modules) throws IOException {
        LoadedModule module = loader.load(
                TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter")));
        List<String> revoked = new ArrayList<>();
        module.ledger().record(noop("event subscription", revoked));
        module.ledger().record(noop("scheduled task", revoked));

        module.start();
        module.stop();

        assertEquals(List.of("scheduled task", "event subscription"), revoked);
        assertEquals(0, module.ledger().size());
        assertTrue(module.ledger().isClosed());
    }

    @Test
    void cannotBeCalledOnceDiscarded(@TempDir Path modules) throws IOException {
        LoadedModule module = loader.load(
                TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter")));

        module.start();
        module.discard();

        assertEquals(ModuleState.DISCARDED, module.state());
        assertThrows(IllegalStateException.class, module::instance);
        assertThrows(IllegalStateException.class, module::start);
    }

    @Test
    void cannotRegisterAnythingAfterBeingDiscarded(@TempDir Path modules) throws IOException {
        LoadedModule module = loader.load(
                TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter")));

        module.discard();

        assertThrows(
                IllegalStateException.class,
                () -> module.ledger().record(noop("late subscription", new ArrayList<>())));
    }

    @Test
    void stoppingTwiceIsHarmless(@TempDir Path modules) throws IOException {
        LoadedModule module = loader.load(
                TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter")));
        List<String> revoked = new ArrayList<>();
        module.ledger().record(noop("event subscription", revoked));

        module.start();
        module.stop();
        module.stop();

        assertEquals(1, revoked.size());
    }

    @Test
    void reloadingGivesACompletelyFreshInstance(@TempDir Path modules) throws IOException {
        Path file = TestModules.writeModule(modules, "elchi.greeter.omni", TestModules.manifestFor("elchi.greeter"));
        LoadedModule first = loader.load(file);
        first.start();
        first.discard();

        LoadedModule second = loader.load(file);

        assertNotSame(first, second);
        assertEquals(ModuleState.LOADED, second.state());
        assertEquals(0, second.ledger().size());
        assertEquals(first.identity(), second.identity());
    }
}
