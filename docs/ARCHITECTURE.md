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

## Conversation and context flow

- Conversations and messages are persisted in the local Room database.
- The chat UI observes Room-backed conversation and message streams; switching conversations does not discard history.
- Before a Groq request, the context builder selects a configurable bounded recent-message window and restores chronological order.
- The system instruction is added at request time and is not stored as a conversation message.
- The API key is read from the Android Keystore-backed vault only when a request is sent.
- The request context is sent to Groq; local history is not uploaded wholesale.

## Inference boundary

Groq chat completions are remote. Any message, summary, memory, or RAG chunk included in a prompt leaves the device. The context builder must minimize and disclose the data sent. The app must work offline for local data browsing and management, while generation requires connectivity.

## Memory lifecycle

- User-authored memories are stored locally with category, source, confidence, and timestamps.
- Candidate episodic and semantic memories may be proposed from the selected conversation, but are never persisted automatically. Each candidate requires an explicit user approval action.
- Candidate extraction is instructed to omit sensitive personal data and rejects malformed, low-confidence, unknown-category, or oversized outputs.
- A conversation has at most one rolling working summary. The summary repository only incorporates older messages outside the recent-message window and advances a source watermark after successful summarization.
- Working summaries are local Room memory records and are included as untrusted reference context in later Groq requests. The user can inspect, edit, pin, or delete them in the memory screen.
- Rolling summary generation is best-effort; failure must not turn an already-saved assistant reply into a failed chat turn.

## Local document ingestion

- Documents are selected through Android's Storage Access Framework using the system document picker; Beehalf does not request broad storage permission.
- The app takes a persistable read grant and stores the content URI plus display name, MIME type, size (when available), and import timestamp in Room.
- Duplicate imports of the same URI update the existing document record rather than creating duplicate entries.
- Removing a document deletes its local metadata record and releases the persisted URI grant where possible.
- SAF imports retain a persistent read-only URI grant and local metadata; document contents are opened only when the user requests reading.
- TXT and Markdown parsing currently runs on-device with a 2 MB input limit, UTF-8/UTF-16 BOM handling, line-ending normalization, and binary-content rejection. Markdown source is preserved rather than rendered so headings, lists, and code fences remain available to future chunking.
- The current UI preview is capped at 12,000 characters. Parsed content is not persisted, embedded, indexed, or uploaded in this parser slice.
- PDF, CSV, JSON, DOCX, and XLSX parsing, chunking, embeddings, and retrieval remain separate roadmap work.
