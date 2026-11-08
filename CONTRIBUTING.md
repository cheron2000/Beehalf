# Contributing

1. Keep modules independent and use explicit interfaces between them.
2. Do not add secrets, API keys, personal data, or real notification dumps.
3. Add unit tests for memory, retrieval, provider, and action-policy logic.
4. Keep Android permissions opt-in and document the user-facing reason.
5. Never let model output call Android APIs directly.
6. Run the Android build and tests before opening a pull request.

See [Architecture](docs/ARCHITECTURE.md), [Roadmap](docs/ROADMAP.md), and [Security](docs/SECURITY.md).
