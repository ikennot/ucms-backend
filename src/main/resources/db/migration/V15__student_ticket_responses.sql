-- Allow students to post responses: make admin_id nullable, add student_id
ALTER TABLE ticket_response
    ALTER COLUMN admin_id DROP NOT NULL;

ALTER TABLE ticket_response
    ADD COLUMN student_id UUID REFERENCES profile(auth_user_id),
    ADD COLUMN responder_role VARCHAR(10) NOT NULL DEFAULT 'ADMIN';

-- Backfill existing rows (all existing rows are admin responses)
UPDATE ticket_response SET responder_role = 'ADMIN' WHERE responder_role IS NULL;
