package io.github.supercoolminecraftdevs.omniruntime.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Everything a module has been given, so that all of it can be taken back.
 *
 * <p>Reloading a module is a discard followed by a fresh instantiation, and that is only safe if
 * the discard is complete. A module that keeps receiving events after it is gone is the failure
 * that makes reloading Java plugins a bad idea, and the sandbox does nothing to prevent it.
 *
 * <p>Revoking happens in reverse order, because later registrations may depend on earlier ones.
 * One that throws does not stop the rest: a half revoked module is worse than a noisy log.
 */
public final class RegistrationLedger {

    private final Deque<Registration> registrations = new ArrayDeque<>();
    private boolean closed;

    public synchronized void record(Registration registration) {
        Objects.requireNonNull(registration, "registration");

        if (closed) {
            throw new IllegalStateException(
                    "Cannot register '" + registration.description() + "': the module has already been torn down.");
        }
        registrations.push(registration);
    }

    /**
     * Revokes everything, closes the ledger, and returns what failed.
     *
     * <p>Failures are returned rather than thrown so the caller can report all of them. An empty
     * list means the module left nothing behind.
     */
    public synchronized List<RevokeFailure> revokeAll() {
        closed = true;

        List<RevokeFailure> failures = new ArrayList<>();
        while (!registrations.isEmpty()) {
            Registration registration = registrations.pop();
            try {
                registration.revoke();
            } catch (RuntimeException e) {
                failures.add(new RevokeFailure(registration.description(), e));
            }
        }
        return List.copyOf(failures);
    }

    public synchronized int size() {
        return registrations.size();
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** A registration that could not be taken back, and why. */
    public record RevokeFailure(String description, RuntimeException cause) {}
}
