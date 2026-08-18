# 9. The manifest is a small text format carried inside the module

Date: 2026-08-13

## Status

Accepted

## Context

Record 0005 requires that a module's capability requests be readable without
executing it. Record 0006 puts the manifest in a custom section of the wasm
file, so the module stays an ordinary wasm file that standard tooling can read.
Neither said what the manifest looks like.

Three things read it, and they do not share an environment. The core reads it
when loading a module, and the core has no server API and no configuration
library. A packaging tool writes it. Anything that installs a module by name
would read it before the file ever reaches a server.

That rules out the trick from record 0006, where the platform adapter supplies
the YAML parser the server already bundles. The manifest has to be readable by
the core alone.

The options were a real format with a dependency, a real format with a parser we
write, or a small format of our own.

JSON has no parser in the standard library. Writing one is a weekend of edge
cases, escapes and number handling, for a file that will usually be six lines.

Full YAML is worse. It is a large specification with genuinely surprising
corners, and a hand written partial implementation of it would be a liability
rather than a convenience.

TOML is pleasant and would still mean either a dependency or a parser.

A format of our own is a few dozen lines to parse. The risk is that it becomes a
thing to learn, and that it grows badly if we later need structure it cannot
express.

## Decision

The manifest is a line-oriented text format, carried UTF-8 encoded in a custom
section named `omnirt.manifest`.

```
identity: elchi.greeter
version: 2.1.0
abi: 1
requires: fs:read modules/elchi.greeter
optional: net:https api.example.com
```

Blank lines and lines starting with `#` are ignored. Every other line is a key,
a colon and a value. `identity`, `version` and `abi` appear exactly once each
and are all required. `requires` and `optional` may appear any number of times.

The syntax is deliberately a strict subset of YAML. Nothing here would parse
differently under a real YAML parser, so manifests written today stay valid if
we ever adopt one.

Unknown keys are an error rather than being ignored, so a typo is reported
instead of silently doing nothing. A misspelled `requires` that was ignored
would mean a module runs with fewer permissions than its author intended, and
finds out at the worst moment.

The `abi` field gates the whole format. A host that does not speak the declared
ABI version refuses the module and says which version it does speak.

## Consequences

The core parses the manifest with no dependency, and so will the packaging tool
and anything else that needs to.

The format has no nesting, so anything structured has to be flattened into a
line. That is fine for what the manifest holds today, and it is the constraint
most likely to be uncomfortable later. Widening it is a decision that gets
recorded.

Rejecting unknown keys means an older host refuses a manifest written for a
newer one. That is intended, and it is why the ABI version exists: a module
declaring `abi: 1` must use only what version 1 defines. Adding a key means
either a new ABI version or accepting that older hosts refuse it.

Being a strict YAML subset gives us an exit. If the format outgrows itself, a
real parser can be adopted without invalidating what people have already
written.

The manifest is not authenticated. Anyone holding the file can edit it,
including the identity. Since grants are keyed on identity, a module that claims
an identity a server has already approved would inherit those grants. Nothing in
this record addresses that, and nothing else does yet either. The answer is
probably signing, or grants recording a fingerprint of the module they were
approved for, and it needs its own record before anyone can install a module
from somewhere they did not build it themselves.
