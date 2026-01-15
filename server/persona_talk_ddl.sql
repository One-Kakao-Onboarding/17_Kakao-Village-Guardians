-- ========================================
-- Persona Talk API - Complete DDL
-- Schema: hackerton
-- ========================================

-- Drop existing tables (cascade to handle foreign keys)
DROP TABLE IF EXISTS hackerton.reactions CASCADE;
DROP TABLE IF EXISTS hackerton.messages CASCADE;
DROP TABLE IF EXISTS hackerton.chat_room_members CASCADE;
DROP TABLE IF EXISTS hackerton.chat_rooms CASCADE;
DROP TABLE IF EXISTS hackerton.profiles CASCADE;
DROP TABLE IF EXISTS hackerton.emoticons CASCADE;
DROP TABLE IF EXISTS hackerton.users CASCADE;

-- ========================================
-- Table: users
-- Description: 사용자 정보 테이블 (LDAP 기반 인증)
-- ========================================
CREATE TABLE hackerton.users (
    id BIGSERIAL PRIMARY KEY,
    ldap VARCHAR(100) NOT NULL UNIQUE,           -- LDAP 아이디 (X-LDAP 헤더에서 가져옴)
    name VARCHAR(100) NOT NULL,                   -- 사용자 이름
    avatar VARCHAR(255),                          -- 프로필 이미지 URL
    CONSTRAINT uk_users_ldap UNIQUE (ldap)
);

CREATE INDEX idx_users_ldap ON hackerton.users(ldap);

COMMENT ON TABLE hackerton.users IS '사용자 정보';
COMMENT ON COLUMN hackerton.users.ldap IS 'LDAP 아이디 (고유값)';
COMMENT ON COLUMN hackerton.users.name IS '사용자 이름';
COMMENT ON COLUMN hackerton.users.avatar IS '프로필 이미지 URL';

-- ========================================
-- Table: chat_rooms
-- Description: 채팅방 테이블 (1:1 또는 그룹)
-- ========================================
CREATE TABLE hackerton.chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                   -- 채팅방 이름
    avatar VARCHAR(255),                          -- 채팅방 이미지 URL
    is_group BOOLEAN NOT NULL DEFAULT false,      -- 그룹 채팅 여부
    formality_level VARCHAR(50),                  -- 격식 수준 (formal, informal, casual)
    relationship VARCHAR(50),                     -- 관계 (friend, colleague, family 등)
    keywords TEXT,                                -- 키워드 배열 (JSON 형식: ["keyword1", "keyword2"])
    CONSTRAINT chk_formality_level CHECK (formality_level IN ('formal', 'informal', 'casual') OR formality_level IS NULL)
);

COMMENT ON TABLE hackerton.chat_rooms IS '채팅방 정보';
COMMENT ON COLUMN hackerton.chat_rooms.is_group IS '그룹 채팅 여부 (true: 그룹, false: 1:1)';
COMMENT ON COLUMN hackerton.chat_rooms.formality_level IS '격식 수준 (AI 변환에 사용)';
COMMENT ON COLUMN hackerton.chat_rooms.relationship IS '관계 유형 (AI 변환에 사용)';
COMMENT ON COLUMN hackerton.chat_rooms.keywords IS 'JSON 배열 형태의 키워드';

-- ========================================
-- Table: chat_room_members
-- Description: 채팅방-사용자 조인 테이블 (멤버십 + 읽음 상태)
-- ========================================
CREATE TABLE hackerton.chat_room_members (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,                 -- 채팅방 ID
    user_id BIGINT NOT NULL,                      -- 사용자 ID
    last_read_message_id BIGINT,                  -- 마지막 읽은 메시지 ID (안읽음 계산용)
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 참여 시간
    CONSTRAINT fk_chat_room_members_room FOREIGN KEY (chat_room_id)
        REFERENCES hackerton.chat_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_room_members_user FOREIGN KEY (user_id)
        REFERENCES hackerton.users(id) ON DELETE CASCADE,
    CONSTRAINT uk_chat_room_members_room_user UNIQUE (chat_room_id, user_id)
);

CREATE INDEX idx_chat_room_members_user ON hackerton.chat_room_members(user_id);
CREATE INDEX idx_chat_room_members_room ON hackerton.chat_room_members(chat_room_id);

COMMENT ON TABLE hackerton.chat_room_members IS '채팅방 멤버 정보 (조인 테이블)';
COMMENT ON COLUMN hackerton.chat_room_members.last_read_message_id IS '마지막 읽은 메시지 ID (unread count 계산에 사용)';

