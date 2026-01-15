# Persona Talk API 명세서

## 개요

이 문서는 Persona Talk 애플리케이션의 Spring Boot 백엔드 서버와 연동하기 위한 API 명세입니다.

- **Base URL**: `${NEXT_PUBLIC_API_BASE_URL}` (환경변수로 설정)
- **Content-Type**: `application/json`
- **인증 방식**: LDAP 헤더 기반 인증 (매 요청마다 `X-LDAP` 헤더로 사용자 식별)

**공통 Request Headers**
```
X-LDAP: {user_ldap}
```

---

## 1. 사용자 정보 API

### 1.1 현재 사용자 정보 조회

LDAP을 통해 식별된 사용자 정보 조회

```
GET /api/v1/users/me
```

**Request Headers**
```
X-LDAP: {user_ldap}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "ldap": "string",
    "name": "string",
    "avatar": "string"
  }
}
```

### 1.2 사용자 정보 수정

사용자 이름 및 프로필 사진 업데이트

```
PUT /api/v1/users/me
```

**Request Headers**
```
X-LDAP: {user_ldap}
Content-Type: application/json
```

**Request Body**
```json
{
  "name": "string",
  "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**설명**
- `name`: 사용자 이름 (선택)
- `avatar`: base64로 인코딩된 이미지 데이터 URL 형식 (선택)
- 지원 형식: PNG, JPEG, GIF
- 최대 크기: 5MB 권장
- 필드는 선택적이며, 전달된 필드만 업데이트됨

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "ldap": "string",
    "name": "string",
    "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
  }
}
```

**Error Response (400 Bad Request)**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_IMAGE_FORMAT",
    "message": "지원하지 않는 이미지 형식입니다.",
    "details": {
      "supportedFormats": ["png", "jpeg", "jpg", "gif"]
    }
  }
}
```

---

## 2. 채팅방 API

### 2.1 채팅방 목록 조회

```
GET /api/v1/chatrooms?profileId={profileId}
```

**Request Headers**
```
X-LDAP: {user_ldap}
```

**Query Parameters**
- `profileId` (optional): 특정 프로필에 연결된 채팅방만 필터링합니다.
  - 제공되지 않거나 `profileId=all`이면 모든 채팅방을 반환
  - 특정 프로필 ID가 제공되면 해당 프로필의 `assignedFriends`에 포함된 채팅방만 반환

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": "string",
      "name": "string",
      "avatar": "string",
      "lastMessage": "string",
      "lastMessageTime": "2026-01-15T12:00:00Z",
      "unreadCount": 3,  // IMPORTANT: 요청한 사용자(X-LDAP)가 읽지 않은 메시지 수. 사용자가 보낸 메시지는 카운트하지 않음
      "formalityLevel": 85,
      "relationship": "boss",
      "isGroup": false,
      "keywords": ["보고", "회의", "검토"]
    }
  ]
}
```

**relationship 타입**
- `boss`: 상사
- `senior`: 선배
- `colleague`: 동료
- `friend`: 친구
- `family`: 가족

**unreadCount 계산 로직**

`unreadCount`는 **요청한 사용자(X-LDAP 헤더)가 받았지만 아직 읽지 않은 메시지의 개수**입니다.

중요한 규칙:
1. **수신자 관점**: 사용자 A가 GET /api/v1/chatrooms를 호출하면, 각 채팅방의 `unreadCount`는 "사용자 A가 받았지만 읽지 않은 메시지 수"를 나타냅니다.
2. **발신자는 0**: 사용자 A가 메시지를 보낸 후, 사용자 A의 화면에서는 `unreadCount: 0`이어야 합니다. 상대방이 읽지 않았더라도 발신자에게는 unread count가 표시되지 않습니다.
3. **읽음 처리**: POST /api/v1/chatrooms/{roomId}/read를 호출하면 해당 채팅방의 unreadCount가 0으로 리셋됩니다.

예시:
- 사용자 A(kai.0109)가 사용자 B에게 메시지 전송 → 사용자 A의 GET /api/v1/chatrooms 응답에서 해당 채팅방의 `unreadCount: 0`
- 사용자 B가 GET /api/v1/chatrooms 호출 → 사용자 B의 응답에서 해당 채팅방의 `unreadCount: 1`
- 사용자 B가 채팅방 열고 POST /api/v1/chatrooms/{roomId}/read 호출 → 사용자 B의 다음 GET 요청에서 `unreadCount: 0`

