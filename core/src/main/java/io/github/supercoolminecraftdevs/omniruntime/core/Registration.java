package io.github.supercoolminecraftdevs.omniruntime.core;

/**
 * Something a module has been given that has to be taken back when it stops.
 *
 * <p>An event subscription, a scheduled task, a registered command, an open file. The sandbox
 * stops a module reaching memory it does not own, but nothing about WebAssembly cancels a task the
 * host scheduled on a module's behalf. That is what these are for.
 */
public interface Registration {

    /** What this is, in words a server owner would understand, used when reporting a failed revoke. */
    String description();

    /** Takes it back. Called once, and expected to tolerate being called on a half built module. */
    void revoke();
}
