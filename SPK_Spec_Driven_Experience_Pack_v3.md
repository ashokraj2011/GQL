# SPEC — Spec-Driven Experience Pack

**Status:** validated draft for implementation review  
**Version:** v3 — reconciled and completed  
**Validated baseline:** `main@6cbf14e`  
**Original input baseline:** `main@4cdcdcc` — implementation must rebase before work begins  
**Namespace:** `SPK`  
**Normative language:** MUST / SHOULD / MAY  
**Clause convention:** `docs/CLAUSE-DRIVEN-SPECIFICATIONS.md`  
**Supersedes:** SPK v1 and v2

## 1. Decision and purpose

SingularityFlow will provide a spec-kit-simple authoring journey over its unchanged governed kernel. [SPK:REQ-001]

The governing product sentence is: **keep the kernel's sophistication; make the experience feel as small as Spec Kit's.** [SPK:CON-001]

The experience will expose five verbs, familiar portable artifacts, reviewer-owned specification quality, explicit clarification consequences, requirement-altitude convergence, and one pinned constitution. [SPK:REQ-002]

Aliases MUST orchestrate registered kernel operations and MUST NOT reimplement lifecycle rules, compute competing state, or bypass an existing transition. [SPK:CON-002]

## 2. Ratified boundaries

Approval authority, transitions, hashing, publication, recovery, containment, identity, and dependency invalidation MUST remain executable kernel responsibilities rather than prompt-file instructions. [SPK:CON-003]

Phase order and transition eligibility MUST remain kernel state and MUST NOT be inferred or executed from a Markdown process description. [SPK:CON-004]

Generations, exact bundle approvals, configuration pins, and evidence lineage MUST remain authoritative even when the fast path hides their mechanics. [SPK:CON-005]

Repository world models MUST remain an independent grounding layer and MUST NOT be replaced by specification artifacts. [SPK:CON-006]

Task maps MUST remain advisory and MUST NOT authorize, block, approve, or advance a lifecycle transition. [SPK:CON-007]

The `PKG` packaging model, `ADP` host-adapter contract, and fold-precedence explanation are outside this specification. [SPK:CON-008]

Existing advanced lifecycle commands MUST remain available; the five verbs are a product vocabulary and orchestration layer, not a removal of expert controls. [SPK:CON-009]

This specification closes all three v2 decisions: deterministic convergence is the default with optional assisted candidates, constitution samples ship outside active configuration, and fast-path verbs become the primary `nextsteps` vocabulary for the new profile. [SPK:CON-010]

## 3. Terms and authority

A **fast-path verb** is a resumable intent router that moves one Story toward a named lifecycle milestone using registered kernel operations. [SPK:REQ-003]

A **checkpoint** is the next kernel-recognized boundary that requires model generation, explicit consent, human review, approval, external completion, or recovery before the verb can continue. [SPK:REQ-004]

A **milestone** is reached only when the workflow state proves completion of the verb's configured lifecycle responsibility; returning successfully from a command is not itself milestone completion. [SPK:CON-011]

A **deterministic fact** is a reproducible observation derived without a model from pinned configuration, approved artifacts, clause indexes, claim maps, reconciliation, source hashes, or recorded evidence. [SPK:REQ-005]

An **assisted candidate** is a model-proposed quality or convergence concern bound to its prompt, model, input hashes, and deterministic facts; it has no lifecycle authority. [SPK:REQ-006]

A **governed finding** is an immutable convergence record created by the kernel from a deterministic fact or a human-adjudicated assisted candidate. [SPK:REQ-007]

## 4. Five-verb fast path — P1

### 4.1 Public commands

Flow MUST ship `sflow specify`, `sflow plan`, `sflow implement`, `sflow verify`, and `sflow converge`. [SPK:REQ-010]

Flow MUST ship the corresponding canonical skills `/sflow-specify`, `/sflow-plan`, `/sflow-implement`, `/sflow-verify`, and `/sflow-converge`. [SPK:REQ-011]

The fast path MUST use the ratified `sflow` and `/sflow-*` namespace; `/sf-*` names MUST NOT be presented as the canonical fast-path vocabulary. [SPK:CON-012]

