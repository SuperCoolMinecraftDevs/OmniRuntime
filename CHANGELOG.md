# Changelog

All notable changes to this project are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Version numbers follow the scheme described in
[CONTRIBUTING.md](CONTRIBUTING.md#versioning).

## [Unreleased]

### Added

- MIT license.
- Contributing guide covering branches, commit style, versioning and how
  decisions get recorded.
- Decision log in ADR format.
- Code owners, so review requests reach the right people automatically.
- Architecture document describing the core, the platform adapter layer, the
  module lifecycle and the limits we can and cannot enforce.
- Decision records covering the build tool, the WebAssembly runtime and the way
  server platforms are supported.
- Capability grant model: modules declare scoped requests in their manifest, the
  server owner approves them, and an update that asks for more waits for
  approval rather than inheriting it.
- Layout on disk. Modules live in `modules/`, the folder beside a module is both
  its data directory and its filesystem sandbox, and configuration is YAML.
  Removing a module archives its data instead of deleting it, and reinstalling
  the same module offers the archive back.

- Maven build with two modules: `omniruntime-core`, which has no server API on
  its classpath, and `omniruntime-paper`, which implements the bridge against
  Paper. A build rule fails the build if a server API ever reaches the core.
- Module identity parsing, with error messages that say what is wrong with a
  name rather than that it is invalid.
- The `ServerBridge` interface, and a Paper implementation of it behind a plugin
  entry point that reports the platform it found and where modules will live.
- Decision record on what the guest ABI targets. Guests are core modules, not
  components, because the runtime cannot load components at all.
- Guest interfaces are described in WIT and use the canonical ABI, so bindings
  are generated for each guest language rather than written by hand.
- Host side of that ABI: passing strings and byte lists into a module and
  reading them back, with the guest's own allocator and the release call that
  goes with it.
- Offsets and lengths coming from a module are checked against its memory before
  the host reads anything, and text is decoded strictly. A module that lies
  about either is stopped rather than accommodated.
- A Rust guest module used as a test fixture, built by cargo, so the host is
  tested against something a real toolchain produced.
- Module manifests. A module declares its identity, version, ABI version and the
  capabilities it wants in a small text format carried inside the wasm file, and
  the host reads it without running the module.
- Manifest problems are reported with the line number and what was expected,
  and an unknown key is an error rather than being ignored.
- Module loading. Files ending in `.omni` in the modules directory are read,
  checked against their manifest and instantiated at startup. A module that
  cannot be loaded is reported and skipped, and the rest still load.
- Module lifecycle, with start, stop and discard. Reloading is a discard
  followed by loading the file again, so no running module is ever mutated.
- A ledger of everything a module has been given, so stopping it takes all of it
  back. One registration failing to be taken back does not prevent the others,
  and a stopped module cannot register anything new.
- The Paper plugin now loads and starts modules on enable, and stops them on
  disable rather than leaving them running.
- The plugin jar now contains the core and the WebAssembly runtime, so it runs
  on a server without anything else being installed.

### Changed

- README now explains what the project is, what it is not yet, and which guest
  languages are planned.
- CI builds and tests the project instead of running a placeholder command.