### 2.2 채팅방 상세 조회

```
GET /api/v1/chatrooms/{roomId}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "name": "string",
    "avatar": "string",
    "formalityLevel": 85,
    "relationship": "boss",
    "isGroup": false,
    "keywords": ["보고", "회의", "검토"],
    "members": [
      {
        "id": "string",
        "name": "string",
        "avatar": "string"
      }
    ]
  }
}
```

### 2.3 친구 추가 (채팅방 생성)

친구의 LDAP을 입력하여 1:1 채팅방 생성

```
POST /api/v1/chatrooms
```

**Request Headers**
```
X-LDAP: {user_ldap}
```

**Request Body**
```json
{
  "friendLdap": "string",
  "formalityLevel": 85,
  "relationship": "colleague",
  "profileId": "profile-123"
}
```

**설명**
- `friendLdap`: 친구의 LDAP ID (필수)
- `formalityLevel`: 이 친구와의 대화 격식 수준 0-100 (필수)
- `relationship`: 관계 타입 (필수) - `boss`, `senior`, `colleague`, `friend`, `family`
- `profileId`: 이 채팅방을 연결할 프로필 ID (선택)
  - 제공되면 해당 프로필의 `assignedFriends`에 자동으로 추가됨
  - DB에 프로필-채팅방 매핑이 저장됨

**Response (201 Created)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "name": "김철수",
    "avatar": "string",
    "lastMessage": null,
    "lastMessageTime": null,
    "unreadCount": 0,
    "formalityLevel": 85,
    "relationship": "colleague",
    "isGroup": false,
    "keywords": []
  }
}
```

### 2.4 채팅방 읽음 처리

```
PUT /api/v1/chatrooms/{roomId}/read
```

**Response (200 OK)**
```json
{
  "success": true,
  "message": "읽음 처리 완료"
}
```

### 2.5 채팅방 삭제

```
DELETE /api/v1/chatrooms/{roomId}
```

**Request Headers**
```
X-LDAP: {user_ldap}
```

**Response (200 OK)**
```json
{
  "success": true,
  "message": "채팅방이 삭제되었습니다."
}
```

**Error Response (404 Not Found)**
```json
{
  "success": false,
  "error": {
    "code": "CHATROOM_NOT_FOUND",
    "message": "채팅방을 찾을 수 없습니다."
  }
}
```

---

## 3. 메시지 API

### 3.1 메시지 목록 조회

```
GET /api/v1/chatrooms/{roomId}/messages
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": "string",
      "content": "string",
      "originalContent": "string",
      "sender": "me",
      "senderName": "string",
      "senderAvatar": "string",
      "timestamp": "2026-01-15T12:00:00Z",
      "reactions": ["👍", "❤️"],
      "wasGuarded": false,
      "isEmoticon": false,
      "emoticonId": null,
      "isRead": false  // 상대방이 읽었는지 여부 (내가 보낸 메시지인 경우에만 의미 있음)
    }
  ]
}
```

**isRead 필드 설명**

`isRead` 필드는 메시지의 읽음 상태를 나타냅니다:

- **내가 보낸 메시지 (sender: "me")**: 상대방이 이 메시지를 읽었는지 여부
  - `false`: 상대방이 아직 읽지 않음 → UI에 "1" 표시
  - `true`: 상대방이 읽음 → UI에 읽음 표시 없음

- **상대방이 보낸 메시지 (sender: "other")**: 이 필드는 항상 `true` (내가 조회했으므로 읽은 것으로 간주)

읽음 상태 업데이트:
- 사용자가 채팅방을 열고 `POST /api/v1/chatrooms/{roomId}/read`를 호출하면, 해당 채팅방의 모든 메시지가 읽음 처리됩니다.
- 이후 상대방이 메시지 목록을 조회할 때 `isRead: true`로 반환됩니다.

### 3.2 메시지 전송

```
POST /api/v1/chatrooms/{roomId}/messages
```

**Request Body**
```json
{
  "content": "string",
  "originalContent": "string",
  "isEmoticon": false,
  "emoticonId": null
}
```

**Response (201 Created)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "content": "string",
    "originalContent": "string",
    "sender": "me",
    "timestamp": "2026-01-15T12:00:00Z",
    "isEmoticon": false,
    "emoticonId": null
  }
}
```

