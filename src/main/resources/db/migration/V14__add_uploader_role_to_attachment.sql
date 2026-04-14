            ALTER TABLE ticket_attachment
                ADD COLUMN IF NOT EXISTS uploader_role VARCHAR(20);
