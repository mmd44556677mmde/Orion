# Maintenance

1. Read `docs/RELEASE_STATUS.md`, `SECURITY_REVIEW.md` and `docs/project-map.json` before changes; inspect the implementation and tests, not just comments. Preserve the current branch and existing user changes.
2. Keep UI strings localized, network and persistence cancellable, credentials out of source/logs, and execution behind app-owned authorization and explicit consent. Domain policies alone are not enforcement.
3. Implement missing features against real boundaries with focused failure, lifecycle, privacy and integration tests; do not replace absent integrations with production fakes. Do not claim support from a domain-only test.
4. In an Android-equipped network-enabled environment, establish reviewed dependency locks and verification metadata, run the clean build/test/lint/device matrix in `HANDOFF.md`, fix failures and record exact artifacts and checksums. Review dependency licenses and select a project license with the rights holder.
5. Update the feature inventory, privacy disclosure and machine-readable map when behavior or paths change. Review manifest/exported components, backup rules, debug/release separation, sensitive logs and signing independently before publishing.
