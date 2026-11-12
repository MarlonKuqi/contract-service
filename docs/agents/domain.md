# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

Layout: **single-context**.

## Before exploring, read these

- **`docs/sujet.txt`**: the source of truth for the requirements.
- **`GLOSSARY.md`** at the repo root, if it exists.
- **`docs/ADR.md`**: all architecture decisions live in this single file (`ADR-001`, `ADR-002`, ...). Read the ADRs that touch the area you're about to work in.
- **`CLAUDE.md`**: the key domain rules and the architecture conventions.

If `GLOSSARY.md` doesn't exist, **proceed silently**. Don't flag its absence; don't suggest creating it upfront. The `/domain-modeling` skill creates it lazily when terms actually get resolved.

## File structure

```
/
├── CLAUDE.md
├── GLOSSARY.md          (created lazily)
├── docs/
│   ├── sujet.txt
│   ├── ADR.md           ← one file, one section per ADR
│   └── cli/             ← feature documentation
└── src/
```

## Recording a new ADR

Append a new section to `docs/ADR.md` with the next number (`ADR-026`, ...), following the format of the existing sections. Don't create a `docs/adr/` folder. When a decision changes, update the existing ADR section.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `GLOSSARY.md` and in the code (`Contract`, `Client`, `Person`, `Company`, `ContractType`, `ContractPeriod`, ...). Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-004 (active contract definition), but worth reopening because…_
