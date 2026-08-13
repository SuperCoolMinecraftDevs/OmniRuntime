package io.github.supercoolminecraftdevs.omniruntime.core;

import java.util.Objects;

/**
 * The name a module is known by, in the form {@code namespace.name}.
 *
 * <p>The same string is used as the module's directory name, as its key in the grants file, and
 * as the name anything installing a module by name would use. Because grants and stored data are
 * keyed on it, an identity is fixed for the life of a module: renaming one orphans both.
 */
public final class ModuleIdentity {

    private static final int MAX_PART_LENGTH = 64;

    private final String namespace;
    private final String name;

    private ModuleIdentity(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public static ModuleIdentity parse(String identity) {
        Objects.requireNonNull(identity, "identity");

        int separator = identity.indexOf('.');
        if (separator < 0) {
            throw new IllegalArgumentException(
                    "Module identity '" + identity + "' has no namespace. Expected namespace.name, such as elchi.greeter.");
        }
        if (identity.indexOf('.', separator + 1) >= 0) {
            throw new IllegalArgumentException(
                    "Module identity '" + identity + "' has more than one dot. Expected namespace.name, such as elchi.greeter.");
        }

        String namespace = identity.substring(0, separator);
        String name = identity.substring(separator + 1);

        validatePart(namespace, "namespace", identity);
        validatePart(name, "name", identity);

        return new ModuleIdentity(namespace, name);
    }

    private static void validatePart(String part, String label, String identity) {
        if (part.isEmpty()) {
            throw new IllegalArgumentException(
                    "Module identity '" + identity + "' has an empty " + label + ".");
        }
        if (part.length() > MAX_PART_LENGTH) {
            throw new IllegalArgumentException(
                    "The " + label + " in module identity '" + identity + "' is longer than " + MAX_PART_LENGTH + " characters.");
        }
        if (!isLetter(part.charAt(0))) {
            throw new IllegalArgumentException(
                    "The " + label + " in module identity '" + identity + "' must start with a lowercase letter.");
        }
        if (part.charAt(part.length() - 1) == '-') {
            throw new IllegalArgumentException(
                    "The " + label + " in module identity '" + identity + "' must not end with a hyphen.");
        }

        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (isLetter(c) || isDigit(c) || c == '-') {
                continue;
            }
            if (c >= 'A' && c <= 'Z') {
                throw new IllegalArgumentException(
                        "Module identity '" + identity + "' contains an uppercase letter. Identities are lowercase, so that one module cannot claim two directories.");
            }
            throw new IllegalArgumentException(
                    "Module identity '" + identity + "' contains '" + c + "'. Only lowercase letters, digits and hyphens are allowed.");
        }
    }

    private static boolean isLetter(char c) {
        return c >= 'a' && c <= 'z';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public String namespace() {
        return namespace;
    }

    public String name() {
        return name;
    }

    public String directoryName() {
        return toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ModuleIdentity)) {
            return false;
        }
        ModuleIdentity that = (ModuleIdentity) other;
        return namespace.equals(that.namespace) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name);
    }

    @Override
    public String toString() {
        return namespace + "." + name;
    }
}
