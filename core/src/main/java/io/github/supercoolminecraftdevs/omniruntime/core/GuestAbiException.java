package io.github.supercoolminecraftdevs.omniruntime.core;

/**
 * Thrown when a guest module breaks the contract the host relies on, such as returning an offset
 * outside its own memory or bytes that are not valid UTF-8 where a string was expected.
 *
 * <p>This is a fault in the module, not in the host, and the module is the thing that should be
 * stopped when it happens.
 */
public class GuestAbiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GuestAbiException(String message) {
        super(message);
    }

    public GuestAbiException(String message, Throwable cause) {
        super(message, cause);
    }
}
