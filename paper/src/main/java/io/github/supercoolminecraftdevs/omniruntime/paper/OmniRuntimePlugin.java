package io.github.supercoolminecraftdevs.omniruntime.paper;

import io.github.supercoolminecraftdevs.omniruntime.core.ServerBridge;
import java.io.IOException;
import java.nio.file.Files;
import org.bukkit.plugin.java.JavaPlugin;

public final class OmniRuntimePlugin extends JavaPlugin {

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
        bridge.logInfo("Modules directory is " + bridge.modulesDirectory());
        bridge.logInfo("No modules are loaded yet. Module loading is not implemented.");
    }
}
