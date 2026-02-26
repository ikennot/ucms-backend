/*
  Flyway migration: RLS policies for Supabase tables
  Scope: authenticated role policies for UCMS backend access patterns
  Notes: admin checks use profile.role and auth.uid()
*/

-- profile
CREATE POLICY profile_select_own
  ON profile
  FOR SELECT
  TO authenticated
  USING (auth_user_id = auth.uid());

CREATE POLICY profile_update_own
  ON profile
  FOR UPDATE
  TO authenticated
  USING (auth_user_id = auth.uid());

-- ticket
CREATE POLICY ticket_select_student
  ON ticket
  FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY ticket_select_admin
  ON ticket
  FOR SELECT
  TO authenticated
  USING ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

CREATE POLICY ticket_insert_student
  ON ticket
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = auth.uid());

CREATE POLICY ticket_update_admin
  ON ticket
  FOR UPDATE
  TO authenticated
  USING ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

-- ticket_response
CREATE POLICY ticket_response_select_student
  ON ticket_response
  FOR SELECT
  TO authenticated
  USING (ticket_id IN (SELECT id FROM ticket WHERE user_id = auth.uid()));

CREATE POLICY ticket_response_select_admin
  ON ticket_response
  FOR SELECT
  TO authenticated
  USING ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

CREATE POLICY ticket_response_insert_admin
  ON ticket_response
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

-- ticket_attachment
CREATE POLICY ticket_attachment_select_student
  ON ticket_attachment
  FOR SELECT
  TO authenticated
  USING (ticket_id IN (SELECT id FROM ticket WHERE user_id = auth.uid()));

CREATE POLICY ticket_attachment_select_admin
  ON ticket_attachment
  FOR SELECT
  TO authenticated
  USING ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

CREATE POLICY ticket_attachment_insert_student
  ON ticket_attachment
  FOR INSERT
  TO authenticated
  WITH CHECK (ticket_id IN (SELECT id FROM ticket WHERE user_id = auth.uid()));

-- notification
CREATE POLICY notification_select_own
  ON notification
  FOR SELECT
  TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY notification_update_own
  ON notification
  FOR UPDATE
  TO authenticated
  USING (user_id = auth.uid());

-- category
CREATE POLICY category_select_authenticated
  ON category
  FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY category_insert_admin
  ON category
  FOR INSERT
  TO authenticated
  WITH CHECK ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

CREATE POLICY category_update_admin
  ON category
  FOR UPDATE
  TO authenticated
  USING ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');

CREATE POLICY category_delete_admin
  ON category
  FOR DELETE
  TO authenticated
  USING ((SELECT role FROM profile WHERE auth_user_id = auth.uid()) = 'ADMIN');
