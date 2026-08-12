# 1. Record architecture decisions

Date: 2026-08-12

## Status

Accepted

## Context

OmniRuntime sits on top of three moving targets at once: the Minecraft server
API, a WebAssembly runtime, and whatever toolchains our guest languages happen
to use this year. Decisions taken against that background go stale, and when
they do, the reasoning behind them matters more than the decision itself. A
choice that looks wrong today is often a choice that was right against
constraints nobody wrote down.

We also expect people to join the project who were not in the conversation where
a choice was made. Without a record, they have two options: guess at the
reasoning from the code, or ask someone who was there. The first produces
changes that quietly undo decisions, and the second does not scale.

Comments and commit messages are the wrong place for this. Comments describe the
code as it is, not the alternatives that were rejected, and commit messages are
found only by people who already know what to search for.

## Decision

Decisions that shape the project are recorded as Architecture Decision Records
in `docs/adr`, in the format described by Michael Nygard.

One file per decision, numbered in sequence, named
`NNNN-short-title-in-kebab-case.md`, containing the sections used here: title,
date, status, context, decision, consequences.

A record is immutable once accepted. When a decision changes, we write a new
record explaining why and mark the old one superseded, because the old reasoning
stays useful even after the conclusion stops being true.

Records are for decisions with consequences we would have to live with:
dependencies we embed, the shape of the guest ABI, which server platforms and
Minecraft versions we support, how modules are isolated from each other. Adding
a helper method is not a decision record.

## Consequences

Writing a record is friction, and that friction is the point. Anything worth
recording is worth ten minutes of writing down why, and a decision that cannot
survive being written down plainly is worth reconsidering.

The log is read as history, not as documentation. It is an ordered list of what
we believed and when, so a record can be accurate and out of date at the same
time. Anything that has to describe the system as it is now belongs in
`docs/architecture.md` instead, which is edited freely.

New contributors get a single place that explains why the project looks the way
it does, and review has something concrete to point at when a change conflicts
with an earlier choice.
