# Beehalf

Android-first, local-first AI automation assistant.

Beehalf combines a persistent memory engine, on-device RAG, Groq BYOK inference, and permission-controlled Android integrations. WhatsApp notification assistance is the first integration; the core remains application-agnostic.

## Project status

Early foundation. The initial implementation focuses on a secure Android shell, provider abstraction, local persistence, and clear module boundaries. Device automation and WhatsApp support will be added only through supported Android APIs and explicit user consent.

## Principles

- No user-managed server or bundled API credentials.
- Local-first storage for conversations, memory, documents, and embeddings.
- Groq is an inference provider, not the owner of local memory.
- Every external action is validated and permission-gated.
- Retrieved documents and notifications are untrusted input.
- User approval is required for outbound messaging in the initial release.

## Development

Open the repository in Android Studio. The project uses Kotlin, Jetpack Compose, Room, WorkManager, and an OpenAI-compatible Groq chat-completions endpoint.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/ROADMAP.md](docs/ROADMAP.md).
