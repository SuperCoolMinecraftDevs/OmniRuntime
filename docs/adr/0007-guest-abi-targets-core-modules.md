# 7. The guest ABI targets core modules, not components

Date: 2026-08-13

## Status

Accepted

## Context

There are two ways a WebAssembly host can define the interface its guests speak.

The component model is the standardised answer. Interfaces are written in WIT,
`wit-bindgen` generates guest side bindings from that one definition, and the
host works in terms of strings, lists and records rather than integers and
memory offsets. For a project whose selling point is that plugins can be written
in any language, having one interface definition produce Rust, Go and Python
bindings is close to the whole SDK problem solved by a tool. WASI 0.2 is built
on it, and it is where the ecosystem is going.

The alternative is to define the ABI ourselves against plain core modules,
passing numbers and offsets, and to write the guest side bindings once per
language by hand.

The choice depends on whether the runtime we embed can load components at all,
so it was tested rather than assumed. Chicory 1.7.5, the version we depend on:

```
add.wasm            magic 00 61 73 6d  version 01 00  layer 00 00   parsed
add.component.wasm  magic 00 61 73 6d  version 0d 00  layer 01 00   rejected
```

The rejection is at the header, reporting an unknown binary version and
expecting `[1, 0, 0, 0]`. This is not a partial or degraded implementation. The
parser does not accept the format. Chicory publishes no component or WIT
artifact and its documentation does not claim support.

A second test matters as much. A component produced by `wasm-tools component
new` was unbundled back into the core module it wraps, and that core module
instantiated and ran on Chicory without complaint. The wrapper is the problem,
not the contents.

That leaves three options.

Switch to a runtime with component support. That means a native runtime, which
record 0003 rejected for reasons that have not changed.

Write the ABI ourselves, entirely. Full control, no ceiling on what we can
express, and every guest language costs a hand written binding library.

Take the middle path. Keep WIT as the interface description, let `wit-bindgen`
generate the guest bindings as it already does, and have the host implement the
canonical ABI against the core module rather than consuming the component
wrapper. Generated bindings for three languages, at the cost of implementing the
lifting and lowering rules ourselves.

The middle path is appealing and we cannot honestly cost it yet. The canonical
ABI is a substantial specification, and the work is not uniform: numbers and
byte lists are simple, strings and records are manageable, and variants,
resources and post return semantics are not. Committing to it before writing any
of it would be guessing.

## Decision

The guest ABI is defined against core modules. OmniRuntime does not require
guests to be components, and does not treat the component model as a
prerequisite for anything.

Whether that ABI follows the canonical ABI or one of our own design is left open
deliberately. It is decided by a spike that implements lifting and lowering for
the subset we actually need, which is numbers, strings and byte lists, and
reports what that cost. That record supersedes nothing here; it answers a
question this one leaves open on purpose.

Until then, guest bindings are written by hand, and Rust is the only guest
language we commit to. Adding a second language before the ABI is settled would
mean writing bindings twice for an interface that is going to change.

## Consequences

The ABI is ours to design and ours to get wrong. There is no specification doing
the thinking for us, and the mistakes will be ours to live with, which is an
argument for keeping the first version small enough to be obviously correct.

Guests stay ordinary core modules, which means every language that targets
WebAssembly at all can produce one. Component support in a toolchain is not
required, and the toolchains that only emit core modules are not excluded.

Go and Python move behind Rust. The README already describes them as planned
rather than available, and this is why.

If Chicory adds component support later, nothing here blocks us from using it.
Accepting components becomes an additional way to load a module, not a
replacement for the ABI, because by then plugins will exist that were written
against what we shipped.

The strongest reason for the middle path stands unchanged: three hand written
binding libraries is a maintenance burden that grows with every ABI change, and
generated bindings do not. That is what the spike is for.
