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

### Changed

- README now explains what the project is, what it is not yet, and which guest
  languages are planned.
