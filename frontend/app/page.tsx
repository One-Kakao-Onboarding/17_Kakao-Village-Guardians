"use client"

import { useState, useCallback, useRef, useEffect } from "react"
import { ChatList } from "@/components/chat/chat-list"
import { ChatRoom } from "@/components/chat/chat-room"
import { LoginScreen } from "@/components/login-screen"
import type { Message, ChatRoom as ChatRoomType, Profile } from "@/lib/types"
import { generateId } from "@/lib/chat-utils"
import { cn } from "@/lib/utils"
import { MessageCircle } from "lucide-react"
import { chatRoomApi, profileApi, messageApi, userApi } from "@/lib/api/services"
import { initializeApiClient } from "@/lib/api/client"
import type { Message as ApiMessage, ChatRoom as ApiChatRoom } from "@/lib/api/types"

const DEFAULT_PROFILES: Profile[] = [
  {
    id: "1",
    name: "회사용",
    avatar: "/professional-business-profile.png",
    description: "직장 동료 및 상사와의 대화용",
    defaultPersona: "formal",
  },
  {
    id: "2",
    name: "친구용",
    avatar: "/casual-profile.png",
    description: "친구들과의 편한 대화용",
    defaultPersona: "casual",
  },
]

const DEMO_ROOMS: ChatRoomType[] = [
  {
    id: "1",
    name: "김부장님",
    avatar: "/professional-man-boss.jpg",
    lastMessage: "내일 회의 자료 준비해주세요",
    lastMessageTime: new Date(Date.now() - 1000 * 60 * 30),
    formalityLevel: 95,
    relationship: "boss",
    keywords: ["보고", "회의", "검토", "결재", "업무지시", "프로젝트"],
  },
  {
    id: "2",
    name: "이선배",
    avatar: "/friendly-senior-woman.jpg",
    lastMessage: "점심 같이 먹을래요?",
    lastMessageTime: new Date(Date.now() - 1000 * 60 * 60 * 2),
    formalityLevel: 70,
    relationship: "senior",
    keywords: ["식사", "조언", "업무", "커리어", "멘토링", "피드백"],
  },
  {
    id: "3",
    name: "마케팅팀",
    avatar: "/team-meeting-group.jpg",
    lastMessage: "다음 주 캠페인 논의합시다",
    lastMessageTime: new Date(Date.now() - 1000 * 60 * 60 * 5),
    unreadCount: 3,
    formalityLevel: 50,
    relationship: "colleague",
    isGroup: true,
    keywords: ["협업", "공유", "일정", "캠페인", "미팅", "브레인스토밍"],
  },
  {
    id: "4",
    name: "민수",
    avatar: "/casual-young-man-friend.jpg",
    lastMessage: "ㅋㅋㅋ 그거 진짜 웃기다",
    lastMessageTime: new Date(Date.now() - 1000 * 60 * 60 * 24),
    formalityLevel: 5,
    relationship: "friend",
    keywords: ["ㅋㅋ", "대박", "오늘", "영화", "게임", "술", "놀자"],
  },
  {
    id: "5",
    name: "엄마",
    avatar: "/mother-woman-family.jpg",
    lastMessage: "저녁 뭐 먹을래?",
    lastMessageTime: new Date(Date.now() - 1000 * 60 * 60 * 48),
    formalityLevel: 10,
    relationship: "family",
    keywords: ["식사", "집", "안부", "건강", "명절", "가족모임"],
  },
]

