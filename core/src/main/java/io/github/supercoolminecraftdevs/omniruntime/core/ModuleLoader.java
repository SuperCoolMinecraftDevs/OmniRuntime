package io.github.supercoolminecraftdevs.omniruntime.core;

import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds modules in the modules directory and instantiates them.
 *
 * <p>One bad module does not stop the others. A file that will not parse, or whose manifest is
 * wrong, is reported as a failure and the rest of the directory is loaded, because a server owner
 * with ten modules should not lose all of them to one bad file.
 */
public final class ModuleLoader {

    public static final String MODULE_EXTENSION = ".omni";

    /**
     * Loads every module in the directory.
     *
     * <p>Returns what loaded and what did not, rather than throwing, so the caller can report all
     * the failures at once instead of one per restart.
     */
    public LoadOutcome loadAll(Path modulesDirectory) {
        List<LoadedModule> loaded = new ArrayList<>();
        List<LoadFailure> failures = new ArrayList<>();
        Map<ModuleIdentity, Path> claimed = new LinkedHashMap<>();

        for (Path file : moduleFiles(modulesDirectory, failures)) {
            try {
                LoadedModule module = load(file);
                Path existing = claimed.putIfAbsent(module.identity(), file);
                if (existing != null) {
                    failures.add(new LoadFailure(
                            file,
                            "Both this file and " + existing.getFileName() + " claim to be " + module.identity()
                                    + ". Identities have to be unique, so one of them has to change."));
                    continue;
                }
                loaded.add(module);
            } catch (ManifestException | GuestAbiException e) {
                failures.add(new LoadFailure(file, e.getMessage()));
            } catch (RuntimeException e) {
                failures.add(new LoadFailure(file, "The file is not a module this host can read: " + e.getMessage()));
            } catch (IOException e) {
                failures.add(new LoadFailure(file, "The file could not be read: " + e.getMessage()));
            }
        }

        return new LoadOutcome(List.copyOf(loaded), List.copyOf(failures));
    }

    /**
     * Loads one module.
     *
     * <p>The file name has to match the identity in the manifest. Grants and data directories are
     * keyed on identity, so a file whose name says one thing and whose manifest says another is a
     * mistake worth stopping rather than guessing at.
     */
    public LoadedModule load(Path file) throws IOException {
        WasmModule parsed = Parser.parse(Files.newInputStream(file));
        ModuleManifest manifest = ModuleManifest.readFrom(parsed);

        String expected = manifest.identity() + MODULE_EXTENSION;
        String actual = file.getFileName().toString();
        if (!expected.equals(actual)) {
            throw new ManifestException(
                    "The file is called " + actual + " but its manifest says it is " + manifest.identity()
                            + ". Rename it to " + expected + ".");
        }

        Instance instance = Instance.builder(parsed).build();
        return new LoadedModule(manifest, file, instance);
    }

    private List<Path> moduleFiles(Path modulesDirectory, List<LoadFailure> failures) {
        if (!Files.isDirectory(modulesDirectory)) {
            return List.of();
        }

        try (var entries = Files.list(modulesDirectory)) {
            return entries.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(MODULE_EXTENSION))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            failures.add(new LoadFailure(modulesDirectory, "The modules directory could not be read: " + e.getMessage()));
            return List.of();
        }
    }

    /** What loaded, and what did not. */
    public record LoadOutcome(List<LoadedModule> loaded, List<LoadFailure> failures) {}

    /** One module that could not be loaded, and the reason to put in front of a server owner. */
    public record LoadFailure(Path file, String reason) {}
}