Existing `/sf-*` compatibility aliases MAY remain outside the fast-path presentation until their separate deprecation policy is decided. [SPK:CON-013]

### 4.2 Milestone mapping

`specify` MUST route intake, clarification, specification generation, publication, submission, and review continuation toward an approved specification milestone without crossing an unfulfilled human boundary. [SPK:REQ-012]

`plan` MUST route design, implementation specification, planning-set generation, publication, submission, and review continuation toward an approved planning milestone without crossing an unfulfilled human boundary. [SPK:REQ-013]

`implement` MUST route approved inputs, required world-model views, governed implementation, source publication, claims, and implementation review toward an implementation milestone. [SPK:REQ-014]

`verify` MUST route configured checks, test evidence, visual assurance when configured, verification publication, conformance, and review toward an approved verification milestone. [SPK:REQ-015]

`converge` MUST route the kernel-owned convergence protocol in Section 8 and MUST NOT implement an autonomous repeat loop. [SPK:REQ-016]

### 4.3 Router behavior

Each verb MUST resolve the active subject, work type, current phase, current generation, pending publication, required inputs, model policy, approval state, and next legal operation before proposing an action. [SPK:REQ-017]

Each invocation MUST execute at most the registered operations that are legal before the next checkpoint and MUST stop before model consent, human selection, approval, external completion, or an unsafe recovery decision. [SPK:CON-014]

The CLI form MUST return a native agent handoff when authoring is required and MUST NOT fabricate generated content inside the deterministic dispatcher. [SPK:CON-015]

The skill form MAY perform governed authoring with the resolved phase agent, approved inputs, template, constitution, and world-model views, then MUST publish through the same kernel operation used by the advanced interface. [SPK:REQ-018]

Each fast-path action MUST be resumable and idempotent at the same repository and lifecycle binding. [SPK:REQ-019]

When a pending-publication or recovery state exists, every verb MUST route recovery before proposing new lifecycle work. [SPK:CON-016]

Each structured result MUST include `verb`, `milestone`, `checkpoint`, `underlyingOperations[]`, `outcome`, `why[]`, `preserved[]`, `stateEffects[]`, and `next[]`. [SPK:REQ-020]

Narration MUST be rendered through the existing command-result and NCL message contracts and MUST NOT calculate or store lifecycle truth. [SPK:CON-017]

Until the current verb milestone is reached, `next[]` MUST teach the checkpoint action while retaining the current fast-path verb as the journey context. [SPK:REQ-021]

After the milestone is reached, `next[]` MUST present the next fast-path verb as the primary continuation for `spec-driven-standard`. [SPK:REQ-022]

### 4.4 Equivalence

For the same initial repository, configuration, identity, choices, model outputs, and human decisions, a fast-path execution and the corresponding advanced operations MUST produce identical authoritative workflow state, events, artifact hashes, approvals, checks, evidence, commits, and pending-publication behavior. [SPK:REQ-180]

The equivalence test MUST compare normalized timestamps and non-authoritative narration separately so presentation differences cannot hide state differences. [SPK:REQ-023]

## 5. `spec-driven-standard` profile — P1

Flow MUST ship `spec-driven-standard` as an ordinary work type defined entirely through the existing workflow configuration and schemas. [SPK:REQ-030]

The profile MUST contain the ordered lifecycle phases `specification → planning → implementation → convergence → verification → release`. [SPK:REQ-031]

The constitution MUST be pinned configuration context and MUST NOT be represented as a Story phase. [SPK:CON-018]

The task map MUST be an advisory member of the planning artifact set and MUST NOT be represented as a lifecycle phase. [SPK:CON-019]

The profile MUST use existing phase-generation, input, approval, world-model, sequence-gate, work-interval, evidence, and publication mechanisms without profile-specific engine branches. [SPK:CON-020]

The `convergence` phase MUST allow an explicit return to `implementation` through governed rework and an explicit human advance to `verification`. [SPK:REQ-032]