const DEMO_MESSAGES: Record<string, Message[]> = {
  "1": [
    {
      id: "1-1",
      content: "안녕하세요, 부장님. 어제 요청하신 자료 정리 중입니다.",
      sender: "me",
      timestamp: new Date(Date.now() - 1000 * 60 * 60),
    },
    {
      id: "1-2",
      content: "내일 회의 자료 준비해주세요",
      sender: "other",
      timestamp: new Date(Date.now() - 1000 * 60 * 30),
    },
  ],
  "2": [
    {
      id: "2-1",
      content: "선배님, 혹시 점심 약속 있으세요?",
      sender: "me",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 3),
    },
    {
      id: "2-2",
      content: "점심 같이 먹을래요?",
      sender: "other",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2),
    },
  ],
  "3": [
    {
      id: "3-1",
      content: "다들 다음 주 캠페인 기획안 검토 부탁드려요~",
      sender: "other",
      senderName: "팀장님",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 6), // 오후 06:48
    },
    {
      id: "3-2",
      content: "저는 검토 완료했습니다!",
      sender: "other",
      senderName: "박대리",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 5.5), // 오후 07:18
    },
    {
      id: "3-3",
      content: "다음 주 캠페인 논의합시다",
      sender: "me",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 5), // 오후 07:48
    },
  ],
  "4": [
    {
      id: "4-1",
      content: "야 어제 그 영상 봤어?",
      sender: "other",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 25),
    },
    {
      id: "4-2",
      content: "ㅋㅋㅋ 그거 진짜 웃기다",
      sender: "other",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24),
    },
  ],
  "5": [
    {
      id: "5-1",
      content: "저녁 뭐 먹을래?",
      sender: "other",
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 48),
    },
  ],
}

// Helper function to convert API chat room to local chat room type
function convertApiChatRoom(room: ApiChatRoom): ChatRoomType {
  return {
    id: room.id,
    name: room.name,
    avatar: room.avatar,
    lastMessage: room.lastMessage,
    lastMessageTime: room.lastMessageTime ? new Date(room.lastMessageTime) : undefined,
    unreadCount: room.unreadCount,
    formalityLevel: room.formalityLevel,
    relationship: room.relationship,
    isGroup: room.isGroup,
    keywords: room.keywords,
  }
}

