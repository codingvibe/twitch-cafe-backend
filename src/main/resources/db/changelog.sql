-- liquibase formatted sql

-- changeset liquibase:1
CREATE TABLE users (id SERIAL PRIMARY KEY, twitch_id VARCHAR UNIQUE, config TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP)

-- changeset liquibase:2
ALTER TABLE users ADD access_token TEXT;

CREATE INDEX access_token_idx ON users (access_token);

-- changeset liquibase:3
CREATE TABLE prefs (id SERIAL PRIMARY KEY, name VARCHAR UNIQUE, valid_values TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP);

INSERT INTO prefs (name, valid_values) VALUES ('DrinkOrder', '[]');
INSERT INTO prefs (name, valid_values) VALUES ('LurkAnimation', '["customer.png"]');
INSERT INTO prefs (name, valid_values) VALUES ('FirstMessageAnimation', '["customer.png"]');
