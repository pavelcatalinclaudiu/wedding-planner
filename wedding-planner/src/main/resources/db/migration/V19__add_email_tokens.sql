-- Email verification and password reset tokens
ALTER TABLE users
    ADD COLUMN email_verification_token    VARCHAR(255) NULL,
    ADD COLUMN email_verification_expires  DATETIME     NULL,
    ADD COLUMN password_reset_token        VARCHAR(255) NULL,
    ADD COLUMN password_reset_expires      DATETIME     NULL;

CREATE INDEX idx_users_email_verification_token ON users (email_verification_token);
CREATE INDEX idx_users_password_reset_token     ON users (password_reset_token);
