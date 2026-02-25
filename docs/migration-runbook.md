# UCMS Migration Runbook

Run each script in order in Supabase Dashboard → SQL Editor → New query.
Wait for "Success" before running the next one.

---

## V1 — Profiles

```sql
CREATE TABLE profile (
    auth_user_id UUID PRIMARY KEY,
    student_id   VARCHAR(20)  NOT NULL UNIQUE,
    name         VARCHAR(255) NOT NULL,
    email        VARCHAR(255),
    email_verified BOOLEAN    NOT NULL DEFAULT FALSE,
    course       VARCHAR(255),
    year_level   INTEGER,
    role         VARCHAR(10)  NOT NULL DEFAULT 'STUDENT',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

## V2 — Categories & Seed Data

```sql
CREATE TABLE category (
    id   BIGSERIAL    PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Seed categories
INSERT INTO category (name) VALUES
    ('Academic-related'),
    ('Grades'),
    ('AIMS Problem'),
    ('ID Concern'),
    ('Facility Issue'),
    ('Others');
```

---

## V3 — Tickets

```sql
CREATE TABLE ticket (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             UUID         NOT NULL REFERENCES profile(auth_user_id),
    category_id         BIGINT       NOT NULL REFERENCES category(id),
    ticket_number       VARCHAR(50)  NOT NULL UNIQUE,
    title               VARCHAR(255) NOT NULL,
    description         TEXT         NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    confirmed_resolved  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

## V4 — Ticket Responses & Attachments

```sql
CREATE TABLE ticket_response (
    id         BIGSERIAL PRIMARY KEY,
    ticket_id  BIGINT    NOT NULL REFERENCES ticket(id),
    admin_id   UUID      NOT NULL REFERENCES profile(auth_user_id),
    message    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE ticket_attachment (
    id                BIGSERIAL    PRIMARY KEY,
    ticket_id         BIGINT       NOT NULL REFERENCES ticket(id),
    storage_path      VARCHAR(500) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    uploaded_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

## V5 — Notifications

```sql
CREATE TABLE notification (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES profile(auth_user_id),
    ticket_id  BIGINT       NOT NULL REFERENCES ticket(id),
    message    TEXT         NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

## After all scripts succeed

Inform the project lead so Flyway baseline can be configured.

---

## Team Setup (Shared Supabase Project)

> The database is already set up on the shared Supabase project — teammates do NOT need to run any migrations. The scripts in this file are for reference and for deployment only.

### Prerequisites

**Java 21 (required for backend)**
```bash
sudo apt install openjdk-21-jdk
```
Verify:
```bash
java -version
```
Expected output: `openjdk 21.x.x`

**Set JAVA_HOME**
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```
To make it permanent, add the line above to your `~/.bashrc` or `~/.zshrc`.

---

### Backend Setup

1. Accept the Supabase invite sent to your email
2. Log in to [supabase.com](https://supabase.com) and open the **ucms** project
3. Go to **Settings → API** to get:
   - Project URL
   - Anon key
   - Service role key (**keep this private — backend only**)
4. Go to **Settings → Database → Connect → Connection pooling** to get:
   - Pooler connection string (use **Transaction mode, port 6543**)
   - Note: the username format is `postgres.<project-ref>`
5. Clone the repo and go to the backend directory:
   ```bash
   cd ucms-backend
   ```
6. Make the Maven wrapper executable:
   ```bash
   chmod +x mvnw
   ```
7. Copy the config template:
   ```bash
   cp src/main/resources/application.yaml.example src/main/resources/application.yaml
   ```
8. Fill in your `application.yaml` with the values from steps 3–4
9. Run the backend:
   ```bash
   ./mvnw spring-boot:run
   ```
10. Verify successful startup — look for these lines in the output:
    ```
    HikariPool-1 - Added connection
    HikariPool-1 - Start completed
    Tomcat started on port 8080
    Started UcmsBackendApplication
    ```
    If you see all four — backend is running and DB is connected. ✓

    You can also verify in Supabase Dashboard → **Table Editor** — you should see these tables:
    - profile
    - category
    - ticket
    - ticket_response
    - ticket_attachment
    - notification

> ⚠️ Never commit `application.yaml` — it is gitignored.
> ⚠️ Never share the service role key publicly.

---

### Android Setup

1. Open Android Studio
2. Open the `ucmsandroid/` folder as a project
3. Wait for Gradle sync to complete
4. Connect a device or start an emulator (min SDK 24 / Android 7.0)
5. Click **Run** or press `Shift+F10`

> ⚠️ The app will talk to the backend at `http://10.0.2.2:8080` (emulator) or your machine's local IP (physical device). Make sure the backend is running before launching the app.

---

## Patch — Remove Department (run if you already applied the old V2/V3)

Run this in Supabase Dashboard → SQL Editor if you previously ran the old migrations that included the `department` table and `department_id` column.

**Step 1 — Drop department table and column:**
```sql
DROP TABLE IF EXISTS department CASCADE;
ALTER TABLE ticket DROP COLUMN IF EXISTS department_id;
```

**Step 2 — Reset and reseed categories:**
```sql
DELETE FROM category;
INSERT INTO category (name) VALUES
    ('Academic-related'),
    ('Grades'),
    ('AIMS Problem'),
    ('ID Concern'),
    ('Facility Issue'),
    ('Others');
```

