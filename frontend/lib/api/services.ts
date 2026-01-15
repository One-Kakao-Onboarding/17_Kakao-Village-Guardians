import { getApiClient } from './client'
import type {
  User,
  ChatRoom,
  Message,
  Profile,
  EmoticonPack,
  TransformResponse,
  EmotionGuardResponse,
  ReactionSuggestion,
  FriendMatchingResponse,
  UpdatesResponse,
} from './types'

// User API
export const userApi = {
  getCurrentUser: () => getApiClient().get<User>('/api/v1/users/me'),

  updateUser: (data: { name?: string; avatar?: string }) =>
    getApiClient().put<User>('/api/v1/users/me', data),
}

// ChatRoom API
export const chatRoomApi = {
  getChatRooms: (profileId?: string) => {
    const query = profileId ? `?profileId=${encodeURIComponent(profileId)}` : ''
    return getApiClient().get<ChatRoom[]>(`/api/v1/chatrooms${query}`)
  },

  getChatRoom: (roomId: string) =>
    getApiClient().get<ChatRoom>(`/api/v1/chatrooms/${roomId}`),

  createChatRoom: (data: {
    friendLdap: string
    formalityLevel: number
    relationship: 'boss' | 'senior' | 'colleague' | 'friend' | 'family'
    profileId?: string
  }) => getApiClient().post<ChatRoom>('/api/v1/chatrooms', data),

  markAsRead: (roomId: string) =>
    getApiClient().put<{ success: boolean; message: string }>(
      `/api/v1/chatrooms/${roomId}/read`
    ),

  deleteChatRoom: (roomId: string) =>
    getApiClient().delete<{ success: boolean; message: string }>(
      `/api/v1/chatrooms/${roomId}`
    ),
}

// Message API
export const messageApi = {
  getMessages: (roomId: string) =>
    getApiClient().get<Message[]>(`/api/v1/chatrooms/${roomId}/messages`),

  sendMessage: (
    roomId: string,
    data: {
      content: string
      originalContent?: string
      isEmoticon?: boolean
      emoticonId?: string | null
    }
  ) => getApiClient().post<Message>(`/api/v1/chatrooms/${roomId}/messages`, data),

  addReaction: (messageId: string, emoji: string) =>
    getApiClient().post<{ messageId: string; reactions: string[] }>(
      `/api/v1/messages/${messageId}/reactions`,
      { emoji }
    ),

  // Polling API
  pollMessages: (
    roomId: string,
    params?: { since?: string; lastMessageId?: string }
  ) => {
    const query = new URLSearchParams()
    if (params?.since) query.append('since', params.since)
    if (params?.lastMessageId) query.append('lastMessageId', params.lastMessageId)
    const queryString = query.toString()
    return getApiClient().get<Message[]>(
      `/api/v1/chatrooms/${roomId}/messages/poll${queryString ? `?${queryString}` : ''}`
    )
  },

  getUpdates: (params?: { since?: string }) => {
    const query = new URLSearchParams()
    if (params?.since) query.append('since', params.since)
    const queryString = query.toString()
    return getApiClient().get<UpdatesResponse>(
      `/api/v1/chatrooms/updates${queryString ? `?${queryString}` : ''}`
    )
  },
}

// AI API
export const aiApi = {
  transform: (data: {
    text: string
    personaId?: string
    formalityLevel: number
    relationship: 'boss' | 'senior' | 'colleague' | 'friend' | 'family'
    roomId?: string
  }) => getApiClient().post<TransformResponse>('/api/v1/ai/transform', data),

  emotionGuard: (data: { text: string }) =>
    getApiClient().post<EmotionGuardResponse>('/api/v1/ai/emotion-guard', data),

  reactionSuggest: (data: {
    message: string
    relationship: string
    formalityLevel: number
  }) => getApiClient().post<ReactionSuggestion>('/api/v1/ai/reaction-suggest', data),

  friendMatching: (data: {
    profileName: string
    personaId: string
    chatRoomIds: string[]
  }) =>
    getApiClient().post<FriendMatchingResponse>('/api/v1/ai/friend-matching', data),
}

// Profile API
export const profileApi = {
  getProfiles: () => getApiClient().get<Profile[]>('/api/v1/profiles'),

  createProfile: (data: {
    name: string
    description: string
    defaultPersona: string
    linkedChatRoomIds?: string[]
  }) => getApiClient().post<Profile>('/api/v1/profiles', data),

  updateProfile: (
    profileId: string,
    data: {
      name: string
      description: string
      defaultPersona: string
      linkedChatRoomIds?: string[]
    }
  ) => getApiClient().put<Profile>(`/api/v1/profiles/${profileId}`, data),

  deleteProfile: (profileId: string) =>
    getApiClient().delete<{ success: boolean; message: string }>(
      `/api/v1/profiles/${profileId}`
    ),
}

// Emoticon API
export const emoticonApi = {
  getEmoticons: () => getApiClient().get<EmoticonPack[]>('/api/v1/emoticons'),
}
