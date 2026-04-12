ALTER TABLE ticket
    ADD COLUMN assigned_admin_id UUID REFERENCES profile(auth_user_id);
