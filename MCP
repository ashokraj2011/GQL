# Configure and use MCP with Singularity Flow

Model Context Protocol (MCP) lets an AI agent use tools supplied by another
process. Examples include opening a browser with Playwright, reading a design from
Figma, querying a knowledge system, or inspecting a test environment.

Singularity Flow does not replace the MCP host. It adds governance around the
host so a repository can answer four questions:

1. Which server is this agent allowed to use?
2. During which workflow phases may it use that server?
3. Which individual tools are allowed?
4. Which results must be retained as reviewable evidence?

## The two-layer model

MCP setup has two deliberately separate layers. Both must be configured before a
governed agent can use a server.

```text
VS Code or Copilot CLI                  Singularity Flow repository
------------------------------------    -----------------------------------
Starts and connects to the server       Names the matching host server
Owns credentials and authentication     Allows agents, phases, and tool names
Asks the user to trust the server       Injects the effective policy in prompts
Presents tools to Copilot               Records selected outputs as evidence

          hostReference / server ID must match between the two layers
```

| Concern | Owner | Typical location |
|---|---|---|
| Server command, URL, transport and environment | VS Code or Copilot CLI | `.vscode/mcp.json` or host settings |
| Tokens, cookies and interactive login | MCP host / operating-system credential facilities | Never committed by Flow |
| Allowed agents, phases and tool names | Singularity Flow | `singularity/workflow.yml` |
| Agent instructions and tool namespace | Singularity Flow | `.github/agents/*.agent.md` |
| Durable screenshots, reports and provenance | Active work item | `singularity/work-items/<ID>/context/mcp/` |

This separation is important. A server being installed does **not** give every
agent permission to use it. Conversely, a policy in `workflow.yml` cannot start a
server that the current developer has not configured and trusted.

## Recommended setup sequence

### 1. Open the correct workspace and repository

Select the Singularity Flow workspace in VS Code. MCP host files are
repository-specific, while credentials and trust may be machine-specific. Confirm
the active repository before changing either one.

### 2. Create or review the host configuration

In the VS Code extension, open:

**Configuration → MCP tools → Configure MCP host**

From a terminal in the repository root, the equivalent starter commands are:

```bash
singularity-flow mcp scaffold playwright
singularity-flow mcp scaffold figma
singularity-flow mcp scaffold figma --local
```

Scaffolding merge-safely updates `.vscode/mcp.json`. It preserves unrelated
servers. If an entry with the same ID has different content, Flow refuses to
overwrite it until it is reviewed; use `--replace-server` only after comparing the
existing and proposed entries.

A conceptual Playwright host entry looks like this:

```json
{
  "servers": {
    "playwright": {
      "command": "npx",
      "args": ["-y", "@playwright/mcp@<approved-version>"]
    }
  }
}
```

Use the release-managed version produced by `mcp scaffold` instead of copying the
placeholder above. In VS Code, run **MCP: List Servers** to review, trust and start
the entry. For Copilot CLI, configure the same server using its MCP configuration
command and inspect it with:

```bash
copilot mcp list
```

### 3. Declare the governed policy

In VS Code, open:

**Configuration → MCP tools → Add server policy**

The server ID and `hostReference` must match the host entry. The corresponding
`singularity/workflow.yml` declaration is:

```yaml
mcpServers:
  playwright:
    label: Playwright browser automation
    hostReference: playwright
    agents: [qa, product-designer]
    phases: [verification, visual-verification, conformance]
    tools:
      - browser_navigate
      - browser_snapshot
      - browser_click
      - browser_take_screenshot
    required: false
    approval: confirm
    evidence:
      captureToolCalls: true
      captureResults: true
```

The important fields are:

| Field | Meaning |
|---|---|
| `hostReference` | Server name as the host knows it |
| `agents` | Governed agents allowed to receive its tools |
| `phases` | Workflow phases in which those tools are eligible |
| `tools` | Exact allowlist; tools not listed remain unavailable |
| `required` | Whether lack of host readiness blocks the configured workflow |
| `approval` | Whether the user must confirm access before use |
| `evidence` | Which durable results are expected to be recorded |

Start with `required: false` while adopting MCP. Make a server required only when
every contributor and review environment can configure it reliably.

### 4. Allow the tool namespace on the agent

Open **Configuration → Agents, prompts & skills**, select the governed agent, and
add the server namespace to its allowed tools. In Agent Markdown:

```yaml
---
name: qa
description: Verify behavior and preserve reproducible evidence.
tools: [read, search, "playwright/*"]
---

Use Playwright only during phases and for tools allowed by the governed MCP policy.
Record material screenshots and reports before submitting the phase.
```

Use `server/tool` for one tool or `server/*` for the namespace. The effective
permission is the intersection of the host's available tools, the workflow policy,
the active phase and the selected agent. A broad agent namespace does not override
the workflow allowlist.

### 5. Diagnose readiness

Run:

```bash
singularity-flow mcp status
singularity-flow mcp doctor
```

Or ask Copilot to run `/sf-mcp`.

`status` identifies configured server names without printing commands, headers,
environment values or tokens. `doctor` performs static checks; it does not contact
the server or package registry. Readiness can remain `needs-host-setup` until the
user reviews, starts and authenticates the server in the host.

Where policy requires an explicit machine-local acknowledgement, record it after
reviewing the host entry:

```bash
singularity-flow mcp attest playwright --confirm playwright
```

The attestation is invalidated if either the host entry or governed policy changes.
It records what the user reviewed; it is not proof that Flow intercepted an MCP
transport.

## Using MCP during governed work

Attach or resume the Story before invoking an agent:

```text
/sf-session WORK-123
/sf-verify
```

The prompt composition for that turn contains the phase contract, selected agent,
approved upstream artifacts, selected world-model views, and the effective MCP
policy. Copilot then calls the MCP host normally. Singularity Flow does not place
credentials in the prompt and does not silently start a server from a read-only
command.

A typical Playwright verification turn is:

1. The `qa` agent opens the exact submitted build or approved environment.
2. It navigates and inspects the page with allowed Playwright tools.
3. It saves material screenshots or reports to the active phase artifact folder.
4. It records provenance for the durable output.
5. The normal phase publication commits the evidence and its hashes.

Record a material output with:

```bash
singularity-flow mcp record playwright \
  --tool browser_take_screenshot \
  --phase verification \
  --output singularity/work-items/WORK-123/artifacts/verification/home.png \
  --note "Authenticated home screen after successful login"
```

Flow copies the selected file under:

```text
singularity/work-items/WORK-123/context/mcp/outputs/
```

and writes its typed provenance record under:

```text
singularity/work-items/WORK-123/context/mcp/records/
```

The governance gate verifies the server, phase, governed agent, allowed tool,
output path, size and SHA-256. The record is an explicit declaration by the agent;
the file hash proves which bytes were reviewed and published.

## Figma guidance

Use MCP for live inspection as a convenience, but use pinned exports as canonical
review evidence. Live Figma content can change after intake.

For Figma `get_metadata`, record the exact file key, opaque version and colon-form
node IDs. The returned payload is XML and defaults to
`figma-mcp-metadata-xml`; it is not Figma REST JSON. For governed design-to-code
work, upload or attach PNG/PDF exports and record the live URL separately.

See [Mobile model intake](MOBILE-MODEL-INTAKE.md) for the full design workflow.

## Corporate and offline environments

### Private npm or Artifactory registry

MCP servers started with `npx` use normal npm configuration. Prefer the company's
existing `.npmrc` or environment settings:

```bash
npm config set registry https://artifactory.company.example/api/npm/npm-virtual/

# Or for one VS Code launch:
NPM_CONFIG_REGISTRY=https://artifactory.company.example/api/npm/npm-virtual/ code .
```

The generated MCP host entry does not override registry, proxy, CA or
authentication settings. Pin and pre-approve the MCP package/version in
Artifactory so starting a server does not depend on the public registry.

### Corporate certificate authorities

Configure the company CA through the approved Node/npm and VS Code mechanisms.
Do not add `strict-ssl=false` and do not disable TLS verification. If the server
works in a terminal but not when VS Code starts it, compare the environment used to
launch VS Code, especially `NODE_EXTRA_CA_CERTS`, proxy variables and npm config.

### Secrets

- Do not put tokens, passwords or cookies in `singularity/workflow.yml`.
- Do not put literal secrets in committed `.vscode/mcp.json` entries.
- Prefer host-supported input variables, environment references or operating-system
  credential storage.
- Review MCP output before publishing it; external content is untrusted input.

## Troubleshooting

| Symptom | Likely cause | Action |
|---|---|---|
| `.vscode/mcp.json already exists` | Flow protected an existing host file | Open it, compare entries, then scaffold with `--replace-server` only if intended |
| Policy is present but no tools appear | Host server is missing, stopped or untrusted | Run **MCP: List Servers**, then `mcp status` and `mcp doctor` |
| Server is running but the agent cannot use it | Agent, phase or tool is outside the allowlist | Compare the active agent/phase with `mcpServers` and Agent Markdown |
| `needs-host-setup` remains | Host entry has not been reviewed/attested on this machine | Start/authenticate it, then run `mcp attest` when required |
| `npx` cannot download the package | Registry, proxy, CA or Artifactory package issue | Verify npm configuration from the same environment that launches VS Code |
| Tool succeeds but no evidence is visible | MCP calls are not automatically durable | Save the output and run `mcp record`, then publish the phase |
| A policy change invalidates readiness | Host or policy hash changed | Review the new configuration and create a fresh attestation |
| A URL works live but differs during review | Remote source changed after intake | Use the pinned export/hash as canonical evidence |

## Security and audit boundaries

Singularity Flow records policy and selected durable evidence; it is not an MCP
network proxy. The host does not expose every tool-call event to Flow, so Flow does
not claim complete transport interception. Its auditable guarantees are:

- the repository policy that was active for the generation;
- the governed agent and phase that declared the use;
- the selected output copied into governed context;
- the exact SHA-256 of those bytes; and
- the commit and approval history that published them.

This gives reviewers reproducible evidence without copying host credentials or raw
private MCP traffic into Git.

## Command reference

```bash
singularity-flow mcp scaffold playwright
singularity-flow mcp scaffold figma [--local]
singularity-flow mcp status
singularity-flow mcp doctor
singularity-flow mcp attest <SERVER> --confirm <SERVER>
singularity-flow mcp record <SERVER> --tool <TOOL> --phase <PHASE> --output <PATH> --note <TEXT>
```

Copilot entry point:

```text
/sf-mcp
```
