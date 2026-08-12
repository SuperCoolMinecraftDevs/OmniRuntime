# 3. Embed Chicory as the WebAssembly runtime

Date: 2026-08-12

## Status

Accepted

## Context

The project is a WebAssembly host, so the runtime we embed is the single choice
with the widest reach. It decides what we can promise about isolation, what we
have to ship, and how much of the sandbox we have to build ourselves.

The people who will run this are not deploying to a fleet they control. They are
running a jar on a box someone else administers, on whatever operating system
and architecture the host provider picked, often through a control panel that
only knows how to upload a file into a folder. Anything that requires the right
native library to be present for the right platform will fail for a share of
them, and it will fail in a way they cannot diagnose.

The options considered:

**Wasmtime through JNI.** The fastest, the most complete, and the one with real
resource metering already built. It ships native code, which means either a jar
per platform or a fat jar carrying every platform's binaries, and any platform
we did not anticipate is simply unsupported. It also puts a native library in
the same process as a server that people already blame for every crash.

**GraalWasm.** Strong runtime, but it pulls in a large dependency and is at its
best under GraalVM, which server owners do not run.

**Chicory.** A WebAssembly runtime written in pure Java with no native code. It
runs wherever the JVM runs, so there is no platform matrix at all. It is slower
than a native runtime. It compiles to Java 11 bytecode, so it does not raise our
own baseline. It has no gas metering yet.

**Writing our own.** Not seriously considered. WebAssembly validation and
execution done wrong is a security hole rather than a bug, and the spec is large.
This is the kind of dependency worth having.

Speed matters less here than it first appears. The workload that made this
project interesting, bulk world editing, is dominated by how the writes are
applied to the server rather than by how fast the guest computes, and the
computation happens on a worker thread where a slower runtime costs latency
rather than server ticks.

Chicory also lets the execution engine be swapped at instantiation time, through
a machine factory supplied on the instance builder. That turns the speed
question into a per module setting instead of a project wide commitment.

## Decision

Embed Chicory, behind a small internal interface of our own so the runtime is
not spread through the codebase.

Two execution modes, chosen per module by the server owner:

- Interpreted, the default. Slower, and the only mode in which we can enforce
  instruction level limits.
- Compiled, opt in. Chicory translates the module to JVM bytecode so HotSpot can
  optimise it, which is much faster and cannot be metered by instruction count.

We do not ship native code, and we do not add a runtime that would require it.

## Consequences

The distributable stays one jar that runs anywhere the server runs. No platform
matrix, no native library loading, no support requests we cannot reproduce.

We are slower than a native runtime. For guest logic that computes for a long
time in a tight loop, meaningfully so. The architecture already puts that work
on a worker thread, which is what keeps it from being a tick time problem, and
the compiled mode exists for people who need the speed and do not need metering.

Instruction level limits are our work, not the library's. Chicory has no gas
metering, so enforcing a CPU budget means supplying our own machine
implementation wrapping the interpreter. Until that exists, the protection
against a runaway module is a wall clock deadline, which is coarser. This must
not be described as a runtime feature we get for free, because it is not one.

Metering and speed cannot both be on for the same module. That is a real
limitation and it belongs in the documentation next to the setting, not in a
footnote.

The compiled mode brings ASM in as a transitive dependency. The interpreter does
not, so a build that only wants the interpreter carries nothing extra.

If a native runtime ever becomes worth the packaging pain, the internal
interface is where it plugs in, and it stays an optional artifact rather than
the default.
