-- ============================================================
-- V8__add_conversation_message_type.sql
-- Adds 'type' column to conversation_messages for structured system events
-- ============================================================

ALTER TABLE conversation_messages
    ADD COLUMN type VARCHAR(40) NOT NULL DEFAULT 'TEXT';
