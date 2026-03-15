-- Allow ADMIN as a valid user role
ALTER TABLE users
    DROP CONSTRAINT chk_users_role,
    ADD CONSTRAINT chk_users_role CHECK (role IN ('COUPLE', 'VENDOR', 'ADMIN'));
