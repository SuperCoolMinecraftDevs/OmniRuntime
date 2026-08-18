package io.github.supercoolminecraftdevs.omniruntime.core;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;

/**
 * Calls functions on a guest module using the calling convention that WIT toolchains emit.
 *
 * <p>A function returning a string or a list does not return the value. It returns an offset to a
 * pair of numbers, the offset and length of the real value, and it expects the host to call the
 * matching {@code cabi_post_} function afterwards so the guest can release what it allocated.
 * Skipping that call leaks guest memory, so it happens in a finally block.
 */
public final class GuestCall {

    private static final String POST_RETURN_PREFIX = "cabi_post_";

    private final Instance instance;
    private final GuestMemory memory;

    public GuestCall(Instance instance) {
        this.instance = instance;
        this.memory = GuestMemory.of(instance);
    }

    public GuestMemory memory() {
        return memory;
    }

    public String callWithStringReturn(String name, String argument) {
        int argumentOffset = memory.writeString(argument);
        int argumentLength = argument.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;

        int returnArea = (int) export(name).apply(argumentOffset, argumentLength)[0];
        try {
            int valueOffset = memory.readInt(returnArea);
            int valueLength = memory.readInt(returnArea + Integer.BYTES);
            return memory.readString(valueOffset, valueLength);
        } finally {
            releaseReturn(name, returnArea);
        }
    }

    public int callWithBytesArgument(String name, byte[] argument) {
        int offset = memory.writeBytes(argument);
        return (int) export(name).apply(offset, argument.length)[0];
    }

    private void releaseReturn(String name, int returnArea) {
        ExportFunction postReturn;
        try {
            postReturn = instance.export(POST_RETURN_PREFIX + name);
        } catch (RuntimeException e) {
            return;
        }
        postReturn.apply(returnArea);
    }

    private ExportFunction export(String name) {
        try {
            return instance.export(name);
        } catch (RuntimeException e) {
            throw new GuestAbiException("The module exports no function called '" + name + "'.", e);
        }
    }
}
