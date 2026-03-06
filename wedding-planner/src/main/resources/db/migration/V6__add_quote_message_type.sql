-- Add QUOTE to the allowed message types
ALTER TABLE messages DROP CHECK chk_messages_type;
ALTER TABLE messages ADD CONSTRAINT chk_messages_type
    CHECK (type IN ('TEXT','IMAGE','FILE','SYSTEM','QUOTE'));