The profile MUST apply conservative defaults: required clarification for specification, required specification-quality confirmation, required approved upstream inputs, explicit model consent, required publication, and human advancement from convergence. [SPK:REQ-033]

## 6. Familiar artifacts and protected metadata — P1

The specification artifact set MUST expose `spec.md` as its primary human-facing member and `checklists/requirements.md` as its quality member. [SPK:REQ-040]

The planning artifact set MUST expose `plan.md` as its primary human-facing member and `tasks.md` as an advisory task-map member. [SPK:REQ-041]

The verification artifact set MUST expose evidence below `verification/` and its final human-readable trace as `conformance.md`. [SPK:REQ-042]

Artifact paths MAY remain below the governed work-item directory, but their portable leaf names and contents MUST NOT depend on a proprietary editor. [SPK:CON-021]

Canonical lifecycle metadata MUST live in kernel-owned sidecars, review packets, and `workflow.json` rather than large managed blocks inside human-authored Markdown. [SPK:CON-022]

Each sidecar MUST contain a schema version, subject, phase, generation, artifact path and hash, configuration and template hashes, input references, producer identity, publication binding, and its own reproducible integrity hash. [SPK:REQ-043]

Sidecars MUST be written only by registered kernel operations, MUST be protected by the existing governed-path policy, and MUST be excluded from model write scope. [SPK:CON-023]

Imported human documents MUST have forged Flow metadata removed or ignored before the kernel creates canonical sidecars. [SPK:CON-024]

Moving identical artifact bytes to a different governed path MUST create a distinct sidecar binding. [SPK:REQ-044]

## 7. Specification quality and clarification — P2

### 7.1 Policy

Workflow phases MAY define a `specificationQuality` policy with `mode: off|warn|enforce`, a versioned checklist definition, deterministic-analysis settings, and optional assisted-analysis settings. [SPK:REQ-050]

The starter specification phase MUST use `mode: enforce`. [SPK:REQ-051]

The starter checklist MUST contain stable articles for completeness, ambiguity, consistency, verifiability, boundary conditions, and non-functional requirements. [SPK:REQ-052]

Checklist definitions MUST be pinned into Story resolution with their source revision and content hash. [SPK:REQ-053]

### 7.2 Semantic separation

Every surface MUST label specification quality as “is the requirement good enough?”, verification as “does the implementation satisfy it?”, and conformance as “does the evidence trace to approved intent?”. [SPK:CON-025]

Specification-quality decisions MUST NOT be presented as implementation test results or semantic conformance verdicts. [SPK:CON-026]

### 7.3 Deterministic analysis

`sflow spec analyze` MUST run without a model and MUST bind its report to the artifact path, artifact hash, clause-index hash, phase, generation, and analysis-policy hash. [SPK:REQ-054]

The deterministic analyzer MUST report malformed or duplicate clause anchors, dangling or cyclic dependencies, invalid test bindings, duplicate normalized active-clause text, unresolved clarification markers, missing required scenario sections, and artifact-integrity changes during analysis. [SPK:REQ-055]

The deterministic analyzer MUST NOT claim that prose is semantically complete, clear, consistent, or correct. [SPK:CON-027]

Undefined domain terms, ambiguous wording, conflicting meaning, missing business behavior, and other semantic concerns MUST be emitted only as assisted candidates or human review observations. [SPK:CON-028]

For identical artifact bytes, path, pinned inputs, and analysis policy, deterministic analysis MUST produce byte-identical findings after exclusion of the recorded observation timestamp. [SPK:REQ-056]

### 7.4 Assisted quality analysis

`sflow spec analyze --assisted` MAY create a bounded narrative and candidate-finding record through a governed relay contract. [SPK:REQ-057]

Assisted analysis MUST reference rather than rewrite the deterministic report and MUST NOT add, remove, suppress, or reclassify a deterministic finding. [SPK:CON-029]

An assisted record MUST capture model and provider, prompt hash, exact input hashes, candidate text, cited clause IDs, usage, and generation time. [SPK:REQ-058]

### 7.5 Reviewer-owned checklist

The review packet MUST render the checklist definition, deterministic findings, assisted candidates when present, open clarification markers, and prior exceptions. [SPK:REQ-059]