-- ========================================
-- Table: messages
-- Description: 메시지 테이블 (AI 변환 정보 포함)
-- ========================================
CREATE TABLE hackerton.messages (
    id BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT NOT NULL,                 -- 채팅방 ID
    sender_id BIGINT NOT NULL,                    -- 발신자 ID
    content TEXT NOT NULL,                        -- 최종 메시지 내용 (AI 변환 후)
    original_content TEXT,                        -- 원본 메시지 내용 (AI 변환 전)
    was_guarded BOOLEAN NOT NULL DEFAULT false,   -- Emotion Guard 적용 여부
    is_emoticon BOOLEAN NOT NULL DEFAULT false,   -- 이모티콘 메시지 여부
    emoticon_id BIGINT,                           -- 이모티콘 ID (is_emoticon=true인 경우)
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 메시지 생성 시간
    CONSTRAINT fk_messages_chat_room FOREIGN KEY (chat_room_id)
        REFERENCES hackerton.chat_rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id)
        REFERENCES hackerton.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_chat_room ON hackerton.messages(chat_room_id);
CREATE INDEX idx_messages_sender ON hackerton.messages(sender_id);
CREATE INDEX idx_messages_timestamp ON hackerton.messages(timestamp);

COMMENT ON TABLE hackerton.messages IS '메시지 정보 (AI 변환 지원)';
COMMENT ON COLUMN hackerton.messages.content IS 'AI 변환 후 최종 메시지';
COMMENT ON COLUMN hackerton.messages.original_content IS 'AI 변환 전 원본 메시지';
COMMENT ON COLUMN hackerton.messages.was_guarded IS 'Emotion Guard 적용 여부';

-- ========================================
-- Table: reactions
-- Description: 메시지 반응 테이블 (이모지)
-- ========================================
CREATE TABLE hackerton.reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,                   -- 메시지 ID
    user_id BIGINT NOT NULL,                      -- 반응한 사용자 ID
    emoji VARCHAR(10) NOT NULL,                   -- 이모지 (예: 👍, ❤️)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 반응 생성 시간
    CONSTRAINT fk_reactions_message FOREIGN KEY (message_id)
        REFERENCES hackerton.messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id)
        REFERENCES hackerton.users(id) ON DELETE CASCADE,
    CONSTRAINT uk_reactions_message_user_emoji UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX idx_reactions_message ON hackerton.reactions(message_id);
CREATE INDEX idx_reactions_user ON hackerton.reactions(user_id);

COMMENT ON TABLE hackerton.reactions IS '메시지 반응 (이모지)';
COMMENT ON COLUMN hackerton.reactions.emoji IS '이모지 문자 (UTF-8)';

-- ========================================
-- Table: profiles
-- Description: 사용자 프로필 테이블 (다중 페르소나 지원)
-- ========================================
CREATE TABLE hackerton.profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,                      -- 사용자 ID
    name VARCHAR(100) NOT NULL,                   -- 프로필 이름
    avatar VARCHAR(255),                          -- 프로필 이미지 URL
    description TEXT,                             -- 프로필 설명
    default_persona VARCHAR(100),                 -- 기본 페르소나 (formal, casual 등)
    linked_chat_room_ids TEXT,                    -- 연결된 채팅방 ID 배열 (JSON: [1, 2, 3])
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
        REFERENCES hackerton.users(id) ON DELETE CASCADE
);

CREATE INDEX idx_profiles_user ON hackerton.profiles(user_id);

COMMENT ON TABLE hackerton.profiles IS '사용자 프로필 (다중 페르소나)';
COMMENT ON COLUMN hackerton.profiles.default_persona IS '기본 페르소나 설정';
COMMENT ON COLUMN hackerton.profiles.linked_chat_room_ids IS 'JSON 배열: [1, 2, 3]';

-- ========================================
-- Table: emoticons
-- Description: 이모티콘 테이블 (스티커 같은 큰 이모티콘)
-- ========================================
CREATE TABLE hackerton.emoticons (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,                   -- 이모티콘 이름
    image_url VARCHAR(500) NOT NULL,              -- 이미지 URL
    category VARCHAR(50),                         -- 카테고리 (emotion, gesture, celebration 등)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP  -- 생성 시간
);

CREATE INDEX idx_emoticons_category ON hackerton.emoticons(category);

COMMENT ON TABLE hackerton.emoticons IS '이모티콘 정보';
COMMENT ON COLUMN hackerton.emoticons.category IS '카테고리 (emotion, gesture, celebration 등)';

-- ========================================
-- Sample Data (for testing)
-- ========================================

-- Insert sample users
INSERT INTO hackerton.users (ldap, name, avatar) VALUES
('user001', 'Alice Kim', 'https://i.pravatar.cc/150?img=1'),
('user002', 'Bob Lee', 'https://i.pravatar.cc/150?img=2'),
('user003', 'Charlie Park', 'https://i.pravatar.cc/150?img=3'),
('user004', 'Diana Choi', 'https://i.pravatar.cc/150?img=4'),
('user005', 'Eric Jung', 'https://i.pravatar.cc/150?img=5');

-- Insert sample chat rooms
INSERT INTO hackerton.chat_rooms (name, avatar, is_group, formality_level, relationship, keywords) VALUES
('Team Project', 'https://i.pravatar.cc/150?img=10', true, 'formal', 'colleague', '["work", "project", "deadline"]'),
('Friends Group', 'https://i.pravatar.cc/150?img=11', true, 'casual', 'friend', '["fun", "hangout", "weekend"]'),
('1:1 with Bob', NULL, false, 'informal', 'friend', '["casual", "chat"]');

