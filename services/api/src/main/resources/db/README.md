# Database migrations

`migration/B77__fresh_install_baseline.sql` is the complete PostgreSQL 16 baseline for a new deployment. Flyway applies it only when building a new database. Existing databases that already reached V77 skip the baseline and continue with V78. The snapshot contains schema plus required system configuration, but no users or business data.

The historical V1-V77 files were intentionally consolidated. `spring.flyway.ignore-migration-patterns=*:missing` allows their existing schema-history rows while checksums and ordering remain enforced for all migrations still shipped with the application.

Fresh deployments must set `BOOTSTRAP_ADMIN_PASSWORD` on the first application start. The application creates the configured bootstrap administrator only when that username does not already exist and never overwrites its password.

The current incremental schema version is V105. Use the next unused version above the latest shipped migration for future changes. This keeps upgrades compatible with databases that previously reached V77 as well as new databases installed from the B77 baseline. Never edit a versioned migration after it has run in any shared environment.

V101 contains compatibility logic for legacy maintenance schemas on this branch. Before rollout, compare the V101 checksum recorded in `flyway_schema_history` with the migration packaged in the release. An environment that already recorded an older V101 checksum can fail validation and must follow an explicitly reviewed migration or repair procedure. Do not run `flyway repair` blindly: first confirm that the database schema is equivalent, take a backup, and record the approval. All future compatibility changes must use a new migration version instead of changing V101 again.

V105 closes the operational control gaps by adding expense payment audit fields, payment applicant/approver/executor user IDs, and pending accounting-period action fields used for independent review of forced close and reopen requests.
