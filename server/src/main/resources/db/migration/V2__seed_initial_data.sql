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
