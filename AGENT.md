# E-LIB agent rules

Read `docs/development-status.md`, then the relevant requirements/decisions/contracts before changes. Implement only the authorized phase. Do not invent domain, policies, users or production status. Run relevant build/tests and update the status with actual evidence. Use Flyway; never Hibernate schema update or destructive volume reset. Preserve secrets in ignored `.env`. Never add fake API fallbacks. Keep error/security behavior consistent. Use small, verified Git checkpoints; do not push without authorization.

Code discovery: prefer codebase-memory MCP search/trace/snippets; verify stale index paths and fall back to targeted file reads. Shell commands use `rtk` (`rtk proxy` for commands not supported directly). Do not dump whole files or logs unnecessarily.
