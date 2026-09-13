-- Flyway creates the elib schema and its history table before this migration.
-- Domain tables are introduced by the phase that owns them, never by Hibernate.
COMMENT ON SCHEMA elib IS 'E-LIB application schema managed by Flyway';
