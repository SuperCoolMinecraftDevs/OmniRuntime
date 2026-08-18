package io.github.supercoolminecraftdevs.omniruntime.core;

/**
 * Where a module is in its life.
 *
 * <pre>
 *   LOADED -> RUNNING -> STOPPED -> DISCARDED
 * </pre>
 *
 * <p>Reloading is not a state. It is a discard followed by loading the file again, so there is no
 * path that mutates a running module in place.
 */
public enum ModuleState {

    /** Parsed and instantiated, but not started. */
    LOADED,

    /** Started, and able to receive calls. */
    RUNNING,

    /** Stopped, with everything it registered taken back. */
    STOPPED,

    /** Finished with. A discarded module is never started again. */
    DISCARDED
}