-- Insert chat room members
INSERT INTO hackerton.chat_room_members (chat_room_id, user_id, last_read_message_id, joined_at) VALUES
(1, 1, NULL, '2025-01-10 10:00:00'),
(1, 2, NULL, '2025-01-10 10:00:00'),
(1, 3, NULL, '2025-01-10 10:00:00'),
(2, 1, NULL, '2025-01-12 14:00:00'),
(2, 4, NULL, '2025-01-12 14:00:00'),
(2, 5, NULL, '2025-01-12 14:00:00'),
(3, 1, NULL, '2025-01-13 09:00:00'),
(3, 2, NULL, '2025-01-13 09:00:00');

-- Insert sample messages
INSERT INTO hackerton.messages (chat_room_id, sender_id, content, original_content, was_guarded, is_emoticon, timestamp) VALUES
(1, 1, '안녕하세요, 프로젝트 진행 상황을 공유드립니다.', '안녕하세요, 프로젝트 진행 상황을 공유드립니다.', false, false, '2025-01-10 10:05:00'),
(1, 2, '좋습니다. 현재 진행 중인 작업에 대해 말씀해주시겠습니까?', '좋아요. 지금 뭐 하고 있어요?', true, false, '2025-01-10 10:06:00'),
(1, 3, '저는 데이터베이스 스키마 설계를 완료했습니다.', '저는 데이터베이스 스키마 설계를 완료했습니다.', false, false, '2025-01-10 10:07:00'),
(2, 1, '이번 주말에 시간 있으신가요?', '이번 주말에 시간 있으신가요?', false, false, '2025-01-12 14:05:00'),
(2, 4, '네, 시간 있어요! 무슨 계획이 있으신가요?', '네, 시간 있어요! 무슨 계획이 있으신가요?', false, false, '2025-01-12 14:06:00'),
(3, 1, '안녕! 오늘 점심 뭐 먹을까?', '안녕! 오늘 점심 뭐 먹을까?', false, false, '2025-01-13 09:05:00'),
(3, 2, '나는 김치찌개 먹고 싶은데 어때?', '나는 김치찌개 먹고 싶은데 어때?', false, false, '2025-01-13 09:06:00');

-- Insert sample reactions
INSERT INTO hackerton.reactions (message_id, user_id, emoji, created_at) VALUES
(1, 2, '👍', '2025-01-10 10:06:00'),
(1, 3, '❤️', '2025-01-10 10:07:00'),
(3, 1, '🎉', '2025-01-10 10:08:00'),
(4, 4, '👋', '2025-01-12 14:06:00'),
(6, 2, '🍜', '2025-01-13 09:07:00');

-- Insert sample profiles
INSERT INTO hackerton.profiles (user_id, name, avatar, description, default_persona, linked_chat_room_ids) VALUES
(1, 'Professional Alice', 'https://i.pravatar.cc/150?img=21', 'Work profile for formal communications', 'formal', '[1]'),
(1, 'Casual Alice', 'https://i.pravatar.cc/150?img=22', 'Casual profile for friends', 'casual', '[2, 3]'),
(2, 'Bob Work', 'https://i.pravatar.cc/150?img=23', 'Professional profile', 'professional', '[1]');

-- Insert sample emoticons
INSERT INTO hackerton.emoticons (name, image_url, category) VALUES
('Happy Face', 'https://cdn.example.com/emoticons/happy.png', 'emotion'),
('Thumbs Up', 'https://cdn.example.com/emoticons/thumbs-up.png', 'gesture'),
('Heart', 'https://cdn.example.com/emoticons/heart.png', 'emotion'),
('Thinking', 'https://cdn.example.com/emoticons/thinking.png', 'emotion'),
('Party', 'https://cdn.example.com/emoticons/party.png', 'celebration'),
('Sad Face', 'https://cdn.example.com/emoticons/sad.png', 'emotion'),
('Fire', 'https://cdn.example.com/emoticons/fire.png', 'symbol'),
('Star', 'https://cdn.example.com/emoticons/star.png', 'symbol');

-- ========================================
-- Verification Queries
-- ========================================

-- List all tables
-- \dt hackerton.*

-- Count records in each table
-- SELECT 'users' as table_name, COUNT(*) as count FROM hackerton.users
-- UNION ALL SELECT 'chat_rooms', COUNT(*) FROM hackerton.chat_rooms
-- UNION ALL SELECT 'chat_room_members', COUNT(*) FROM hackerton.chat_room_members
-- UNION ALL SELECT 'messages', COUNT(*) FROM hackerton.messages
-- UNION ALL SELECT 'reactions', COUNT(*) FROM hackerton.reactions
-- UNION ALL SELECT 'profiles', COUNT(*) FROM hackerton.profiles
-- UNION ALL SELECT 'emoticons', COUNT(*) FROM hackerton.emoticons;
