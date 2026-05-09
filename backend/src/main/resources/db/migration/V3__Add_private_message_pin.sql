ALTER TABLE private_messages ADD COLUMN pinned_at TIMESTAMP;

CREATE INDEX idx_private_messages_chat_id_pinned_at ON private_messages(chat_id, pinned_at DESC);

UPDATE private_messages
   SET message_status = 1
 WHERE message_status IS NULL;
