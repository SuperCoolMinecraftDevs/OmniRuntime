package io.github.supercoolminecraftdevs.omniruntime.core;

import java.nio.file.Path;

/**
 * Everything the core is allowed to know about the server it is running on.
 *
 * <p>The core has no server API on its classpath, so this is the whole of the boundary. Each
 * supported platform implements it once, and a fake implementation is enough to exercise the core
 * in tests without starting a server.
 *
 * <p>Implementations are chosen at startup by checking for the classes and methods they need,
 * never by parsing a version string.
 */
public interface ServerBridge {

    /** Human readable name of the server software, used in logs and in support requests. */
    String platformName();

    /**
     * Version of the server software, as it reports itself.
     *
     * <p>This is for display only. Version strings across the platforms we support do not share a
     * comparable format, so no decision may be taken by parsing this.
     */
    String platformVersion();

    /** Directory holding modules and their data directories. */
    Path modulesDirectory();

    /** Directory for the host's own files, which no module may reach. */
    Path hostDirectory();

    void logInfo(String message);

    void logWarning(String message);

    void logError(String message, Throwable cause);
}
