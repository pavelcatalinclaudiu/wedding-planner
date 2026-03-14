DROP PROCEDURE IF EXISTS migrate_v27;

CREATE PROCEDURE migrate_v27()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name   = 'guests'
          AND column_name  = 'invite_token'
    ) THEN
        ALTER TABLE guests ADD COLUMN invite_token CHAR(36) NULL UNIQUE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name   = 'guests'
          AND column_name  = 'invite_opened_at'
    ) THEN
        ALTER TABLE guests ADD COLUMN invite_opened_at DATETIME NULL;
    END IF;
END;

CALL migrate_v27();
DROP PROCEDURE IF EXISTS migrate_v27;

