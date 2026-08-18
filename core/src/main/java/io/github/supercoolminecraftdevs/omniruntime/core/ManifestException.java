package io.github.supercoolminecraftdevs.omniruntime.core;

/**
 * Thrown when a module's manifest is missing, unreadable, or says something the host cannot act
 * on. The message is written for the person holding the file, since they are usually the one who
 * has to fix it.
 */
public class ManifestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ManifestException(String message) {
        super(message);
    }

    public ManifestException(String message, Throwable cause) {
        super(message, cause);
    }
}
