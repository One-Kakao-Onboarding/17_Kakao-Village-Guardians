export interface User {
  id: string
  ldap: string
  name: string
  avatar: string
}

export interface ChatRoom {
  id: string
  name: string
  avatar: string
  lastMessage?: string
  lastMessageTime?: string
  unreadCount?: number
  formalityLevel: number
  relationship: 'boss' | 'senior' | 'colleague' | 'friend' | 'family'
  isGroup?: boolean
  keywords?: string[]
  members?: User[]
}

export interface Reaction {
  id: string
  user: User
  emoji: string
  createdAt: string
}

export interface Message {
  id: string
  content: string
  originalContent?: string
  sender: User
  timestamp: string
  reactions?: Reaction[]
  wasGuarded?: boolean
  isEmoticon?: boolean
  emoticonId?: string | null
  isRead?: boolean  // 상대방이 읽었는지 여부 (sender가 "me"인 경우에만 의미 있음)
}

export interface Profile {
  id: string
  name: string
  avatar: string
  description: string
  defaultPersona: string
  linkedChatRoomIds?: string[]
}

export interface Emoticon {
  id: string
  name: string
  imageUrl: string
  category: string
}

export interface EmoticonPack {
  name: string
  emoticons: Emoticon[]
}

export interface TransformResponse {
  originalText: string
  transformedText: string
  formalityLevel: number
  appliedPersona: string
  changes: Array<{
    type: 'tone' | 'detail' | 'formality' | 'vocabulary'
    description: string
  }>
  shouldSuggest: boolean
  suggestionReason?: string
}

export interface EmotionGuardResponse {
  isAggressive: boolean
  aggressionType?: 'sarcasm' | 'passive_aggressive' | 'direct_attack' | 'dismissive'
  aggressionScore: number
  suggestedText?: string
  warningMessage?: string
}

export interface ReactionSuggestion {
  emotion: 'happy' | 'sad' | 'angry' | 'surprised' | 'neutral' | 'excited' | 'worried'
  emotionScore: number
  suggestedEmojis: string[]
  suggestedTexts: Array<{
    text: string
    type: string
  }>
  quickResponses: Array<{
    text: string
    icon: string
  }>
}

export interface FriendMatchingResponse {
  recommendations: Array<{
    chatRoomId: string
    chatRoomName: string
    matchScore: number
    matchReason: string
  }>
}

export interface ChatRoomUpdate {
  roomId: string
  roomName: string
  lastMessage: string
  lastMessageTime: string
  unreadCount: number
  hasNewMessage: boolean
}

export interface UpdatesResponse {
  updates: ChatRoomUpdate[]
  timestamp: string
}
