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

### Changed

- README now explains what the project is, what it is not yet, and which guest
  languages are planned.
- CI builds and tests the project instead of running a placeholder command.
