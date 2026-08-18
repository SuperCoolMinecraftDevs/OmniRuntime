package io.github.supercoolminecraftdevs.omniruntime.core;

import com.dylibso.chicory.runtime.Instance;
import java.nio.file.Path;
import java.util.List;

/**
 * A module that has been instantiated, and the state that belongs to it.
 *
 * <p>Everything the module is given while it runs is recorded in its ledger, so stopping it can
 * take all of it back. Once discarded, an instance is never reused: reloading means loading the
 * file again from disk.
 */
public final class LoadedModule {

    private static final String START_EXPORT = "start";
    private static final String STOP_EXPORT = "stop";

    private final ModuleManifest manifest;
    private final Path source;
    private final Instance instance;
    private final RegistrationLedger ledger = new RegistrationLedger();

    private ModuleState state = ModuleState.LOADED;

    LoadedModule(ModuleManifest manifest, Path source, Instance instance) {
        this.manifest = manifest;
        this.source = source;
        this.instance = instance;
    }

    public ModuleIdentity identity() {
        return manifest.identity();
    }

    public ModuleManifest manifest() {
        return manifest;
    }

    public Path source() {
        return source;
    }

    public synchronized ModuleState state() {
        return state;
    }

    public RegistrationLedger ledger() {
        return ledger;
    }

    public synchronized Instance instance() {
        requireUsable();
        return instance;
    }

    public synchronized void start() {
        if (state != ModuleState.LOADED) {
            throw new IllegalStateException(identity() + " cannot start from state " + state + ".");
        }

        callIfPresent(START_EXPORT);
        state = ModuleState.RUNNING;
    }

    /**
     * Stops the module and takes back everything it registered.
     *
     * <p>The guest's own stop function is called first and its failure does not prevent the
     * revoking that follows, because a module that fails while shutting down is exactly the one
     * whose registrations must not survive.
     */
    public synchronized List<RegistrationLedger.RevokeFailure> stop() {
        if (state == ModuleState.STOPPED || state == ModuleState.DISCARDED) {
            return List.of();
        }

        RuntimeException guestFailure = null;
        if (state == ModuleState.RUNNING) {
            try {
                callIfPresent(STOP_EXPORT);
            } catch (RuntimeException e) {
                guestFailure = e;
            }
        }

        List<RegistrationLedger.RevokeFailure> failures = ledger.revokeAll();
        state = ModuleState.STOPPED;

        if (guestFailure != null) {
            List<RegistrationLedger.RevokeFailure> all = new java.util.ArrayList<>(failures);
            all.add(new RegistrationLedger.RevokeFailure("the module's own stop function", guestFailure));
            return List.copyOf(all);
        }
        return failures;
    }

    public synchronized List<RegistrationLedger.RevokeFailure> discard() {
        List<RegistrationLedger.RevokeFailure> failures = stop();
        state = ModuleState.DISCARDED;
        return failures;
    }

    private void callIfPresent(String export) {
        try {
            instance.export(export);
        } catch (RuntimeException notExported) {
            return;
        }
        instance.export(export).apply();
    }

    private void requireUsable() {
        if (state == ModuleState.DISCARDED) {
            throw new IllegalStateException(identity() + " has been discarded and cannot be called.");
        }
        if (state == ModuleState.STOPPED) {
            throw new IllegalStateException(identity() + " has been stopped and cannot be called.");
        }
    }
}
