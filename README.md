```
   ____                  _ ____              __  _
  / __ \____ ___  ____  (_) __ \__  ______  / /_(_)___ ___  ___
 / / / / __ `__ \/ __ \/ / /_/ / / / / __ \/ __/ / __ `__ \/ _ \
/ /_/ / / / / / / / / / / _, _/ /_/ / / / / /_/ / / / / / /  __/
\____/_/ /_/ /_/_/ /_/_/_/ |_|\__,_/_/ /_/\__/_/_/ /_/ /_/\___/
```

[![CI](https://img.shields.io/github/actions/workflow/status/SuperCoolMinecraftDevs/OmniRuntime/ci.yml?branch=main&style=flat-square&label=CI)](https://github.com/SuperCoolMinecraftDevs/OmniRuntime/actions)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)
[![Java](https://img.shields.io/badge/java-25-e76f00?style=flat-square)](https://openjdk.org/)
[![Paper](https://img.shields.io/badge/paper-26.2-0288d1?style=flat-square)](https://papermc.io/)
[![Status](https://img.shields.io/badge/status-early%20development-yellow?style=flat-square)](#project-status)

Write Minecraft server plugins in Rust, Go or Python, compile them to
WebAssembly, and run them on a normal Paper server.

## What this is

OmniRuntime is a server plugin that hosts a WebAssembly runtime. You drop a
`.wasm` file into a folder, and OmniRuntime loads it, wires it up to the server
API, and runs it like any other plugin. The module never sees the JVM, and it
never sees the host machine. It only sees the functions we hand it.

The point is not that WebAssembly is faster than Java. It usually is not. The
point is what the sandbox lets us do that the JVM cannot:

- A module can be thrown away and replaced while the server is running, without
  the class loader leaks that make Java plugin reloading a bad idea.
- A module that corrupts its own memory corrupts only its own memory.
- A module reaches the filesystem, the network or the server API only where the
  host grants it, per module, by name.
- The language a plugin was written in stops being the server owner's problem.

## Project status

Early development. There is no release yet, nothing to download, and the guest
API is not designed, let alone stable. The repository currently holds the
documentation, the decision log and the build setup. Watch the repository if you
want to know when that changes.

## How it works

```
  guest plugin (.wasm)          OmniRuntime host              server
  --------------------          ----------------              ------
  Rust / Go / Python    ---->   WebAssembly runtime   ---->   Paper
  compiled to wasm              host function table           Spigot
                                capability grants             Folia
                                lifecycle and reload          Purpur
```

The host embeds [Chicory](https://chicory.dev), a WebAssembly runtime written in
pure Java. That choice keeps OmniRuntime a single portable jar with no native
libraries to ship per operating system. The reasoning, including what we give up
by not using a native runtime, is written down in the
[decision log](docs/adr).

Server specific code sits behind an adapter layer, so support for other server
software and for older Minecraft versions is a matter of writing an adapter
rather than forking the project.

## Guest languages

| Language | Support | Notes |
| --- | --- | --- |
| Rust | First class | Small modules, fast startup, official SDK |
| Go | First class | Built with TinyGo, official SDK |
| Python | Supported | Ships an embedded interpreter, so modules are large and start slower |

Anything else that compiles to WebAssembly will load, because the host only
cares about the module's exports. It simply will not have an SDK written for it.

## Documentation

- [Decision log](docs/adr)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## License

MIT. See [LICENSE](LICENSE).
