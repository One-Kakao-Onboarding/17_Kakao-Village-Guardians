-- Connect to spring_boot_demo database
\c spring_boot_demo

-- Create application schema
CREATE SCHEMA IF NOT EXISTS app_schema AUTHORIZATION spring_user;

-- Set search path to use app_schema by default
ALTER USER spring_user SET search_path TO app_schema, public;

-- Grant all privileges on schema to spring_user
GRANT ALL PRIVILEGES ON SCHEMA app_schema TO spring_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA app_schema TO spring_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA app_schema TO spring_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA app_schema GRANT ALL ON TABLES TO spring_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA app_schema GRANT ALL ON SEQUENCES TO spring_user;

-- Create users table in app_schema
CREATE TABLE IF NOT EXISTS app_schema.users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create messages table in app_schema
CREATE TABLE IF NOT EXISTS app_schema.messages (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON app_schema.messages(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_email ON app_schema.users(email);

-- Insert sample data for testing
INSERT INTO app_schema.users (name, email) VALUES
    ('John Doe', 'john@example.com'),
    ('Jane Smith', 'jane@example.com'),
    ('Alice Johnson', 'alice@example.com')
ON CONFLICT (email) DO NOTHING;

INSERT INTO app_schema.messages (username, content) VALUES
    ('John Doe', 'Hello, this is my first message!'),
    ('Jane Smith', 'Welcome to the chat!'),
    ('John Doe', 'Thanks! Great to be here.'),
    ('Alice Johnson', 'Excited to join this community!')
ON CONFLICT DO NOTHING;

-- Display created tables
\dt app_schema.*

-- Display sample data
SELECT 'Users table:' as info;
SELECT * FROM app_schema.users;

SELECT 'Messages table:' as info;
SELECT * FROM app_schema.messages ORDER BY created_at;
