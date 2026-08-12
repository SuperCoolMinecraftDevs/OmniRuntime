# Architecture

This describes how OmniRuntime is put together and why the pieces sit where they
do. It is kept current, so it describes the system as we intend it to be today.
For the reasoning behind individual choices, and for the options we rejected,
read the [decision log](adr).

Nothing here is built yet. Treat it as the plan we are working against.

## Overview

```
  +---------------------------------------------------------------+
  |  guest modules                                                 |
  |  plugin.wasm   plugin.wasm   plugin.wasm                       |
  +---------------------------------------------------------------+
             |  exports called by host, imports provided by host
  +---------------------------------------------------------------+
  |  omniruntime-core                                              |
  |                                                                |
  |  module loader     host function table     capability grants   |
  |  lifecycle         registration ledger     resource limits     |
  +---------------------------------------------------------------+
             |  ServerBridge, implemented once per platform
  +---------------------------------------------------------------+
  |  omniruntime-paper   omniruntime-folia   omniruntime-spigot    |
  +---------------------------------------------------------------+
             |  server API
  +---------------------------------------------------------------+
  |  the server                                                    |
  +---------------------------------------------------------------+
```

## The core knows nothing about Minecraft

`omniruntime-core` has no server API on its classpath. It cannot import a Bukkit
type even by accident, because the dependency is not there. It deals in loading
modules, calling them, tracking what they registered, enforcing limits, and
handing their requests to a `ServerBridge`.

This is a constraint we accept deliberately. It makes the core testable without
a running server, which is the difference between a test suite that runs in two
seconds on every push and one that nobody runs. It also means adding a server
platform is an afternoon of writing an adapter rather than a fork.

The cost is indirection. Every server concept the guest can touch has to be
expressed in the bridge interface first, in terms the core can hold without
knowing what they mean. When that starts feeling like ceremony, the fix is to
question whether the guest should have that capability at all, not to leak a
server type into the core.

## Platform adapters

An adapter implements `ServerBridge` against one server API and is selected at
startup by probing for classes and methods, not by parsing a version string.

Version strings are not a reliable signal any more. The API artifact for the
1.21 line is `1.21.8-R0.1-SNAPSHOT`, and for the current line it is
`26.2.build.112-stable`. Those cannot be compared by the same rules, and the
next change of scheme will break whatever parser we write today. Asking whether
a method exists is boring, it is correct, and it keeps working.

Paper is the first adapter. Spigot, Purpur and Folia follow. Folia is the one
that shapes the design rather than just consuming it, because its regionised
threading means there is no single main thread to schedule onto.

## Java baseline

The core targets Java 17 bytecode. Adapters target whatever their platform
requires, which is Java 25 for the current Paper line and lower for the older
lines we want to keep supporting. Chicory is published as Java 11 bytecode, so
the runtime does not drag the baseline up on its own.

## Module lifecycle

```
  discovered -> parsed -> instantiated -> started -> running
                                                       |
                                          stopped <----+
                                             |
                                          discarded
```

Reloading is not a state. It is a discard followed by a fresh instantiation from
the file on disk. There is no path that mutates a running instance in place,
because that is where Java plugin reloading goes wrong and there is no reason to
repeat it.

What makes the discard safe is the registration ledger. Every effect a module
has on the outside world, such as an event subscription, a scheduled task, a
command or an open resource, is recorded by the host at the moment it is
granted. Teardown walks the ledger and reverses it. A module never holds a
handle the host does not know about, so a stale module cannot keep receiving
events after it is gone.

This is the hard part of the project. The sandbox gives us memory isolation for
free, but nothing about WebAssembly cleans up a scheduled task we registered on
the module's behalf.

## The guest ABI

Not designed yet, and it is the decision most worth taking slowly, because it is
the one we cannot revise without breaking every plugin written against it.

The constraints are already fixed by WebAssembly itself:

- Only numbers cross the boundary. Anything larger travels as an offset and a
  length into the module's linear memory.
