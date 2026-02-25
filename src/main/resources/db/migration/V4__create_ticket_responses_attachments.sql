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
