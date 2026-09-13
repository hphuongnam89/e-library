-- ==============================================================================
-- E-LIB Least-Privilege Database Role Hardening Script (Phase 14)
-- ==============================================================================
-- Architecture:
--   1. elib_owner:   Schema owner for running Flyway migrations / DDL during deploy.
--   2. elib_app:     Runtime application user (DML only: SELECT, INSERT, UPDATE, DELETE).
--                    NO DROP, NO ALTER, NO TRUNCATE, NO DDL.
--   3. elib_readonly: Read-only reporting & analytical replica user (SELECT only).
-- ==============================================================================

-- 1. Create Roles if they do not exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'elib_owner') THEN
        CREATE ROLE elib_owner WITH LOGIN PASSWORD 'CHANGE_IN_PROD_OWNER_PWD';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'elib_app') THEN
        CREATE ROLE elib_app WITH LOGIN PASSWORD 'CHANGE_IN_PROD_APP_PWD';
    END IF;

    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'elib_readonly') THEN
        CREATE ROLE elib_readonly WITH LOGIN PASSWORD 'CHANGE_IN_PROD_READONLY_PWD';
    END IF;
END
$$;

-- 2. Database Connection Privileges
REVOKE CONNECT ON DATABASE elib FROM PUBLIC;
GRANT CONNECT ON DATABASE elib TO elib_owner;
GRANT CONNECT ON DATABASE elib TO elib_app;
GRANT CONNECT ON DATABASE elib TO elib_readonly;

-- 3. Schema Ownership & Usage
ALTER SCHEMA elib OWNER TO elib_owner;
GRANT USAGE, CREATE ON SCHEMA elib TO elib_owner;

GRANT USAGE ON SCHEMA elib TO elib_app;
GRANT USAGE ON SCHEMA elib TO elib_readonly;

-- 4. Application Runtime Privileges (Least Privilege DML Only)
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA elib TO elib_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA elib TO elib_app;

-- Explicitly revoke destructive privileges from application runtime
REVOKE TRUNCATE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA elib FROM elib_app;
REVOKE CREATE ON SCHEMA elib FROM elib_app;

-- 5. Read-Only Reporting Privileges
GRANT SELECT ON ALL TABLES IN SCHEMA elib TO elib_readonly;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON ALL TABLES IN SCHEMA elib FROM elib_readonly;

-- 6. Future Default Privileges (Ensures tables created by Flyway migration inherit correct permissions)
ALTER DEFAULT PRIVILEGES FOR ROLE elib_owner IN SCHEMA elib
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO elib_app;

ALTER DEFAULT PRIVILEGES FOR ROLE elib_owner IN SCHEMA elib
    GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO elib_app;

ALTER DEFAULT PRIVILEGES FOR ROLE elib_owner IN SCHEMA elib
    GRANT SELECT ON TABLES TO elib_readonly;

-- 7. Audit & Verification Output
SELECT 
    grantee, 
    table_schema, 
    string_agg(privilege_type, ', ' ORDER BY privilege_type) AS privileges
FROM information_schema.role_table_grants 
WHERE table_schema = 'elib' AND grantee IN ('elib_owner', 'elib_app', 'elib_readonly')
GROUP BY grantee, table_schema;