Every approval counted toward the phase threshold MUST record one decision for every required checklist article: `satisfied`, `exception`, or `not-applicable`. [SPK:REQ-060]

An `exception` or `not-applicable` decision MUST include a human-authored reason and MUST satisfy the configured exception authority. [SPK:REQ-061]

A model MAY summarize evidence for the reviewer but MUST NOT create the checklist confirmation attributed to the human identity. [SPK:CON-030]

In enforce mode, approval MUST fail when a required checklist article is absent from the approval record. [SPK:REQ-181]

### 7.6 Clarification markers

The exact marker grammar MUST be `[NEEDS CLARIFICATION: <question>]`, where `<question>` is non-empty, single-line text within configured length limits. [SPK:REQ-062]

Marker extraction MUST ignore fenced code, inline code, HTML comments, and kernel-managed blocks consistently with clause extraction. [SPK:REQ-063]

The resolved phase MUST pin a marker policy of `off`, `warn`, or `block` separately from the conversational clarification mode. [SPK:REQ-064]

In `block` mode, unresolved markers MUST prevent publication or submission before any state mutation; in `warn` mode they MUST appear in status, narration, and the review packet. [SPK:REQ-065]

A clarification answer MUST be recorded through the existing clarification contract with marker identity, question hash, answer, human identity, phase, generation, and timestamp. [SPK:REQ-066]

A marker is resolved only when a later artifact generation removes it and records the answer and prior artifact hash as inputs; deleting text without a matching clarification record MUST remain an integrity warning or failure according to policy. [SPK:REQ-067]

### 7.7 Scenario-first templates

The starter `spec.md` template MUST lead with prioritized user scenarios and Given/When/Then acceptance cases before general requirements. [SPK:REQ-068]

The template MUST include actors, happy paths, failure and empty states, permissions, boundary conditions, non-functional requirements, assumptions, and explicit clarification markers. [SPK:REQ-069]

## 8. Kernel-owned convergence — P2

### 8.1 Altitude and inputs

`story converge` MUST be the canonical kernel operation behind `sflow converge`. [SPK:REQ-070]

Reconciliation MUST remain the path-altitude authority for changed paths, interval baseline, protected-path policy, planned paths, claims, checks, and source evidence. [SPK:CON-031]

Convergence MUST operate at requirement altitude by consuming the exact reconciliation output and joining it with approved clause indexes, planned and observed claims, test evidence, deviations, and conformance inputs. [SPK:REQ-071]

Convergence MUST NOT re-enumerate or independently classify changed paths already governed by reconciliation. [SPK:CON-032]

Each iteration MUST bind the configuration revision, constitution hash, approved specification and planning generations, clause-index hashes, reconciliation record and hash, source base and target commits, claim-map hashes, and evidence-record hashes. [SPK:REQ-072]

### 8.2 Deterministic default

`sflow converge` MUST default to `modelPolicy: never` and produce deterministic convergence facts. [SPK:REQ-073]

Deterministic facts MUST include absent observed claims for active implementation clauses, unclaimed changed paths reported by reconciliation, stale or invalid claim bindings, missing or failing bound tests, claimed withdrawn clauses, unresolved deviations, and missing required evidence. [SPK:REQ-074]

An absent claim or unclaimed path MUST be described as missing trace evidence and MUST NOT by itself be asserted as semantically missing or unplanned implementation. [SPK:CON-033]

The deterministic fact record MUST be canonical and byte-stable for the same bound inputs after exclusion of observation time. [SPK:REQ-075]

### 8.3 Assisted candidates

`sflow converge --assisted` MAY inspect the approved intent and bounded current implementation evidence to propose candidate gaps of `missing`, `partial`, `contradicts`, or `unplanned`. [SPK:REQ-076]

Assisted candidate analysis MUST consume the deterministic convergence facts and MUST NOT replace, suppress, or mutate them. [SPK:CON-034]

Each candidate MUST cite clause IDs, source or test evidence, deterministic facts when applicable, and its model-input receipt. [SPK:REQ-077]

