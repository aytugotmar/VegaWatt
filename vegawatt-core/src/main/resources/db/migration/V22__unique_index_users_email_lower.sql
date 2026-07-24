-- Defense in depth alongside app-level email normalization (register/change-email now both
-- trim().toLowerCase() before the uniqueness check and save): a case-insensitive unique index
-- means "Foo@x.com" and "foo@x.com" can never both exist as rows, even if something ever bypasses
-- the application layer. Additive — the original plain UNIQUE constraint on `email` from
-- V11__create_users_table.sql is left untouched.
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));
