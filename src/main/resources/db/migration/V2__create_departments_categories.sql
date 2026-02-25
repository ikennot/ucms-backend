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
