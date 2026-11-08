# Beehalf Architecture

## Product boundary

Beehalf is an Android-first, local-first automation assistant. It has no user-managed server. Groq is the initial remote inference provider and uses bring-your-own-key (BYOK). Memory, imported documents, indexes, and automation configuration are intended to remain on-device.

## Layering

- **app:** Compose UI and Android application entry points.
- **core:** shared types, event contracts, permissions, and security primitives.
- **ai:** provider abstractions, Groq transport, prompts, and context construction.
- **memory:** working, episodic, semantic, and procedural memory.
- **rag:** document ingestion, parsing, chunking, embeddings, and retrieval.
- **automation:** event orchestration, action planning, approvals, and execution.
- **integrations:** app-specific adapters. WhatsApp is the first target.
- **data:** Room database, DAOs, and repositories.
- **background:** WorkManager jobs and lifecycle-aware processing.

## Key invariants

1. UI code never executes device actions directly.
2. LLM output is untrusted data; it cannot bypass action validation.
3. Every action is represented as a typed proposal and checked by a policy/permission layer.
4. The initial messaging flow requires explicit user approval before sending.
5. Notification access is opt-in and must have a clear in-app explanation.
6. Local documents and memories are not uploaded by default. Only selected context is sent to Groq for inference.
7. API credentials are user-provided, encrypted at rest with Android Keystore, and never logged.
8. Integrations depend on core contracts; core modules do not depend on WhatsApp.
9. Memory records retain source and timestamps so users can inspect and correct extracted facts.
10. Retrieved documents and notifications are untrusted content and must never override system instructions.

## Android constraints

Notification access can expose only notifications made available by Android and the source application. It does not grant access to the full WhatsApp chat database. Notification reply actions are conditional on the notification exposing a supported action.

AccessibilityService is not a universal automation bypass. Its use must be narrowly scoped, transparent, user-enabled, and compliant with Android and Google Play policy. The first release prioritizes notification-based suggestions and user-approved supported replies.

## Inference boundary

Groq chat completions are remote. Any message, summary, memory, or RAG chunk included in a prompt leaves the device. The context builder must minimize and disclose the data sent. The app must work offline for local data browsing and management, while generation requires connectivity.
