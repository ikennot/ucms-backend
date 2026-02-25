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