export default function Home() {
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [userLdap, setUserLdap] = useState("")
  const [userName, setUserName] = useState("")
  const [userAvatar, setUserAvatar] = useState<string | null>(null)
  const [selectedRoom, setSelectedRoom] = useState<ChatRoomType | null>(null)
  const [messages, setMessages] = useState<Record<string, Message[]>>({})
  const [profiles, setProfiles] = useState<Profile[]>([])
  const [activeProfile, setActiveProfile] = useState<Profile | null>(null)
  const [rooms, setRooms] = useState<ChatRoomType[]>([])
  const [loading, setLoading] = useState(false)

  // Polling intervals from env
  const CHATROOMS_POLLING_INTERVAL = Number(process.env.NEXT_PUBLIC_CHATROOMS_POLLING_INTERVAL) || 5000
  const MESSAGES_POLLING_INTERVAL = Number(process.env.NEXT_PUBLIC_MESSAGES_POLLING_INTERVAL) || 3000

  // Check local storage for saved login on mount
  useEffect(() => {
    const savedLdap = localStorage.getItem('userLdap')
    const savedName = localStorage.getItem('userName')
    const savedAvatar = localStorage.getItem('userAvatar')
    if (savedLdap && savedName) {
      setUserLdap(savedLdap)
      setUserName(savedName)
      setUserAvatar(savedAvatar)
      setIsLoggedIn(true)
      // Re-initialize API client with saved LDAP
      initializeApiClient(savedLdap)
    }
  }, [])

  const handleLogin = useCallback((ldap: string, name: string) => {
    setUserLdap(ldap)
    setUserName(name)
    setIsLoggedIn(true)
    // Save to local storage
    localStorage.setItem('userLdap', ldap)
    localStorage.setItem('userName', name)
  }, [])

  const handleLogout = useCallback(() => {
    setIsLoggedIn(false)
    setUserLdap("")
    setUserName("")
    setUserAvatar(null)
    setSelectedRoom(null)
    setMessages({})
    setRooms([])
    setProfiles([])
    setActiveProfile(null)
    // Clear local storage
    localStorage.removeItem('userLdap')
    localStorage.removeItem('userName')
    localStorage.removeItem('userAvatar')
  }, [])

  const handleAvatarChange = useCallback(async (file: File) => {
    const reader = new FileReader()
    reader.onloadend = async () => {
      const dataUrl = reader.result as string
      setUserAvatar(dataUrl)
      localStorage.setItem('userAvatar', dataUrl)

      // Upload to backend
      try {
        await userApi.updateUser({ avatar: dataUrl })
      } catch (error) {
        console.error('Failed to update avatar:', error)
        alert('프로필 사진 업데이트에 실패했습니다.')
      }
    }
    reader.readAsDataURL(file)
  }, [])

  // Helper function to convert API message to local message type
  const convertApiMessage = useCallback((msg: ApiMessage): Message => {
    return {
      id: msg.id,
      content: msg.content,
      originalContent: msg.originalContent,
      sender: msg.sender.ldap === userLdap ? "me" : "other",
      senderName: msg.sender.name,
      senderAvatar: msg.sender.avatar,
      timestamp: new Date(msg.timestamp),
      reactions: msg.reactions?.map(r => r.emoji),
      wasGuarded: msg.wasGuarded,
      isEmoticon: msg.isEmoticon,
      emoticonId: msg.emoticonId || undefined,
      isRead: msg.isRead,
    }
  }, [userLdap])

  // Load initial data after login
  useEffect(() => {
    if (!isLoggedIn) return

    const loadData = async () => {
      setLoading(true)
      try {
        // Load profiles first
        const profilesData = await profileApi.getProfiles()
        setProfiles(profilesData)
        if (profilesData.length > 0) {
          setActiveProfile(profilesData[0])

          // Load chat rooms filtered by first profile
          const roomsData = await chatRoomApi.getChatRooms(profilesData[0].id)
          const convertedRooms = roomsData.map(convertApiChatRoom)
          setRooms(convertedRooms)
        } else {
          // No profiles, load all chat rooms
          const roomsData = await chatRoomApi.getChatRooms()
          const convertedRooms = roomsData.map(convertApiChatRoom)
          setRooms(convertedRooms)
        }
      } catch (error) {
        console.error('Failed to load initial data:', error)
        // Fallback to demo data
        setRooms(DEMO_ROOMS)
        setProfiles(DEFAULT_PROFILES)
        setActiveProfile(DEFAULT_PROFILES[0])
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [isLoggedIn])

  // Reload chatrooms when activeProfile changes
  useEffect(() => {
    if (!isLoggedIn || !activeProfile) return

    const loadChatRooms = async () => {
      try {
        const roomsData = await chatRoomApi.getChatRooms(activeProfile.id)
        const convertedRooms = roomsData.map(convertApiChatRoom)
        setRooms(convertedRooms)
      } catch (error) {
        console.error('Failed to load chat rooms for profile:', error)
      }
    }

    loadChatRooms()
  }, [isLoggedIn, activeProfile])

  // Polling for chatrooms list
  useEffect(() => {
    if (!isLoggedIn) return

    const loadChatRooms = async () => {
      try {
        const roomsData = await chatRoomApi.getChatRooms(activeProfile?.id)
        const convertedRooms = roomsData.map(convertApiChatRoom)
        setRooms(convertedRooms)
      } catch (error) {
        console.error('Failed to poll chat rooms:', error)
      }
    }

    const intervalId = setInterval(loadChatRooms, CHATROOMS_POLLING_INTERVAL)

    return () => clearInterval(intervalId)
  }, [isLoggedIn, activeProfile?.id, CHATROOMS_POLLING_INTERVAL])

  const handleSelectRoom = useCallback(
    async (room: ChatRoomType) => {
      setSelectedRoom(room)
      setRooms((prev) => prev.map((r) => (r.id === room.id ? { ...r, unreadCount: undefined } : r)))

      // Load messages for this room
      const loadMessages = async () => {
        try {
          const messagesData = await messageApi.getMessages(room.id)
          const convertedMessages = messagesData.map(convertApiMessage)
          setMessages((prev) => ({
            ...prev,
            [room.id]: convertedMessages,
          }))
        } catch (error) {
          console.error('Failed to load messages:', error)
        }
      }

      // Initial load
      await loadMessages()

      // Mark room as read
      try {
        await chatRoomApi.markAsRead(room.id)
      } catch (error) {
        console.error('Failed to mark as read:', error)
      }

      // Start polling - reload all messages
      const intervalId = setInterval(loadMessages, MESSAGES_POLLING_INTERVAL)

      // Store interval ID for cleanup
      setMessages((prev) => ({
        ...prev,
        [`__interval_${room.id}`]: intervalId as any,
      }))
    },
    [convertApiMessage, MESSAGES_POLLING_INTERVAL]
  )

  const handleBack = useCallback(() => {
    if (selectedRoom) {
      // Stop polling for this room
      setMessages((prev) => {
        const intervalId = prev[`__interval_${selectedRoom.id}`]
        if (intervalId) {
          clearInterval(intervalId as any)
          const { [`__interval_${selectedRoom.id}`]: _, ...rest } = prev
          return rest
        }
        return prev
      })
    }
    setSelectedRoom(null)
  }, [selectedRoom])

  const handleSendMessage = useCallback(
    async (message: Message) => {
      if (!selectedRoom) return

      try {
        // Send message to API
        await messageApi.sendMessage(selectedRoom.id, {
          content: message.content,
          originalContent: message.originalContent,
          isEmoticon: message.isEmoticon,
          emoticonId: message.emoticonId || null,
        })

        // Immediately reload messages after sending
        const messagesData = await messageApi.getMessages(selectedRoom.id)
        const convertedMessages = messagesData.map(convertApiMessage)
        setMessages((prev) => ({
          ...prev,
          [selectedRoom.id]: convertedMessages,
        }))

        // Mark as read and clear unread count
        await chatRoomApi.markAsRead(selectedRoom.id)
        setRooms((prev) => prev.map((r) => (r.id === selectedRoom.id ? { ...r, unreadCount: 0 } : r)))
      } catch (error) {
        console.error('Failed to send message:', error)
      }
    },
    [selectedRoom, convertApiMessage],
  )

  const handleAddReaction = useCallback(
    async (messageId: string, emoji: string) => {
      if (!selectedRoom) return

      try {
        await messageApi.addReaction(messageId, emoji)

        // Immediately reload messages after adding reaction
        const messagesData = await messageApi.getMessages(selectedRoom.id)
        const convertedMessages = messagesData.map(convertApiMessage)
        setMessages((prev) => ({
          ...prev,
          [selectedRoom.id]: convertedMessages,
        }))
      } catch (error) {
        console.error('Failed to add reaction:', error)
      }
    },
    [selectedRoom, convertApiMessage],
  )

  const handleAddProfile = useCallback(async (profileData: Omit<Profile, "id">) => {
    try {
      const newProfile = await profileApi.createProfile(profileData)
      setProfiles((prev) => [...prev, newProfile])
    } catch (error) {
      console.error('Failed to create profile:', error)
    }
  }, [])

  const handleEditProfile = useCallback(async (id: string, profileData: Omit<Profile, "id">) => {
    try {
      const updatedProfile = await profileApi.updateProfile(id, profileData)
      setProfiles((prev) => prev.map((p) => (p.id === id ? updatedProfile : p)))
    } catch (error) {
      console.error('Failed to update profile:', error)
    }
  }, [])

  const handleDeleteProfile = useCallback(
    async (id: string) => {
      try {
        await profileApi.deleteProfile(id)
        setProfiles((prev) => prev.filter((p) => p.id !== id))
        if (activeProfile?.id === id) {
          setActiveProfile(profiles.find((p) => p.id !== id) || profiles[0] || null)
        }
      } catch (error) {
        console.error('Failed to delete profile:', error)
      }
    },
    [activeProfile, profiles],
  )

  const handleAddFriend = useCallback(
    async (data: {
      friendLdap: string
      formalityLevel: number
      relationship: "boss" | "senior" | "colleague" | "friend" | "family"
      profileId?: string
    }) => {
      try {
        const newChatRoom = await chatRoomApi.createChatRoom(data)
        const convertedRoom = convertApiChatRoom(newChatRoom)
        setRooms((prev) => [convertedRoom, ...prev])

        // Reload chat rooms to ensure proper filtering
        const roomsData = await chatRoomApi.getChatRooms(activeProfile?.id)
        const convertedRooms = roomsData.map(convertApiChatRoom)
        setRooms(convertedRooms)
      } catch (error) {
        console.error('Failed to add friend:', error)
        throw error
      }
    },
    [activeProfile?.id],
  )

  const handleDeleteChatRoom = useCallback(
    async (roomId: string) => {
      try {
        await chatRoomApi.deleteChatRoom(roomId)

        // If deleted room is currently selected, clear selection
        if (selectedRoom?.id === roomId) {
          setSelectedRoom(null)
          // Stop polling for this room
          const intervalId = messages[`__interval_${roomId}`]
          if (intervalId) {
            clearInterval(intervalId as any)
          }
        }

        // Remove from rooms list
        setRooms((prev) => prev.filter((r) => r.id !== roomId))

        // Clean up messages
        setMessages((prev) => {
          const { [roomId]: _, [`__interval_${roomId}`]: __, ...rest } = prev
          return rest
        })
      } catch (error) {
        console.error('Failed to delete chat room:', error)
        throw error
      }
    },
    [selectedRoom, messages],
  )

  if (!isLoggedIn) {
    return <LoginScreen onLogin={handleLogin} />
  }

  return (
    <div className="h-screen flex">
      <aside
        className={cn(
          "w-full md:w-80 lg:w-96 border-r border-border bg-card",
          "md:block",
          selectedRoom ? "hidden" : "block",
        )}
      >
        <ChatList
          rooms={rooms}
          selectedRoom={selectedRoom}
          onSelectRoom={handleSelectRoom}
          profiles={profiles}
          activeProfile={activeProfile}
          onSelectProfile={setActiveProfile}
          onAddProfile={handleAddProfile}
          onEditProfile={handleEditProfile}
          onDeleteProfile={handleDeleteProfile}
          onAddFriend={handleAddFriend}
          onDeleteChatRoom={handleDeleteChatRoom}
          onLogout={handleLogout}
          userName={userName}
          userAvatar={userAvatar}
          onAvatarChange={handleAvatarChange}
        />
      </aside>

      <main className={cn("flex-1", "md:block", selectedRoom ? "block" : "hidden md:block")}>
        {selectedRoom ? (
          <ChatRoom
            key={selectedRoom.id}
            room={selectedRoom}
            messages={messages[selectedRoom.id] || []}
            onBack={handleBack}
            onSendMessage={handleSendMessage}
            onAddReaction={handleAddReaction}
            onDeleteRoom={() => handleDeleteChatRoom(selectedRoom.id)}
          />
        ) : (
          <div className="hidden md:flex flex-col items-center justify-center h-full bg-muted/30">
            <div className="w-20 h-20 rounded-full bg-primary/10 flex items-center justify-center mb-4">
              <MessageCircle className="h-10 w-10 text-primary" />
            </div>
            <h2 className="text-xl font-semibold text-foreground mb-2">스마트톡</h2>
            <p className="text-muted-foreground text-center max-w-sm">
              AI가 도와주는 스마트한 메시지 작성 도우미
              <br />
              대화를 선택하여 시작하세요
            </p>
          </div>
        )}
      </main>
    </div>
  )
}
