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
