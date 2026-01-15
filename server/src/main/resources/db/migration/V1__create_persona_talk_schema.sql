-- Drop existing tables if any
DROP TABLE IF EXISTS hackerton.reactions CASCADE;
DROP TABLE IF EXISTS hackerton.messages CASCADE;
DROP TABLE IF EXISTS hackerton.chat_room_members CASCADE;
DROP TABLE IF EXISTS hackerton.chat_rooms CASCADE;
DROP TABLE IF EXISTS hackerton.profiles CASCADE;
DROP TABLE IF EXISTS hackerton.emoticons CASCADE;
DROP TABLE IF EXISTS hackerton.users CASCADE;

-- Create users table
CREATE TABLE hackerton.users (
    id BIGSERIAL PRIMARY KEY,
    ldap VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255)
);

CREATE INDEX idx_users_ldap ON hackerton.users(ldap);

-- Create chat_rooms table
CREATE TABLE hackerton.chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    is_group BOOLEAN NOT NULL DEFAULT false,
    formality_level VARCHAR(50),
    relationship VARCHAR(50),
    keywords TEXT
);

-- Create chat_room_members table (join table)
CREATE TABLE hackerton.chat_room_members (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES hackerton.chat_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES hackerton.users(id) ON DELETE CASCADE,
    last_read_message_id BIGINT,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(chat_room_id, user_id)
);

CREATE INDEX idx_chat_room_members_user ON hackerton.chat_room_members(user_id);
CREATE INDEX idx_chat_room_members_room ON hackerton.chat_room_members(chat_room_id);

-- Create messages table
CREATE TABLE hackerton.messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL REFERENCES hackerton.chat_rooms(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES hackerton.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    original_content TEXT,
    was_guarded BOOLEAN NOT NULL DEFAULT false,
    is_emoticon BOOLEAN NOT NULL DEFAULT false,
    emoticon_id BIGINT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_chat_room ON hackerton.messages(chat_room_id);
CREATE INDEX idx_messages_sender ON hackerton.messages(sender_id);
CREATE INDEX idx_messages_timestamp ON hackerton.messages(timestamp);

-- Create reactions table
CREATE TABLE hackerton.reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL REFERENCES hackerton.messages(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES hackerton.users(id) ON DELETE CASCADE,
    emoji VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(message_id, user_id, emoji)
);

CREATE INDEX idx_reactions_message ON hackerton.reactions(message_id);
CREATE INDEX idx_reactions_user ON hackerton.reactions(user_id);

-- Create profiles table
CREATE TABLE hackerton.profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES hackerton.users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    avatar VARCHAR(255),
    description TEXT,
    default_persona VARCHAR(100),
    linked_chat_room_ids TEXT
);

CREATE INDEX idx_profiles_user ON hackerton.profiles(user_id);

-- Create emoticons table
CREATE TABLE hackerton.emoticons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_emoticons_category ON hackerton.emoticons(category);