Assisted candidates MUST remain advisory until individually adjudicated by a human. [SPK:CON-035]

### 8.4 Governed findings and output

The kernel MUST assign immutable content-derived IDs to deterministic facts, assisted candidates, and governed findings. [SPK:REQ-078]

The human MUST be able to adjudicate an item as `rework`, `accepted-deviation`, `dismissed`, or `deferred`, with a mandatory reason for every disposition except direct deterministic rework. [SPK:REQ-079]

The authoritative `convergence.json` MUST be a deterministic projection of the bound input records and recorded human adjudications; raw model prose MUST remain in a referenced candidate record. [SPK:REQ-080]

`convergence.json` MUST expose iteration, bound hashes, facts, candidates by reference, governed findings, dispositions, unresolved blockers, and allowed next transitions. [SPK:REQ-081]

The agent running convergence MUST NOT approve a finding, modify an approved specification, create a change request, accept a deviation, or advance the phase. [SPK:CON-036]

### 8.5 Rework and advancement

Selecting `rework` MUST create a structured change request through the existing approval-authority and lifecycle transition path, targeting `implementation` and the selected clause IDs when available. [SPK:REQ-182]

Rework MUST preserve prior convergence records, findings, approvals, artifacts, and evidence while creating the next governed implementation generation. [SPK:REQ-082]

After the next implementation publication, the Story MUST enter a new convergence iteration bound to the new source and evidence hashes. [SPK:REQ-083]

Advancement from convergence to verification MUST require an explicit human action and MUST fail while unresolved blocking findings remain. [SPK:REQ-183]

The kernel MUST refuse configuration or commands that autonomously repeat implementation and convergence until a condition becomes true. [SPK:CON-037]

Convergence MUST NOT replace final verification or conformance; it is the pre-verification requirement-altitude closure loop. [SPK:CON-038]

## 9. One constitution — P2

### 9.1 Location and pinning

The approved configuration branch MUST contain one `singularity/constitution.md`. [SPK:REQ-090]

Story start MUST materialize and pin the exact constitution path, file hash, constitution-index hash, configuration commit, and policy-resolution hash. [SPK:REQ-091]

An active Story MUST continue using its pinned constitution when `sflow/config` advances. [SPK:CON-039]

The constitution MUST be a configuration input applied across phases and MUST NOT maintain separate lifecycle state. [SPK:CON-040]

### 9.2 Article model

Every active constitution article MUST have an immutable stable ID and one type: `enforced` or `judged`. [SPK:REQ-092]

An `enforced` article MUST reference one resolvable machine-policy path and MUST be rendered from the normalized effective policy value and a versioned renderer. [SPK:REQ-093]

The prose, policy reference, policy value hash, renderer version, and article hash of an enforced article MUST be generated together and MUST fail validation after a hand edit. [SPK:CON-041]

A `judged` article MUST contain authored prose, `level: must|should`, and `evidenceRequired: true|false`. [SPK:REQ-094]

The constitution MAY use YAML front matter for article metadata, but the visible Markdown body MUST contain the matching article anchor and readable text. [SPK:REQ-095]

A generated constitution index MUST join front-matter metadata, visible article anchors, content hashes, policy bindings, and generation provenance without becoming a second approval authority. [SPK:CON-042]

Judged article IDs and prose MUST be immutable after approval; replacement MUST withdraw the old article and allocate a new ID. [SPK:REQ-096]

### 9.3 Regeneration and examples

`sflow constitution generate` MUST regenerate only enforced articles from the approved policy resolution while preserving judged articles byte-for-byte. [SPK:REQ-097]

For the same configuration and renderer version, constitution generation MUST be byte-identical. [SPK:REQ-098]

Constitution changes MUST be proposed and reviewed through the configuration-authority workflow and MUST NOT write application `main`. [SPK:CON-043]

The distribution MUST ship one enforced and two judged examples under `examples/constitution/`; sample articles MUST NOT become active policy on installation. [SPK:REQ-099]

### 9.4 Lifecycle use

Specification and planning templates MUST cite applicable constitution article IDs. [SPK:REQ-100]