### 3.3 메시지 리액션 추가/제거

```
POST /api/v1/messages/{messageId}/reactions
```

**Request Body**
```json
{
  "emoji": "👍"
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "messageId": "string",
    "reactions": ["👍", "❤️"]
  }
}
```

---

## 4. AI 기능 API

### 4.1 말투 변환 (Transform Message)

입력된 메시지를 현재 페르소나/격식 수준에 맞게 변환

```
POST /api/v1/ai/transform
```

**Request Headers**
```
X-LDAP: {user_ldap}
Content-Type: application/json
```

**Request Body**
```json
{
  "text": "알았어",
  "personaId": "very-formal",
  "formalityLevel": 85,
  "relationship": "boss",
  "roomId": "chatroom-123"
}
```

**설명**
- `text`: 변환할 원본 메시지 (필수)
- `personaId`: 페르소나 ID (선택, 기본값: formalityLevel에 따라 자동 선택)
- `formalityLevel`: 격식 수준 0-100 (필수)
  - 0-20: 매우 친근 (반말, 이모티콘 사용)
  - 20-40: 친근 (편한 반말)
  - 40-60: 중립 (존댓말, 평어)
  - 60-80: 격식 (정중한 존댓말)
  - 80-100: 매우 격식 (업무용 존댓말, 경어)
- `relationship`: 관계 타입 (필수) - `boss`, `senior`, `colleague`, `friend`, `family`
- `roomId`: 채팅방 ID (선택, 컨텍스트 분석용)

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "originalText": "알았어",
    "transformedText": "네, 확인했습니다. 말씀하신 내용 반영하여 진행하겠습니다.",
    "formalityLevel": 85,
    "appliedPersona": "very-formal",
    "changes": [
      {
        "type": "tone",
        "description": "반말을 정중한 존댓말로 변경"
      },
      {
        "type": "detail",
        "description": "구체적인 응답으로 확장"
      }
    ],
    "shouldSuggest": true,
    "suggestionReason": "상사와의 대화에서 더 격식있는 표현이 적합합니다."
  }
}
```

**설명 (Response)**
- `originalText`: 원본 메시지
- `transformedText`: 변환된 메시지
- `formalityLevel`: 적용된 격식 수준
- `appliedPersona`: 적용된 페르소나 ID
- `changes`: 변경 사항 목록
  - `type`: 변경 타입 (`tone`, `detail`, `formality`, `vocabulary`)
  - `description`: 변경 설명
- `shouldSuggest`: 사용자에게 변환된 메시지 사용을 제안해야 하는지 여부
- `suggestionReason`: 제안 이유 (shouldSuggest가 true일 때)

**Error Response (400 Bad Request)**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_FORMALITY_LEVEL",
    "message": "격식 수준은 0-100 사이여야 합니다."
  }
}
```

### 4.2 감정 가드 분석 (Emotion Guard)

입력된 메시지의 공격성/비꼬기 감지

```
POST /api/v1/ai/emotion-guard
```

**Request Body**
```json
{
  "text": "참 잘하시네요"
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "isAggressive": true,
    "aggressionType": "sarcasm",
    "aggressionScore": 0.85,
    "suggestedText": "혹시 제가 부족한 부분이 있었을까요? 피드백 주시면 개선하겠습니다.",
    "warningMessage": "조금 더 부드럽게 말해볼까요?"
  }
}
```

**aggressionType 타입**
- `sarcasm`: 비꼬기
- `passive_aggressive`: 수동적 공격
- `direct_attack`: 직접적 공격
- `dismissive`: 무시/퇴짜

### 4.3 리액션 추천 (Reaction Suggestion)

상대방 메시지 감정 분석 및 리액션 추천

```
POST /api/v1/ai/reaction-suggest
```

**Request Body**
```json
{
  "message": "시험 떨어졌어...",
  "relationship": "friend",
  "formalityLevel": 10
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "emotion": "sad",
    "emotionScore": 0.9,
    "suggestedEmojis": ["😢", "🫂", "💪", "❤️"],
    "suggestedTexts": [
      {
        "text": "괜찮아, 다음에 잘하면 돼!",
        "type": "comfort"
      },
      {
        "text": "헐 ㅠㅠ 힘내...",
        "type": "empathy"
      }
    ],
    "quickResponses": [
      {
        "text": "무슨 일이야?",
        "icon": "❓"
      },
      {
        "text": "힘내! 응원할게",
        "icon": "💪"
      }
    ]
  }
}
```

