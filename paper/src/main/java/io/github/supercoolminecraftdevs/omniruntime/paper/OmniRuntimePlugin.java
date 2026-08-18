package io.github.supercoolminecraftdevs.omniruntime.paper;

import io.github.supercoolminecraftdevs.omniruntime.core.LoadedModule;
import io.github.supercoolminecraftdevs.omniruntime.core.ModuleLoader;
import io.github.supercoolminecraftdevs.omniruntime.core.RegistrationLedger;
import io.github.supercoolminecraftdevs.omniruntime.core.ServerBridge;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class OmniRuntimePlugin extends JavaPlugin {

    private final ModuleLoader loader = new ModuleLoader();
    private final List<LoadedModule> modules = new ArrayList<>();

    private ServerBridge bridge;

    @Override
    public void onEnable() {
        bridge = new PaperServerBridge(this);

        try {
            Files.createDirectories(bridge.modulesDirectory());
            Files.createDirectories(bridge.hostDirectory());
        } catch (IOException e) {
            bridge.logError("Could not create the modules directory at " + bridge.modulesDirectory()
                    + ". OmniRuntime cannot load anything until that path is writable.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        bridge.logInfo("Running on " + bridge.platformName() + " " + bridge.platformVersion());

        ModuleLoader.LoadOutcome outcome = loader.loadAll(bridge.modulesDirectory());

        for (ModuleLoader.LoadFailure failure : outcome.failures()) {
            bridge.logWarning("Skipped " + failure.file().getFileName() + ": " + failure.reason());
        }

        for (LoadedModule module : outcome.loaded()) {
            try {
                module.start();
                modules.add(module);
                bridge.logInfo("Started " + module.identity() + " " + module.manifest().version());
            } catch (RuntimeException e) {
                bridge.logError("Could not start " + module.identity() + ", so it is not running.", e);
                module.discard();
            }
        }

        if (outcome.loaded().isEmpty() && outcome.failures().isEmpty()) {
            bridge.logInfo("No modules found in " + bridge.modulesDirectory() + ".");
        } else {
            bridge.logInfo("Started " + modules.size() + " of " + outcome.loaded().size() + " modules.");
        }
    }

    @Override
    public void onDisable() {
        for (LoadedModule module : modules) {
            for (RegistrationLedger.RevokeFailure failure : module.discard()) {
                bridge.logWarning("While stopping " + module.identity() + ", could not take back "
                        + failure.description() + ": " + failure.cause().getMessage());
            }
        }
        modules.clear();
    }
}