The kernel MUST validate cited ID existence and pinned revision before publication and MUST render the cited articles in review packets. [SPK:REQ-101]

Implementation prompt composition, convergence, and final conformance MUST include the minimum cited or evidence-required constitution articles and their pinned index hash. [SPK:REQ-102]

A model MAY propose evidence or a candidate verdict for a judged article, but the conformance verdict for that article MUST be recorded by a human authority. [SPK:CON-044]

An exception to a constitution article MUST record article ID, reason, scope, actor, authority, timestamp, expiry when applicable, and exact Story/source binding. [SPK:REQ-103]

Every exception MUST appear in the review packet, Story evidence, trace output, and final conformance. [SPK:REQ-104]

Enforced-article regeneration from policy and hand-edit rejection MUST be covered by an integration test. [SPK:REQ-184]

## 10. Typed artifact sets and advisory tasks — P2

Specification, planning, and verification phases MUST support typed artifact sets whose primary and supporting members are individually hashed and catalogued. [SPK:REQ-110]

Member-scoped review and change requests MAY identify affected members, but approval MUST remain bound to the exact complete phase bundle. [SPK:CON-045]

Surgical reopen MUST preserve unchanged member bytes and hashes, regenerate only selected members, disclose any incidental change, and require a new exact-bundle approval. [SPK:REQ-111]

`tasks.md` MUST be derived from the approved specification and planning generation and MUST record their hashes. [SPK:REQ-112]

Task items SHOULD cite applicable clause IDs and expected paths or checks. [SPK:REQ-113]

Completion markers in `tasks.md` MAY guide checkpoints, continuation, and progress narration but MUST NOT be treated as kernel evidence that implementation or verification is complete. [SPK:CON-046]

## 11. Configuration and schemas

The workflow-definition schema MUST add `specificationQuality`, fast-path milestone mapping, convergence policy, constitution policy, and artifact-set definitions with `additionalProperties: false` at every new governed object. [SPK:REQ-120]

The resolved Story snapshot MUST pin every new policy object and hash rather than re-reading moving configuration during an active Story. [SPK:REQ-121]

The operation catalog MUST classify deterministic analysis, convergence facts, constitution validation, and constitution generation as `modelPolicy: never`. [SPK:REQ-122]

Assisted analysis operations MUST be explicitly classified as model-optional or model-required and MUST name the deterministic fallback. [SPK:REQ-123]

New records MUST have versioned JSON schemas for fast-path results, quality analysis, checklist decisions, clarification markers, convergence facts, assisted candidates, convergence projection, constitution index, and artifact sidecars. [SPK:REQ-124]

All governed paths from artifacts, tasks, candidates, evidence, and constitution metadata MUST pass the existing repository-containment and regular-file checks. [SPK:CON-047]

## 12. Security, reliability, and performance

Fast-path and convergence mutations MUST use the existing publication transaction, expected-head validation, isolated index, fast-forward push, and recovery journal. [SPK:CON-048]

No new command MAY invoke a shell fragment supplied by an artifact, checklist, constitution, model response, or assisted candidate. [SPK:CON-049]

Deterministic analysis and convergence MUST impose bounded artifact bytes, clause counts, path counts, evidence counts, and output sizes with configuration limits. [SPK:REQ-130]

Assisted operations MUST receive bounded reference envelopes rather than unrestricted repository or evidence content. [SPK:REQ-131]

Read-only status, review rendering, `spec analyze`, and default convergence MUST NOT invoke a model or external network service. [SPK:CON-050]

All generated records MUST redact configured secrets and MUST NOT persist host credentials, tokens, or raw secret-bearing tool output. [SPK:CON-051]

The fast-path routing overhead excluding its underlying operation SHOULD remain below 100 ms at the 95th percentile for a local repository with 10,000 lifecycle/evidence records. [SPK:REQ-132]

Deterministic analysis and convergence SHOULD complete within five seconds for configured default limits, excluding registered test-command execution. [SPK:REQ-133]

## 13. Compatibility and migration

The pack MUST apply automatically only to new Stories created with `spec-driven-standard`. [SPK:CON-052]

