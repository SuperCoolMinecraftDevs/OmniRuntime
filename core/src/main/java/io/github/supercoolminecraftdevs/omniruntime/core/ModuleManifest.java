package io.github.supercoolminecraftdevs.omniruntime.core;

import com.dylibso.chicory.wasm.WasmModule;
import com.dylibso.chicory.wasm.types.CustomSection;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What a module says about itself, read from a custom section of the wasm file.
 *
 * <p>It is read without executing anything, because deciding whether to grant a capability by
 * running the code that wants it is not a decision. The same applies to anything that installs a
 * module: it can read this before the module ever runs.
 *
 * <p>The format is a strict subset of YAML, so the file stays readable and a future move to a real
 * YAML parser would not invalidate manifests written today. Unknown keys are rejected rather than
 * ignored, so a typo is reported instead of quietly doing nothing.
 */
public final class ModuleManifest {

    public static final String SECTION_NAME = "omnirt.manifest";
    public static final int SUPPORTED_ABI_VERSION = 1;

    private final ModuleIdentity identity;
    private final String version;
    private final int abiVersion;
    private final List<CapabilityRequest> capabilities;

    private ModuleManifest(ModuleIdentity identity, String version, int abiVersion, List<CapabilityRequest> capabilities) {
        this.identity = identity;
        this.version = version;
        this.abiVersion = abiVersion;
        this.capabilities = List.copyOf(capabilities);
    }

    public static ModuleManifest readFrom(WasmModule module) {
        CustomSection section = module.customSection(SECTION_NAME);
        if (section == null) {
            throw new ManifestException(
                    "The module carries no " + SECTION_NAME + " section, so there is no way to tell what it is or what it wants.");
        }
        if (!(section instanceof UnknownCustomSection unknown)) {
            throw new ManifestException("The " + SECTION_NAME + " section could not be read as raw bytes.");
        }

        return parse(decode(unknown.bytes()));
    }

    private static String decode(byte[] raw) {
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new ManifestException("The " + SECTION_NAME + " section is not valid UTF-8.", e);
        }
    }

    public static ModuleManifest parse(String text) {
        String identity = null;
        String version = null;
        String abi = null;
        List<CapabilityRequest> capabilities = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].strip();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int colon = line.indexOf(':');
            if (colon < 0) {
                throw new ManifestException("Line " + lineNumber + " of the manifest has no colon: " + line);
            }

            String key = line.substring(0, colon).strip();
            String value = line.substring(colon + 1).strip();
            if (value.isEmpty()) {
                throw new ManifestException("Line " + lineNumber + " of the manifest sets '" + key + "' to nothing.");
            }

            switch (key) {
                case "identity", "version", "abi" -> {
                    if (!seen.add(key)) {
                        throw new ManifestException("The manifest sets '" + key + "' more than once, at line " + lineNumber + ".");
                    }
                    switch (key) {
                        case "identity" -> identity = value;
                        case "version" -> version = value;
                        default -> abi = value;
                    }
                }
                case "requires" -> capabilities.add(capability(value, true, lineNumber));
                case "optional" -> capabilities.add(capability(value, false, lineNumber));
                default -> throw new ManifestException(
                        "Line " + lineNumber + " of the manifest sets '" + key + "', which is not a manifest key. Expected identity, version, abi, requires or optional.");
            }
        }

        return build(identity, version, abi, capabilities);
    }

    private static ModuleManifest build(String identity, String version, String abi, List<CapabilityRequest> capabilities) {
        if (identity == null) {
            throw new ManifestException("The manifest does not say what the module is. Add an 'identity' line.");
        }
        if (version == null) {
            throw new ManifestException("The manifest does not say which version this is. Add a 'version' line.");
        }
        if (abi == null) {
            throw new ManifestException("The manifest does not say which ABI version it was built against. Add an 'abi' line.");
        }

        ModuleIdentity parsedIdentity;
        try {
            parsedIdentity = ModuleIdentity.parse(identity);
        } catch (IllegalArgumentException e) {
            throw new ManifestException(e.getMessage(), e);
        }

        if (version.indexOf(' ') >= 0) {
            throw new ManifestException("Version '" + version + "' contains a space.");
        }

        int parsedAbi;
        try {
            parsedAbi = Integer.parseInt(abi);
        } catch (NumberFormatException e) {
            throw new ManifestException("ABI version '" + abi + "' is not a whole number.", e);
        }
        if (parsedAbi != SUPPORTED_ABI_VERSION) {
            throw new ManifestException(
                    "The module was built against ABI version " + parsedAbi + ", and this host speaks version " + SUPPORTED_ABI_VERSION + ".");
        }

        return new ModuleManifest(parsedIdentity, version, parsedAbi, capabilities);
    }

    private static CapabilityRequest capability(String value, boolean required, int lineNumber) {
        int space = value.indexOf(' ');
        String name = space < 0 ? value : value.substring(0, space);
        String scope = space < 0 ? "" : value.substring(space + 1).strip();

        try {
            return new CapabilityRequest(name, scope, required);
        } catch (IllegalArgumentException e) {
            throw new ManifestException("Line " + lineNumber + " of the manifest: " + e.getMessage(), e);
        }
    }

    public ModuleIdentity identity() {
        return identity;
    }

    public String version() {
        return version;
    }

    public int abiVersion() {
        return abiVersion;
    }

    public List<CapabilityRequest> capabilities() {
        return capabilities;
    }

    public List<CapabilityRequest> requiredCapabilities() {
        return capabilities.stream().filter(CapabilityRequest::required).toList();
    }

    public List<CapabilityRequest> optionalCapabilities() {
        return capabilities.stream().filter(c -> !c.required()).toList();
    }
}
