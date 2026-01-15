export interface Message {
  id: string
  content: string
  originalContent?: string
  sender: "me" | "other"
  timestamp: Date
  reactions?: string[]
  wasGuarded?: boolean
  isEmoticon?: boolean
  emoticonId?: string
  isRead?: boolean  // 상대방이 읽었는지 여부 (sender가 "me"인 경우에만 의미 있음)
}

export interface ChatRoom {
  id: string
  name: string
  avatar: string
  lastMessage?: string
  lastMessageTime?: Date
  unreadCount?: number
  formalityLevel: number
  relationship: "boss" | "senior" | "colleague" | "friend" | "family"
  isGroup?: boolean
  keywords?: string[]
}

export interface Persona {
  id: string
  name: string
  description: string
  icon: string
  promptStyle: string
  formalityRange: [number, number]
}

export interface Profile {
  id: string
  name: string
  avatar: string
  description: string
  defaultPersona: string
}

export interface Emoticon {
  id: string
  name: string
  imageUrl: string
  category: string
}

export const EMOTICON_PACKS: { name: string; emoticons: Emoticon[] }[] = [
  {
    name: "기본",
    emoticons: [
      // 카카오프렌즈 스타일 플레이스홀더 이미지
      {
        id: "e1",
        name: "좋아요",
        imageUrl: "/cute-yellow-cat-character-thumbs-up-kakao-friends-.jpg",
        category: "기본",
      },
      { id: "e2", name: "하트", imageUrl: "/cute-yellow-cat-character-heart-love-kakao-friends.jpg", category: "기본" },
      { id: "e3", name: "웃음", imageUrl: "/cute-yellow-cat-character-laughing-kakao-friends-s.jpg", category: "기본" },
      { id: "e4", name: "놀람", imageUrl: "/cute-yellow-cat-character-surprised-kakao-friends-.jpg", category: "기본" },
      { id: "e5", name: "슬픔", imageUrl: "/cute-yellow-cat-character-sad-crying-kakao-friends.jpg", category: "기본" },
      { id: "e6", name: "화남", imageUrl: "/cute-yellow-cat-character-angry-kakao-friends-styl.jpg", category: "기본" },
      { id: "e7", name: "감사", imageUrl: "/cute-yellow-cat-character-thank-you-bowing-kakao-f.jpg", category: "기본" },
      { id: "e8", name: "응원", imageUrl: "/cute-yellow-cat-character-cheering-kakao-friends-s.jpg", category: "기본" },
    ],
  },
  {
    name: "비즈니스",
    emoticons: [
      {
        id: "b1",
        name: "확인",
        imageUrl: "/cute-yellow-cat-character-ok-check-kakao-friends-s.jpg",
        category: "비즈니스",
      },
      {
        id: "b2",
        name: "회의중",
        imageUrl: "/cute-yellow-cat-character-busy-meeting-kakao-frien.jpg",
        category: "비즈니스",
      },
      {
        id: "b3",
        name: "잠시만",
        imageUrl: "/cute-yellow-cat-character-wait-moment-kakao-friend.jpg",
        category: "비즈니스",
      },
      {
        id: "b4",
        name: "고생했어요",
        imageUrl: "/cute-yellow-cat-character-good-job-tired-kakao-fri.jpg",
        category: "비즈니스",
      },
    ],
  },
]

export const PERSONAS: Persona[] = [
  {
    id: "very-formal",
    name: "매우 정중함",
    description: "최대한 격식을 차린 표현",
    icon: "👔",
    promptStyle: "매우 공손하고 격식있는 표현으로 변환해주세요.",
    formalityRange: [80, 100],
  },
  {
    id: "formal",
    name: "정중함",
    description: "예의 바른 표현",
    icon: "🤝",
    promptStyle: "정중하고 예의바른 표현으로 변환해주세요.",
    formalityRange: [60, 79],
  },
  {
    id: "casual-polite",
    name: "친근하지만 예의있게",
    description: "편안하면서도 예의있는 표현",
    icon: "😊",
    promptStyle: "친근하면서도 예의있는 표현으로 변환해주세요.",
    formalityRange: [40, 59],
  },
  {
    id: "casual",
    name: "친근함",
    description: "편안한 대화체",
    icon: "🙂",
    promptStyle: "친근하고 편안한 표현으로 변환해주세요.",
    formalityRange: [20, 39],
  },
  {
    id: "very-casual",
    name: "매우 친근함",
    description: "친한 친구와의 대화",
    icon: "😎",
    promptStyle: "매우 친근하고 캐주얼한 표현으로 변환해주세요.",
    formalityRange: [0, 19],
  },
]

export const REACTION_EMOJIS = ["👍", "❤️", "😂", "😮", "😢", "👏", "🎉", "🔥", "💯", "🙏"]

export const QUICK_RESPONSES_BY_PERSONA: Record<string, { text: string; icon: string }[]> = {
  "very-formal": [
    { text: "네, 말씀하신 대로 진행하겠습니다.", icon: "✅" },
    { text: "확인 후 다시 보고드리겠습니다.", icon: "📋" },
    { text: "감사합니다. 좋은 하루 되세요.", icon: "🙏" },
  ],
  formal: [
    { text: "네, 확인했습니다. 진행할게요.", icon: "✅" },
    { text: "잠시 후에 다시 연락드릴게요.", icon: "📞" },
    { text: "감사합니다!", icon: "🙏" },
  ],
  "casual-polite": [
    { text: "네~ 알겠어요!", icon: "👍" },
    { text: "확인했어요, 고마워요!", icon: "✅" },
    { text: "잠시만요, 바로 할게요!", icon: "⏰" },
  ],
  casual: [
    { text: "ㅇㅋ 알겠어~", icon: "👍" },
    { text: "ㄱㅅ!", icon: "🙏" },
    { text: "잠만 기다려~", icon: "⏰" },
  ],
  "very-casual": [
    { text: "ㅇㅇ ㄱㄱ", icon: "👍" },
    { text: "ㅋㅋㅋ ㅇㅋ", icon: "😂" },
    { text: "ㄴㄴ 안됨", icon: "❌" },
  ],
}

export const QUICK_RESPONSES = [
  { text: "지금 회의 중이라 30분 뒤에 자세히 답변드릴게요!", icon: "⏰" },
  { text: "네, 확인했습니다. 말씀하신 대로 진행할게요.", icon: "✅" },
  { text: "감사합니다! 좋은 하루 보내세요.", icon: "🙏" },
  { text: "죄송합니다, 잠시 후에 다시 연락드릴게요.", icon: "📞" },
]
