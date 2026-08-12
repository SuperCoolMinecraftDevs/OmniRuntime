# Decision log

Why the project looks the way it does. Each record is a decision we took, the
situation that forced it, and what it cost us.

Records are immutable. When a decision changes we add a new record and mark the
old one superseded, so the reasoning stays readable even after the conclusion
stops being true. Read them as history rather than as documentation: for how
the system works today, see the architecture document.

| Number | Title | Status |
| --- | --- | --- |
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-build-with-maven.md) | Build with Maven | Accepted |

## Adding a record

Copy the sections from record 0001, take the next free number, and name the file
`NNNN-short-title-in-kebab-case.md`. Write the context before you write the
decision, including the options you rejected and why. Then add a row to the
table above in the same pull request.
