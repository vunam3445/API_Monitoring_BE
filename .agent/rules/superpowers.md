---
description: Superpowers workflow rules - apply when building features, debugging, or planning any development task
alwaysApply: true
---

# Superpowers Skills Integration

> **Core rule:** Before any implementation/debugging/planning action, check if a skill applies and read its SKILL.md. Skip only for trivial factual questions with zero implementation.

This workspace uses the **Superpowers** skills library located in `superpowers/skills/`.
All skills are symlinked into `.agent/skills/` and are automatically available.

## Core Rule: Check Skills Before Acting

**Before any response or action**, check if a relevant skill applies. If there's even a 1% chance a skill applies, read it via `view_file` on its `SKILL.md` and follow it exactly.

**Exception (per CLAUDE.md §1):** Skip the skill check for trivial tasks — simple factual questions, one-line answers, or requests that clearly involve zero implementation (e.g., "what does X mean?", "how do I spell Y?"). If in doubt, check anyway.

## Available Skills

### Development Workflow

| Skill | When to Use |
|---|---|
| `brainstorming` | Before ANY creative work — adding features, building components, modifying behavior |
| `writing-plans` | After design is approved — break work into 2-5 min tasks |
| `executing-plans` | When running a plan in batches with human checkpoints |
| `subagent-driven-development` | When dispatching subagents per task with two-stage review |
| `test-driven-development` | During ALL implementation — RED → GREEN → REFACTOR |
| `systematic-debugging` | When debugging any issue |
| `verification-before-completion` | Before declaring any fix or task is done |
| `requesting-code-review` | Before submitting code for review |
| `receiving-code-review` | When responding to review feedback |
| `using-git-worktrees` | When starting work on a new isolated branch |
| `finishing-a-development-branch` | When tasks complete — merge / PR / discard |
| `dispatching-parallel-agents` | When running concurrent subagent workflows |
| `writing-skills` | When creating new skills |
| `using-superpowers` | When starting any conversation — find and use skills |

### Technical Roles

| Skill | When to Use |
|---|---|
| ⚠️ `frontend-developer` | (removed upstream) Web UI, component architecture, React/Vue/Svelte/Vanilla |
| ⚠️ `mobile-developer` | (removed upstream) Mobile apps — React Native, Flutter, iOS, Android |
| ⚠️ `frontend-design` | (removed upstream) Web components, pages, artifacts — high design quality |
| ⚠️ `mobile-uiux-promax` | (removed upstream) Mobile app UI for iOS, Android, React Native, Flutter, SwiftUI, Compose |

### Product & Design

| Skill | When to Use |
|---|---|
| ⚠️ `product-manager` | (removed upstream) Product requirements, feature prioritization, roadmap |
| ⚠️ `ux-designer` | (removed upstream) UI design, wireframes, user research, IA |
| ⚠️ `copywriter` | (removed upstream) Landing copy, app descriptions, email sequences |

### Infrastructure & Integration

| Skill | When to Use |
|---|---|
| ⚠️ `subscription-billing` | (removed upstream) Stripe, IAP, trials, dunning flows |
| ⚠️ `i18n-localization` | (removed upstream) Internationalization, translations, localized ASO |

## How to Read a Skill (Antigravity)

Use `view_file` on the skill's `SKILL.md`:
```
.agent/skills/<skill-name>/SKILL.md
```
Example: `.agent/skills/brainstorming/SKILL.md`

## Instruction Priority

1. **User's explicit instructions** — highest priority
2. **Superpowers skills** — override default behavior
3. **Default system behavior** — lowest priority

## Key Principles

- **YAGNI**: Don't build what isn't needed yet
- **TDD always**: Write failing tests first, then code
- **Systematic over ad-hoc**: Follow the skill process, don't guess
- **Evidence over claims**: Verify before declaring success

