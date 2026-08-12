# 2. Build with Maven

Date: 2026-08-12

## Status

Accepted

## Context

The Minecraft plugin ecosystem has largely settled on Gradle, and most of the
tutorials, templates and example projects a new contributor will find are
written for it. Choosing anything else means choosing to be the odd one out.

The historical reason to prefer Gradle for Paper work specifically was
`paperweight-userdev`, which handled remapping between obfuscated and readable
server internals. That reason is gone. Paper has run on Mojang mappings since
1.20.5 and dropped reobfuscation, so a plugin that touches server internals no
longer needs a remapping step at build time, and the plugin that provided it is
Gradle only.

What we do need is a build that describes a module graph clearly. The project is
several modules from the start: a core with no server dependency, one module per
server platform, guest SDKs, and test fixtures. That graph is the thing most
likely to rot, because it is what stops server types leaking into the core.

Chicory publishes a bill of materials, which pins its module versions
consistently under either tool.

The team writes Maven by preference. This is not nothing. Build files get edited
under pressure, usually while something is broken, and familiarity is worth more
then than elegance.

## Decision

Build with Maven, as a multi module project. The root is a pom that declares
versions and plugin configuration and contains no code of its own.

Guest language toolchains stay out of the Maven build. Rust, Go and Python
modules are built by their own toolchains and driven from CI. Maven consumes the
compiled `.wasm` files as test fixtures, and does not try to invoke `cargo`.

## Consequences

A contributor arriving from the plugin ecosystem will find our build unfamiliar
and most Paper examples will not copy across directly. The contributing guide
has to carry its weight here.

Module boundaries become hard to violate by accident. A dependency that is not
declared in a module's pom is not on its classpath, so the rule that the core
cannot see the server API is enforced by the build rather than by review.

Maven is verbose. The root pom will be long before it is interesting, and that
is the cost of having every version in one visible place.

Anything that is genuinely dynamic, such as a matrix build across server
platforms, moves into CI rather than into the build tool. That is a reasonable
split and it keeps the build reproducible on a laptop.

The guest side is not covered by `mvn verify`. Running the full test suite
locally requires the Rust, Go and Python toolchains as well, and CI has to make
sure the fixtures are rebuilt rather than trusted from a previous run.
