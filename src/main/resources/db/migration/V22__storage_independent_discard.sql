-- Preserve existing tombstones while separating lifecycle from moderation.
ALTER TABLE storage_media_control ADD COLUMN discarded BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE storage_media_control SET discarded=TRUE, state='VISIBLE' WHERE state='DISCARDED';
CREATE INDEX ix_storage_reference_usage ON storage_media_references(media_id,context,consumer_id,role,id);
CREATE INDEX ix_storage_moderation_scope ON storage_governance_events(media_id,operation,reference_id,id);
