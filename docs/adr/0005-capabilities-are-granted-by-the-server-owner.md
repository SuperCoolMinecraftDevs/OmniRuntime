# 5. Capabilities are granted by the server owner

Date: 2026-08-13

## Status

Accepted

## Context

A Java plugin runs with the full authority of the server process. It can read
any file the user can read, open any socket, and take the environment variables
with it. Server owners install them anyway, because there is no alternative on
offer and reading the source of every plugin is not a thing anyone does.

WebAssembly changes what is possible here. A module starts with no ability to
affect anything outside its own memory, and every capability it has is one the
host chose to hand over. That is the difference worth building the project
around, and it only means something if the default is nothing.

The difficulty is that the default being nothing has to survive contact with a
server owner who just wants the plugin to work. If the experience of installing
a module is a wall of prompts, or an error that says permission denied with no
indication of what to do about it, people will find the setting that turns the
whole system off and we will have built an inconvenience rather than a sandbox.

Three constraints shape what is possible:

A server console is not an interactive session. There is nobody to show a
dialogue to at the moment a module wants something, and a module that blocks
waiting for an answer blocks a server thread. Whatever we build has to work
without a human present at the moment of the request.

Capability requests have to be readable without running the module. Deciding
whether to grant something by executing the code that wants it is not a
decision, and a dashboard or an installer needs the same information before
anything runs.

Auto-update is the sharp edge. A plugin that was granted one directory in
version 1.2 and asks for network access in 1.3 must not receive it because an
update happened to be available. Anyone who can push a release would otherwise
own every server running that plugin.

## Decision

A module declares the capabilities it wants in its manifest. Each request is
scoped, and each is marked either required or optional.

Scoped means the grant names the thing, not the category. A filesystem grant
names a directory. A network grant names hosts. A capability that means
everything of its kind is not a capability, it is an off switch with extra
steps.

Nothing is granted implicitly, including on first install. A module whose
requests have not been answered sits in a pending state and does not run. The
console says which capabilities it asked for, why, and the exact line needed to
grant them.

Grants live in a host-owned file, editable directly or through a console
command. They record the module identity, the capability, its scope, and the
module version the grant was approved against.

Refusal has two behaviours, which is what makes the required and optional
marking meaningful:

- A missing required capability means the module does not load. The log says
  which one, not a stack trace.
- A missing optional capability means the module loads without it. The host
  reports the granted set to the guest, which is expected to degrade rather than
  fail.

The same required and optional marking covers dependencies on other modules, so
there is one mechanism to learn rather than two.

If an update requests more than the installed version was granted, it does not
install. It waits for a human to approve the widened scope, and the previous
version keeps running in the meantime.

## Consequences

Installing a module takes one deliberate step more than dropping a jar in a
folder. That step is the product. It has to be one line, and the message that
prompts it has to be good enough that nobody goes looking for the master switch.

Guests have to handle absent capabilities. That means a host function to query
what was actually granted, available before the module starts doing work, and
guest SDKs that make checking it natural rather than an afterthought.

The grants file is a security boundary. It cannot live anywhere a module can
write, or a module with filesystem access is one path traversal away from
granting itself the rest.

Error messages are load bearing rather than cosmetic. A denial that does not
explain itself will be read as the runtime being broken. This is a documentation
and log quality commitment, and it is the part most likely to be skimped on
under time pressure.

Approving a capability is not approving intent. A module granted a directory can
do anything it likes within that directory, and we are not in the business of
deciding whether what it does there is reasonable. What we can promise is that
the boundary is real, visible before installation, and unchanged without
consent.

Recording the version a grant was approved against means we can tell an update
that widens scope from one that does not. It also means grants carry history,
which is what a management interface would need to show a meaningful diff.