Existing active Stories MUST retain their pinned workflow, configuration, artifacts, commands, and naming behavior. [SPK:CON-053]

No migration MAY reinterpret an existing artifact, task checkbox, checklist, or model narrative as a new authoritative SPK record. [SPK:CON-054]

Repositories with specification mode disabled MUST remain behaviorally unchanged. [SPK:CON-055]

The installer and packaged VS Code extension MUST contain the new profile, templates, schemas, skills, examples, and canonical help content. [SPK:REQ-140]

## 14. Surfaces and narration

The CLI, skills, VS Code Journey, Inbox, review packets, pull-request narrative, status, report, and offline trace MUST project the same fast-path milestone and checkpoint from the same command result. [SPK:REQ-150]

VS Code SHOULD present the five verbs as the primary journey rail for `spec-driven-standard` and allow expansion into the underlying phases, generations, evidence, and advanced operations. [SPK:REQ-151]

The review surface MUST keep checklist confirmation, verification evidence, conformance, convergence findings, and constitution exceptions visually distinct. [SPK:CON-056]

Refusals MUST state what did not happen, what was preserved, and the exact recovery or continuation action through the NCL contract. [SPK:REQ-152]

## 15. Acceptance and tripwire suite

The suite MUST prove alias equivalence through `test/spk-fastpath.test.mjs` using identical fixtures for fast-path and advanced operations. [SPK:AC-001]

The suite MUST prove per-approval checklist confirmation, exception authority, and model non-ownership through `test/spk-quality.test.mjs`. [SPK:AC-002]

The suite MUST prove that a selected finding creates rework only through the existing governed change-request transition in `test/spk-converge.test.mjs`. [SPK:AC-003]

The suite MUST prove that every convergence iteration and advancement requires an explicit human action in `test/spk-converge.test.mjs`. [SPK:AC-004]

The suite MUST prove enforced-article regeneration, byte stability, pinning, and hand-edit rejection in `test/spk-constitution.test.mjs`. [SPK:AC-005]

The suite MUST retain and extend v1 marker-policy, scenario-template, artifact-set, surgical-reopen, and task-reference fixtures. [SPK:AC-006]

Tripwire tests MUST prove that aliases cannot bypass a gate, tasks cannot gate, models cannot confirm checklists, assisted candidates cannot mutate deterministic facts, convergence cannot re-derive path findings, agents cannot create rework, constitution text cannot override policy, and old Stories cannot adopt moving configuration. [SPK:AC-007]

One end-to-end fixture MUST execute specification, planning, implementation, deterministic convergence, human-selected rework, a second convergence iteration, verification, conformance, and release from a fresh clone. [SPK:AC-008]

Packaging tests MUST execute at least one fast-path command and constitution validation from the exact installed CLI and VS Code extension layouts. [SPK:AC-009]

## 16. Delivery increments

P1 MUST deliver the five verbs, router, NCL result mapping, `spec-driven-standard`, familiar artifact names, protected sidecars, packaging, and equivalence tests. [SPK:REQ-160]

P2A MUST deliver deterministic quality analysis, assisted-quality candidates, reviewer checklist decisions, clarification markers, scenario templates, typed artifact sets, and advisory tasks. [SPK:REQ-161]

P2B MUST deliver deterministic convergence facts, assisted candidates, human adjudication, governed rework, iteration, and explicit advancement. [SPK:REQ-162]

P2C MUST deliver constitution generation, judged articles, pinning, citations, exceptions, and human conformance. [SPK:REQ-163]

P2D MUST deliver cross-surface integration, offline trace, telemetry, packaging, crash recovery, and the end-to-end fixture. [SPK:REQ-164]

P3 MAY begin only through the companion `PKG`, `ADP`, and fold-tooling specifications. [SPK:CON-057]

## 17. Product success criteria

The release MUST measure time and action count from Story start to first reviewable specification, fast-path completion rate, advanced-command fallback rate, clarification count, checklist exceptions, convergence iterations, and rework discovery phase. [SPK:REQ-170]

