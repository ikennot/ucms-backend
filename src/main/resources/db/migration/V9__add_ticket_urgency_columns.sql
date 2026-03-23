ALTER TABLE ticket
    ADD COLUMN urgency_score INT,
    ADD COLUMN urgency_label VARCHAR(20),
    ADD COLUMN urgency_reason VARCHAR(500),
    ADD COLUMN urgency_confidence DOUBLE PRECISION,
    ADD COLUMN urgency_updated_at TIMESTAMP;