- The host cannot allocate inside the guest. Modules export an allocator, and
  the host uses it to hand data in.
- Calls are synchronous in both directions. Anything that needs to wait has to
  be modelled as a call now and a callback later.
- The guest holds no pointers to host objects. It gets opaque handles, and the
  host resolves them, which is also where it checks that the handle is still
  valid and still belongs to that module.

## Threading

Server state belongs to the thread that owns it. A guest call that touches the
world is dispatched onto that thread by the bridge, and a guest call that does
pure computation runs on a worker.

The bridge exposes this as "run this where the thread that owns this location
can see it" rather than "run this on the main thread", because on Folia those
are different questions and the second one has no answer. Writing the interface
the other way round would work everywhere except the platform that most needs
it.

Bulk world editing follows from that. Copy the region out, hand the copy to the
module on a worker, take the result back, apply it on the owning thread in one
pass. The speed comes from doing the writes in bulk on the right thread, not
from the language the module was written in.

## Capabilities

Everything is denied by default. A module reaches the filesystem, the network or
any part of the server API only where the server owner has granted it, per
module, by name, in configuration. The grant model is
[record 0005](adr/0005-capabilities-are-granted-by-the-server-owner.md).

A module declares its requests in its manifest, scoped and marked required or
optional. Until they are answered it sits pending and does not run. A missing
required capability stops the module loading; a missing optional one does not,
and the host reports the granted set to the guest so it can degrade instead of
failing.

That means WASI is not switched on wholesale. Where we implement WASI calls, we
implement them against the grants, so a module that was given one directory gets
one directory and a module that was given nothing gets an error it can handle.

## Layout on disk

```
server/
  modules/
    myplugin.omni
    myplugin/
      config.yml
  plugins/
    OmniRuntime/
      config.yml
      grants.yml
```

The folder beside a module is its data directory and the root of its filesystem
sandbox, so where its files live and what it can reach are one answer. Host
owned files, including the grants, sit outside `modules/` where no module can
write to them. Configuration is YAML, parsed by the adapter using the parser the
server already ships rather than by the core. The details are in
[record 0006](adr/0006-module-layout-and-configuration.md).

## Module identity

A module is identified by a namespaced name, lowercase, in the form
`namespace.name`. It is the directory name, the key in the grants file, and the
name anything installing a module by name would use. It is fixed for the life of
the module, because changing it orphans both the data directory and the grants.

## Resource limits

A memory ceiling per instance and a wall clock deadline per call are both
straightforward and will be there from the start.

Instruction level limits are not. The runtime has no metering of its own, so
counting instructions means supplying our own machine implementation, and that
only works while a module is interpreted. A module compiled for speed cannot be
metered that way. The two are mutually exclusive for now and the choice is the
server owner's, per module.

## Before the ABI can be called stable

In order, and without dates attached:

1. A host that loads a module, calls it, and unloads it without leaking.
2. The registration ledger, with teardown proven by tests that assert nothing
   survives a discard.
3. Enough of the bridge to write a plugin somebody would actually run.
4. Handle validation and capability checks on every host function, not most.
5. Resource limits that hold under a module written specifically to break them.
6. A guest SDK for Rust, then Go, then Python.
7. A second and third platform adapter, which is what proves the abstraction is
   real rather than a Paper wrapper with extra steps.

## Open questions

- How a module declares which events it wants without the host having to
  instantiate it first.
- Whether modules can call each other, and if so through what.
- What happens to a module that traps during teardown.
- Whether state survives a reload, and if so, who owns the format.
- Whether modules are ever managed from outside the server console. A web
  dashboard, and installing by name from a registry, are both plausible and
  neither is decided. What they need from us is already true: a manifest that
  can be read without executing the module, and a stable module identity. If
  either is ever built, the security question it raises is not the interface
  but the authenticated channel behind it, which is a larger surface than the
  runtime itself.