The release MUST measure input, cached-input, and output tokens; injected artifact and world-model bytes; deterministic versus assisted operation counts; duration; rejection; and rework using the same repository, model, work type, and task for comparison. [SPK:REQ-171]

Token reduction MUST NOT be accepted when clarification, approval evidence, identity warnings, conformance quality, or publication safety regresses. [SPK:CON-058]

The release target is zero authoritative differences between fast-path and advanced-operation fixtures and zero constitution-projection drift. [SPK:REQ-172]

## 18. Definition of done

The pack is complete only when all SPK schemas validate, extraction closes with no duplicate or unanchored normative clauses, every operation has a model-policy classification, all tripwires pass, full Node and VS Code suites pass, deterministic checks pass, packaging contains every asset, and the end-to-end fixture succeeds from a fresh clone. [SPK:AC-010]

The product is ready for default onboarding only after a representative user can complete the golden Story using the five verbs and review decisions without learning the underlying phase-command vocabulary. [SPK:AC-011]

## Appendix A — Reference profile shape

The following shape is illustrative; the repository schema remains authoritative.

```yaml
workTypes:
  spec-driven-standard:
    label: Spec-Driven Standard
    phases:
      - specification
      - planning
      - implementation
      - convergence
      - verification
      - release
    fastPath:
      specify: { milestone: specification-approved }
      plan: { milestone: planning-approved }
      implement: { milestone: implementation-published }
      converge: { milestone: convergence-advanced }
      verify: { milestone: verification-approved }
    constitution:
      path: singularity/constitution.md
      mode: enforce
    convergence:
      mode: enforce
      defaultAnalysis: deterministic
      assisted: optional
      advance: human
    phaseOverrides:
      specification:
        clarification:
          mode: required
          markers: { mode: block }
        specificationQuality:
          mode: enforce
          checklist: requirements-quality-v1
      planning:
        artifactSet: spec-driven-planning
      implementation:
        worldModel: { required: [developer] }
      verification:
        worldModel: { required: [testing] }
```

## Appendix B — Artifact-set shape

```yaml
artifactSets:
  spec-driven-specification:
    primary: spec.md
    members:
      - path: spec.md
        role: specification
        required: true
      - path: checklists/requirements.md
        role: specification-quality
        required: true
  spec-driven-planning:
    primary: plan.md
    members:
      - path: plan.md
        role: implementation-plan
        required: true
      - path: tasks.md
        role: advisory-task-map
        required: false
        authority: advisory
  spec-driven-verification:
    primary: conformance.md
    members:
      - path: conformance.md
        role: conformance
        required: true
      - path: verification/
        role: evidence-collection
        required: true
```

## Appendix C — Convergence records

```json
{
  "schemaVersion": 1,
  "workId": "ENG-142",
  "iteration": 2,
  "bindings": {
    "specIndexSha256": "sha256:...",
    "reconciliationSha256": "sha256:...",
    "sourceBaseCommit": "...",
    "sourceTargetCommit": "...",
    "observedClaimsSha256": "sha256:..."
  },
  "facts": [
    {
      "id": "CF-...",
      "kind": "missing-bound-test",
      "clauseIds": ["ENG-142:AC-003"],
      "evidence": []
    }
  ],
  "candidateRecords": ["context/convergence/candidates-iter2.json"],
  "findings": [
    {
      "id": "GF-...",
      "classification": "partial",
      "disposition": "rework",
      "clauseIds": ["ENG-142:REQ-008"],
      "decision": { "actor": "...", "at": "...", "reason": "..." }
    }
  ],
  "unresolvedBlockers": ["GF-..."],
  "allowedNext": ["create-rework"]
}
```

## Appendix D — Explicitly closed decisions

1. Deterministic convergence is the default; assisted gap analysis is optional and advisory.
2. Constitution examples ship as inactive examples, not installed active principles.
3. Fast-path verbs are the primary `nextsteps` vocabulary for `spec-driven-standard`.
4. Constitution is pinned context, not a phase.
5. Tasks are a planning artifact member, not a phase or gate.
6. A verb stops at human and model boundaries; it never turns five words into an autopilot.
