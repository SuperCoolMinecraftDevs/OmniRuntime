package io.github.supercoolminecraftdevs.omniruntime.paper;

import io.github.supercoolminecraftdevs.omniruntime.core.ServerBridge;
import java.nio.file.Path;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

final class PaperServerBridge implements ServerBridge {

    private final JavaPlugin plugin;
    private final Path modulesDirectory;

    PaperServerBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modulesDirectory = plugin.getServer().getWorldContainer().toPath().resolve("modules");
    }

    @Override
    public String platformName() {
        return plugin.getServer().getName();
    }

    @Override
    public String platformVersion() {
        return plugin.getServer().getVersion();
    }

    @Override
    public Path modulesDirectory() {
        return modulesDirectory;
    }

    @Override
    public Path hostDirectory() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public void logInfo(String message) {
        plugin.getLogger().info(message);
    }

    @Override
    public void logWarning(String message) {
        plugin.getLogger().warning(message);
    }

    @Override
    public void logError(String message, Throwable cause) {
        plugin.getLogger().log(Level.SEVERE, message, cause);
    }
}
