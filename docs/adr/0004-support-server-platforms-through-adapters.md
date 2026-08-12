# 4. Support server platforms through adapters

Date: 2026-08-12

## Status

Accepted

## Context

The immediate target is Paper on the current Minecraft line. The stated goal is
wider: older Minecraft versions, and other server software including Spigot,
Purpur and Folia. A plugin written for OmniRuntime should not care which of
those it is running on.

Two problems make the naive approach fail.

The first is that server APIs differ in ways that are not cosmetic. Folia
replaces the single main thread with regionised threading, so code written
against "schedule this on the main thread" has nothing to bind to. Any
abstraction that assumes one main thread will be correct on three platforms and
wrong on the one that most needs the abstraction.

The second is that version strings are no longer parseable by a single rule. The
API artifact for the 1.21 line is `1.21.8-R0.1-SNAPSHOT`. For the current line
it is `26.2.build.112-stable`. A comparison written for one is meaningless
against the other, and the next change of scheme will break whatever we write
now. Meanwhile the thing we actually want to know is never really the version.
It is whether a given method exists.

Version specific code also has a habit of spreading. Left unchecked it appears
as scattered conditionals throughout ordinary logic, and after two or three
versions nobody can say which branches are still reachable.

## Decision

The core module depends on no server API. It talks to the server through a
`ServerBridge` interface it defines itself.

Each supported platform is a separate Maven module implementing that interface
against one server API. Adapters are selected at startup by probing for the
classes and methods they need, never by parsing a version string. Where a
platform genuinely cannot provide a capability, the bridge reports it as absent
and the host tells the module, rather than failing at the call site.

The bridge is written in terms of the most constrained platform rather than the
most convenient one. Scheduling is expressed as running work where the thread
owning a given location can see it, which has a correct answer on Folia and on
everything else, instead of as running work on the main thread, which does not.

## Consequences

Adding a platform is writing an adapter. It does not touch the core, and it
cannot break the other platforms, because it cannot reach them.

The core is testable without a server. A fake bridge is enough to exercise
loading, lifecycle, teardown and limits, which keeps the majority of the test
suite fast enough that people will run it.

Capability probing is slower to write than a version check and reads as more
work up front. It pays for itself the first time a server fork moves a method,
and it keeps working across version scheme changes that would break a parser.

The bridge is a bottleneck by design. Every server capability a guest can reach
has to be modelled there first, which slows down adding features and is the
point. When that friction becomes annoying, the question to ask is whether the
guest should have that capability, not whether the core can import one server
type just this once.

Designing for Folia from the start costs us clarity on the platforms where a
single main thread does exist. Server owners on Paper will read scheduling
documentation that is more careful than their situation requires. That is a
better outcome than an abstraction we have to break later.

Each supported platform needs its own CI coverage, and the matrix grows with
every one we add. Support that is claimed and not tested is worse than support
that is not claimed, so a platform is supported only once it is in the matrix.