**emotion 타입**
- `happy`: 기쁨
- `sad`: 슬픔
- `angry`: 화남
- `surprised`: 놀람
- `neutral`: 중립
- `excited`: 흥분
- `worried`: 걱정

### 4.4 AI 친구 매칭 추천

프로필에 어울리는 친구 추천

```
POST /api/v1/ai/friend-matching
```

**Request Body**
```json
{
  "profileName": "회사용",
  "personaId": "formal",
  "chatRoomIds": ["1", "2", "3", "4", "5"]
}
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "recommendations": [
      {
        "chatRoomId": "1",
        "chatRoomName": "김부장님",
        "matchScore": 95,
        "matchReason": "회사/업무 관련 프로필, 상사 관계"
      },
      {
        "chatRoomId": "2",
        "chatRoomName": "이선배",
        "matchScore": 85,
        "matchReason": "격식있는 말투, 선배 관계"
      }
    ]
  }
}
```

---

## 5. 프로필 API

### 5.1 프로필 목록 조회

```
GET /api/v1/profiles
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": "all",
      "name": "전체",
      "avatar": "string",
      "description": "모든 채팅방 보기",
      "defaultPersona": "casual-polite",
      "assignedFriends": [],
      "isDefault": true
    },
    {
      "id": "string",
      "name": "회사용",
      "avatar": "string",
      "description": "업무용 프로필",
      "defaultPersona": "formal",
      "assignedFriends": ["chatroom-1", "chatroom-2"],
      "isDefault": false
    }
  ]
}
```

**설명**
- 첫 번째 프로필은 항상 "전체" 프로필 (`id: "all"`, `isDefault: true`)
- "전체" 프로필은 `assignedFriends`가 빈 배열이며, 이 경우 모든 채팅방을 표시
- "전체" 프로필은 수정/삭제 불가
- `isDefault`: 시스템 기본 프로필 여부 (전체 프로필만 true)

### 5.2 프로필 생성

```
POST /api/v1/profiles
```

**Request Body**
```json
{
  "name": "회사용",
  "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "description": "업무용 프로필",
  "defaultPersona": "formal",
  "assignedFriends": ["chatroom-1", "chatroom-2"]
}
```

**설명**
- `name`: 프로필 이름 (필수)
- `avatar`: base64로 인코딩된 이미지 데이터 URL 형식 (선택)
  - avatar를 제공하지 않으면 기본 아바타 자동 생성
  - 지원 형식: PNG, JPEG, GIF
  - 최대 크기: 5MB 권장
- `description`: 프로필 설명 (필수)
- `defaultPersona`: 기본 페르소나 ID (필수)
- `assignedFriends`: 이 프로필에 할당된 채팅방 ID 목록 (선택)
  - 프로필에서 보고 싶은 채팅방들의 ID 배열
  - 빈 배열이면 해당 프로필에서는 채팅방이 보이지 않음
  - 이 프로필로 필터링할 때 해당 채팅방들만 표시됨

**Response (201 Created)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "name": "회사용",
    "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "description": "업무용 프로필",
    "defaultPersona": "formal",
    "assignedFriends": ["chatroom-1", "chatroom-2"]
  }
}
```

### 5.3 프로필 수정

```
PUT /api/v1/profiles/{profileId}
```

**Request Body**
```json
{
  "name": "회사용 (수정)",
  "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  "description": "업무용 프로필 수정",
  "defaultPersona": "very-formal",
  "assignedFriends": ["chatroom-1", "chatroom-3"]
}
```

**설명**
- 모든 필드는 선택적이며, 전달된 필드만 업데이트됨
- `avatar`: base64로 인코딩된 이미지 데이터 URL 형식 (선택)
  - 지원 형식: PNG, JPEG, GIF
  - 최대 크기: 5MB 권장
- `assignedFriends`: 이 프로필에 할당된 채팅방 ID 목록 (선택)
  - 프로필에 새 친구를 추가하거나 제거할 때 사용
  - 전체 배열을 덮어쓰므로, 추가/제거 시 현재 목록을 가져와서 수정 후 전송
  - 예: 기존 ["room-1", "room-2"]에서 "room-3" 추가 → ["room-1", "room-2", "room-3"] 전송

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "id": "string",
    "name": "회사용 (수정)",
    "avatar": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
    "description": "업무용 프로필 수정",
    "defaultPersona": "very-formal",
    "assignedFriends": ["chatroom-1", "chatroom-3"]
  }
}
```

