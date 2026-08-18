package io.github.supercoolminecraftdevs.omniruntime.core;

import java.util.Objects;

/**
 * One capability a module asks for, and whether it can run without it.
 *
 * <p>A request names what it wants and the scope it wants it over, such as reading one directory
 * rather than the filesystem. A request without a scope asks for the capability in general, which
 * the host may refuse on that basis alone.
 *
 * @param name what is being asked for, such as {@code fs:read}
 * @param scope what it applies to, or empty for an unscoped request
 * @param required whether the module refuses to run without it
 */
public record CapabilityRequest(String name, String scope, boolean required) {

    public CapabilityRequest {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(scope, "scope");

        if (name.isBlank()) {
            throw new IllegalArgumentException("A capability request has no name.");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == ':' || c == '-';
            if (!allowed) {
                throw new IllegalArgumentException(
                        "Capability name '" + name + "' contains '" + c + "'. Names are lowercase, and may contain digits, colons and hyphens.");
            }
        }
    }

    public boolean isScoped() {
        return !scope.isEmpty();
    }

    @Override
    public String toString() {
        return isScoped() ? name + " " + scope : name;
    }
}
