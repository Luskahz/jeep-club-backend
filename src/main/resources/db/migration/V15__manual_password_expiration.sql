-- Legacy accounts intentionally remain NULL: password hashes cannot distinguish
-- previously delivered manual passwords from strong invitation credentials.
ALTER TABLE authentication_accounts ADD COLUMN manual_password_expires_at DATETIME(6) NULL;
