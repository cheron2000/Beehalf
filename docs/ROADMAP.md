# Roadmap

## M1 — Android foundation (complete)
- [x] Repository and Android application scaffold
- [x] Compose landing screen
- [x] Groq BYOK connection validation
- [x] Android Keystore-backed API key vault
- [x] Groq client unit tests (MockWebServer)
- [x] GitHub Actions unit-test and debug-build workflow
- [ ] Add model picker and inference playground

## M2 — Persistent memory
- [x] Persistent chat screen connected to Room
- [x] Conversation history selection and new conversation creation
- [x] Groq chat generation using bounded local conversation context
- [x] Provider settings retained in the chat experience
- [x] Room schema for conversations, messages, and categorized memory records
- [ ] Working memory and rolling summaries
- [ ] Episodic and semantic memory extraction and lifecycle
- [x] Conversation repository for persistence, retrieval, and deletion
- [x] Bounded chronological context builder (configurable message/token budgets)
- [x] Memory inspection, manual correction, pinning, and deletion UI
- [x] Memory repository validation, bounded retrieval, and unpinned retention operation
- [ ] Retention settings UI and user-configurable retention policy

## M3 — Local RAG
- [ ] Android Storage Access Framework import
- [ ] PDF, TXT, Markdown, CSV, JSON, DOCX, and XLSX parsers
- [ ] Chunking and metadata
- [ ] On-device embedding runtime
- [ ] Hybrid full-text and vector retrieval
- [ ] Source citations and retrieval evaluation set

## M4 — Safe automation core
- [ ] Typed event and action contracts
- [ ] Action policy and permission checks
- [ ] User approval queue
- [ ] Idempotency and audit log
- [ ] Workflow configuration and kill switch

## M5 — WhatsApp notification adapter
- [ ] Opt-in NotificationListenerService
- [ ] Notification parsing and conversation matching
- [ ] Context + RAG + Groq response suggestions
- [ ] Supported notification quick-reply flow with user approval
- [ ] Device/OEM compatibility tests

## M6 — Additional Android integrations
- [ ] Generic notification adapter
- [ ] Supported Android intents
- [ ] Additional app adapters behind explicit user consent

## M7 — Hardening and release
- [ ] Threat model and privacy review
- [ ] Battery and background execution tests
- [ ] Accessibility and Play policy review
- [ ] Data export and deletion verification
- [ ] Release signing and distribution documentation
