-- BACK-599 / F131-01. Existing invalid rows deliberately fail migration instead of being rewritten.
ALTER TABLE storage_media_variants
    ADD CONSTRAINT ck_storage_variant_code_uppercase CHECK (variant_code = UPPER(variant_code));
