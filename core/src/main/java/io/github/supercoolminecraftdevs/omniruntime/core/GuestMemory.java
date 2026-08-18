package io.github.supercoolminecraftdevs.omniruntime.core;

import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;

/**
 * Moves values across the boundary between the host and a guest module.
 *
 * <p>Only numbers can cross that boundary, so anything larger travels as an offset and a length
 * into the guest's own memory. Space for values going in is obtained from the guest's allocator,
 * because the host cannot allocate inside a guest.
 *
 * <p>Offsets arriving from a guest are untrusted. Every read is checked against the current size
 * of guest memory before it happens, so a module cannot persuade the host to read outside it.
 */
public final class GuestMemory {

    private static final String ALLOCATOR_EXPORT = "cabi_realloc";
    private static final int ALIGNMENT = 1;

    private final Memory memory;
    private final ExportFunction allocator;

    private GuestMemory(Memory memory, ExportFunction allocator) {
        this.memory = memory;
        this.allocator = allocator;
    }

    public static GuestMemory of(Instance instance) {
        Memory memory = instance.memory();
        if (memory == null) {
            throw new GuestAbiException("The module exports no memory, so nothing can be passed to it.");
        }

        ExportFunction allocator;
        try {
            allocator = instance.export(ALLOCATOR_EXPORT);
        } catch (RuntimeException e) {
            throw new GuestAbiException(
                    "The module exports no " + ALLOCATOR_EXPORT + " function, so the host has no way to pass it data.", e);
        }

        return new GuestMemory(memory, allocator);
    }

    public byte[] readBytes(int offset, int length) {
        checkRange(offset, length);
        return memory.readBytes(offset, length);
    }

    public String readString(int offset, int length) {
        byte[] raw = readBytes(offset, length);
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new GuestAbiException(
                    "The module returned " + length + " bytes at offset " + offset + " that are not valid UTF-8.", e);
        }
    }

    public int writeBytes(byte[] value) {
        int offset = allocate(value.length);
        memory.write(offset, value);
        return offset;
    }

    public int writeString(String value) {
        return writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public int readInt(int offset) {
        checkRange(offset, Integer.BYTES);
        return memory.readInt(offset);
    }

    public int size() {
        return memory.pages() * Memory.PAGE_SIZE;
    }

    private int allocate(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Cannot allocate a negative number of bytes.");
        }
        if (length == 0) {
            return 0;
        }

        long offset = allocator.apply(0, 0, ALIGNMENT, length)[0];
        if (offset <= 0 || offset > Integer.MAX_VALUE) {
            throw new GuestAbiException("The module's allocator refused a request for " + length + " bytes.");
        }

        checkRange((int) offset, length);
        return (int) offset;
    }

    private void checkRange(int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new GuestAbiException("The module gave a negative offset or length: offset " + offset + ", length " + length + ".");
        }

        long end = (long) offset + length;
        int size = size();
        if (end > size) {
            throw new GuestAbiException(
                    "The module asked the host to read bytes " + offset + " to " + end + ", past the end of its memory at " + size + ".");
        }
    }
}
