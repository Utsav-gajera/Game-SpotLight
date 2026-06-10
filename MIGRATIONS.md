# Database Migrations

Flyway migrations have been removed from this repository. The per-service Flyway dependency, migration scripts, and the CI workflow that executed them were removed to simplify deployment.

If you still need to run schema migrations, choose one of the following:
- Restore migration scripts under `services/<service>/src/main/resources/db/migration` and re-add Flyway to that service.
- Manage migrations externally (database tool, dedicated migration runner, or an operator-managed job).

Notes:
- Removing Flyway from the repo means applications will not run automatic migrations on startup.
- Ensure your production databases are provisioned and migrated before deploying services that depend on schema changes.

Manual migration: converting `user_roles` to `users.role`
-----------------------------------------------------
If you previously used a `user_roles` join table, a helper SQL migration is included for Postgres at:

- `services/auth-user-service/src/main/resources/db/migration/V2__migrate_user_roles_to_role.sql`

Run it manually against your database (ensure you have a DB backup and maintenance window). Example using `psql`:

```bash
psql "postgresql://USER:PASSWORD@HOST:PORT/DBNAME" -f services/auth-user-service/src/main/resources/db/migration/V2__migrate_user_roles_to_role.sql
```

If you prefer Flyway-managed execution, restore Flyway to the `auth-user-service` `pom.xml` and run the Flyway migration via Maven or CI.

