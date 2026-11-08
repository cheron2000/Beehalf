# Security and Privacy

## Credentials
- Users provide their own Groq API key.
- The key is encrypted using AES-GCM with a key held by Android Keystore.
- Never put keys in source control, build configuration, logs, analytics, or crash reports.
- Rotate or remove credentials from Settings.

## Data handling
- Conversation and document data are local-first.
- Do not upload documents or embeddings by default.
- Prompts sent to Groq may contain user data. Show this clearly and minimize context.
- Provide user-visible retention, export, and deletion controls before production release.

## Automation
- Treat all LLM output as untrusted.
- Validate action type, target, parameters, permission, and approval state.
- Require approval for outbound messages and sensitive actions.
- Make action execution idempotent and auditable.
- Do not collect passwords, OTPs, or protected credentials from notifications.

## Android permissions
- Request notification access only when the user enables the WhatsApp integration.
- Explain what notification data is read and how it is stored.
- Provide an obvious pause/disable control.
- Do not use AccessibilityService as a general-purpose autonomous control bypass.