### 5.4 프로필 삭제

```
DELETE /api/v1/profiles/{profileId}
```

**Response (200 OK)**
```json
{
  "success": true,
  "message": "프로필이 삭제되었습니다."
}
```

---

## 6. 데이터베이스 스키마 및 관계

### 6.1 프로필-채팅방 매핑 (Profile-ChatRoom Mapping)

멀티프로필 기능은 양방향 관계로 동작합니다:

**데이터 구조:**
```
User (사용자)
├── Profiles (여러 프로필 소유)
│   ├── Profile 1 (회사용)
│   │   └── assignedFriends: [chatroom-1, chatroom-2]
│   └── Profile 2 (친구용)
│       └── assignedFriends: [chatroom-3, chatroom-4]
└── ChatRooms (여러 채팅방 참여)
    ├── ChatRoom 1 (김부장님)
    │   ├── member1: user-A (나)
    │   └── member2: user-B (김부장님)
    └── ChatRoom 2 (친구 민수)
        ├── member1: user-A (나)
        └── member2: user-C (민수)
```

**DB 테이블 설계:**

1. **users** 테이블
   - id (PK)
   - ldap (unique)
   - name
   - avatar

2. **profiles** 테이블
   - id (PK)
   - user_id (FK → users.id)
   - name
   - avatar
   - description
   - default_persona
   - is_default (BOOLEAN) - "전체" 프로필 여부

3. **chat_rooms** 테이블
   - id (PK)
   - name
   - avatar
   - is_group
   - last_message
   - last_message_time

4. **chat_room_members** 테이블 (채팅방 멤버십)
   - id (PK)
   - chat_room_id (FK → chat_rooms.id)
   - user_id (FK → users.id)
   - formality_level
   - relationship
   - joined_at

5. **profile_chat_room_mappings** 테이블 (프로필-채팅방 매핑) ⭐ 핵심
   - id (PK)
   - profile_id (FK → profiles.id)
   - chat_room_id (FK → chat_rooms.id)
   - created_at
   - **UNIQUE(profile_id, chat_room_id)** - 중복 방지

**동작 방식:**

1. **채팅방 생성 시** (`POST /api/v1/chatrooms`):
   ```
   Request: { friendLdap, formalityLevel, relationship, profileId }

   → DB 작업:
   1. chat_rooms 테이블에 새 레코드 생성
   2. chat_room_members에 나와 친구 추가
   3. profileId가 제공되면 profile_chat_room_mappings에 매핑 추가
   ```

2. **프로필별 채팅방 조회** (`GET /api/v1/chatrooms?profileId=xxx`):
   ```sql
   SELECT cr.*
   FROM chat_rooms cr
   JOIN profile_chat_room_mappings pcrm ON cr.id = pcrm.chat_room_id
   WHERE pcrm.profile_id = :profileId
     AND EXISTS (
       SELECT 1 FROM chat_room_members crm
       WHERE crm.chat_room_id = cr.id
         AND crm.user_id = :currentUserId
     )
   ```

3. **프로필에 친구 추가** (`PUT /api/v1/profiles/{profileId}`):
   ```
   Request: { assignedFriends: ["chatroom-1", "chatroom-3"] }

   → DB 작업:
   1. 기존 profile_chat_room_mappings에서 해당 profile_id 매핑 삭제
   2. assignedFriends 배열의 각 chatroom_id에 대해 새 매핑 생성
   ```

**중요 사항:**
- 각 사용자는 자신의 프로필에만 채팅방을 할당할 수 있음
- 채팅방 삭제 시 관련된 profile_chat_room_mappings도 자동 삭제 (CASCADE)
- 프로필 삭제 시 매핑만 삭제되고 채팅방은 유지됨
- 한 채팅방은 여러 프로필에 동시에 속할 수 있음 (예: 회사 동료가 친구로도 등록)
- "전체" 프로필(`id: "all"`)은 시스템 기본 프로필로, 모든 사용자에게 자동 생성되며 삭제 불가
- "전체" 프로필의 `assignedFriends`가 빈 배열이면 모든 채팅방 표시

