# 6. Module layout and configuration

Date: 2026-08-13

## Status

Accepted

## Context

Server owners already know how this is supposed to work. A file goes in a
folder, the server starts, a config file appears next to it, and they edit it.
Any layout that asks them to learn something new needs to earn that, and ours
does not have a reason to.

Two things do make our situation different from a jar in `plugins/`.

The first is that a module has a filesystem sandbox, and something has to decide
what is inside it. Every module needs somewhere to keep its own files, and every
module needs a boundary. If those are two separate concepts, a server owner has
to hold both, and the answer to "where do my files go" stops matching the answer
to "what can this thing touch".

The second is that the core cannot depend on a configuration library. It has no
server API, and the project keeps its dependency count deliberately low, so
reading YAML would mean adding a parser for the sake of one file format. That
would be an odd dependency to take on given every Bukkit derived server already
ships one.

There is also the question of where defaults come from. A plugin that ships a
default config traditionally carries it inside the jar and writes it out on
first run. A bare WebAssembly module has no resource directory to carry it in,
which is one of the arguments people reach for when proposing a container
format.

## Decision

Modules and their data sit together, under `modules/` in the server directory:

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

The directory next to a module is both its data directory and the root of its
filesystem sandbox. The answer to where its files live and the answer to what it
can reach are the same answer.

Host owned files stay under the host's own plugin directory, outside `modules/`,
for the reason given in record 0005. Nothing a module can write to may influence
what a module is allowed to do.

The directory name comes from the module identity in the manifest, which is
namespaced and restricted to lowercase letters, digits and hyphens, with a dot
between namespace and name. That keeps it safe as a path component, usable as a
key in the grants file, and unique enough to survive two authors picking the
same plugin name. Identity is fixed for the life of a module, because renaming
one orphans both its data directory and its grants.

Administrator facing configuration is YAML. It is what server owners already
read and edit, and being different here would cost familiarity and buy nothing.

The core does not parse it. The core defines a configuration tree, and the
platform adapter supplies it using the parser the server already bundles. YAML
support with no dependency added.

Default configuration ships inside the module, in a custom section of the wasm
file. The host writes it out on first load if no config exists, and never
overwrites one that does. Custom sections are part of the format and are ignored
by anything that does not know about them, so the module stays a valid wasm file
that ordinary tooling can read.

What a module writes inside its own directory beyond that is its own business.
The host does not care about the format of a module's data.

## Consequences

A server owner learns one layout: a module, a folder next to it, and that folder
is the extent of what it can see. There is no separate sandbox configuration to
understand.

Support for a platform that does not bundle a YAML parser means that adapter has
to supply one. That cost falls on the adapter that needs it rather than on the
core, which is the right place for it.

YAML's rough edges are now ours to handle. Tabs break parsing, and the word `no`
becomes a boolean when the author meant a string. We validate and report clearly
rather than pretending that does not happen, and the error tells the person the
line to fix.

Embedding defaults in a custom section means the packaging tool has to write
that section and the loader has to read it. Both are small, and it removes the
strongest argument for a container format, which keeps modules ordinary wasm
files for longer.

Identity being fixed and namespaced is a constraint on module authors from day
one, imposed before there is any registry to enforce uniqueness. That is
deliberate. It is the piece that a management interface or an install-by-name
flow would need, and adding it later would mean rewriting grants and moving data
directories on servers that are already running.

Data directories accumulate. A module removed from `modules/` leaves its folder
and its grants behind, which is what you want the first time somebody deletes a
file by accident and what you do not want after a year of experimenting.
Cleaning that up needs a deliberate command rather than automatic deletion.