---

## 7. 이모티콘 API

### 7.1 이모티콘 팩 목록 조회

```
GET /api/v1/emoticons
```

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "name": "기본",
      "emoticons": [
        {
          "id": "e1",
          "name": "좋아요",
          "imageUrl": "/emoticons/thumbs-up.jpg",
          "category": "기본"
        }
      ]
    },
    {
      "name": "비즈니스",
      "emoticons": [
        {
          "id": "b1",
          "name": "확인",
          "imageUrl": "/emoticons/ok-check.jpg",
          "category": "비즈니스"
        }
      ]
    }
  ]
}
```

---

## 8. 에러 응답

모든 API는 에러 발생 시 다음 형식으로 응답합니다.

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": {}
  }
}
```

**HTTP Status Codes**
| Code | Description |
|------|-------------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 500 | 서버 에러 |

**Error Codes**
| Code | Description |
|------|-------------|
| AUTH_INVALID_LDAP | 유효하지 않은 LDAP |
| AUTH_MISSING_LDAP | LDAP 헤더 누락 |
| CHATROOM_NOT_FOUND | 채팅방 없음 |
| MESSAGE_NOT_FOUND | 메시지 없음 |
| PROFILE_NOT_FOUND | 프로필 없음 |
| AI_SERVICE_ERROR | AI 서비스 오류 |
| VALIDATION_ERROR | 유효성 검사 실패 |
| INVALID_IMAGE_FORMAT | 지원하지 않는 이미지 형식 |
| IMAGE_TOO_LARGE | 이미지 크기 초과 (5MB 제한) |
| INVALID_FORMALITY_LEVEL | 격식 수준 범위 오류 (0-100) |
| EMPTY_MESSAGE_TEXT | 변환할 메시지가 비어있음 |

---

## 9. 실시간 업데이트 API (Polling)

### 9.1 새 메시지 확인 (Polling)

채팅방의 새 메시지를 폴링하여 조회

```
GET /api/v1/chatrooms/{roomId}/messages/poll
```

**Request Headers**
```
X-LDAP: {user_ldap}
```

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| since | string | N | 이 시간 이후의 메시지만 조회 (ISO 8601) |
| lastMessageId | string | N | 이 메시지 ID 이후의 메시지만 조회 |

**Response (200 OK)**
```json
{
  "success": true,
  "data": [
    {
      "id": "string",
      "content": "string",
      "originalContent": "string",
      "sender": "other",
      "senderName": "김부장님",
      "senderAvatar": "string",
      "timestamp": "2026-01-15T12:00:00Z",
      "reactions": ["👍"],
      "wasGuarded": false,
      "isEmoticon": false,
      "emoticonId": null,
      "isRead": true  // 상대방이 읽었는지 여부 (내가 보낸 메시지인 경우에만 의미 있음)
    }
  ]
}
```

### 9.2 전체 채팅방 업데이트 확인

모든 채팅방의 새 메시지 및 업데이트 확인

```
GET /api/v1/chatrooms/updates
```

**Request Headers**
```
X-LDAP: {user_ldap}
```

**Query Parameters**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| since | string | N | 이 시간 이후의 업데이트만 조회 (ISO 8601) |

**Response (200 OK)**
```json
{
  "success": true,
  "data": {
    "updates": [
      {
        "roomId": "string",
        "roomName": "김부장님",
        "lastMessage": "네, 확인했습니다.",
        "lastMessageTime": "2026-01-15T12:00:00Z",
        "unreadCount": 1,
        "hasNewMessage": true
      }
    ],
    "timestamp": "2026-01-15T12:00:00Z"
  }
}
```

---

## 부록: 데이터 타입 정의

### Persona ID
| ID | Name | Formality Range |
|----|------|-----------------|
| very-formal | 매우 정중함 | 80-100% |
| formal | 정중함 | 60-79% |
| casual-polite | 친근하지만 예의있게 | 40-59% |
| casual | 친근함 | 20-39% |
| very-casual | 매우 친근함 | 0-19% |

### Relationship Type
| Type | Description | Default Formality |
|------|-------------|-------------------|
| boss | 상사 | 95% |
| senior | 선배 | 70% |
| colleague | 동료 | 50% |
| friend | 친구 | 10% |
| family | 가족 | 15% |
