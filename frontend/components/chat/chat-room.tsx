"use client"

import { useState, useRef, useEffect, useCallback } from "react"
import type { Message, ChatRoom as ChatRoomType, Persona } from "@/lib/types"
import { MessageBubble } from "./message-bubble"
import { MessageInput } from "./message-input"
import { FormalityGauge } from "./formality-gauge"
import { PersonaSelector } from "./persona-selector"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { ArrowLeft, Search, Menu, Trash2, Smile, Sparkles } from "lucide-react"
import { getPersonaByFormalityLevel, calculateFormalityLevel, generateId } from "@/lib/chat-utils"

interface ChatRoomProps {
  room: ChatRoomType
  messages: Message[]
  onBack: () => void
  onSendMessage: (message: Message) => void
  onAddReaction: (messageId: string, emoji: string) => void
  onDeleteRoom?: () => void
}

export function ChatRoom({ room, messages, onBack, onSendMessage, onAddReaction, onDeleteRoom }: ChatRoomProps) {
  const [formalityLevel, setFormalityLevel] = useState(() => {
    const level = calculateFormalityLevel(room)
    return isNaN(level) ? 50 : level
  })
  const [selectedPersona, setSelectedPersona] = useState<Persona>(() => {
    const level = calculateFormalityLevel(room)
    return getPersonaByFormalityLevel(isNaN(level) ? 50 : level)
  })
  const [showReactionBot, setShowReactionBot] = useState(true)
  const [enableAutoTransform, setEnableAutoTransform] = useState(true)
  const recommendedPersona = getPersonaByFormalityLevel(isNaN(formalityLevel) ? 50 : formalityLevel)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const messagesContainerRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  useEffect(() => {
    const newFormalityLevel = calculateFormalityLevel(room)
    const validLevel = isNaN(newFormalityLevel) ? 50 : newFormalityLevel
    setFormalityLevel(validLevel)
    setSelectedPersona(getPersonaByFormalityLevel(validLevel))
  }, [room])

  const handleFormalityChange = (level: number) => {
    setFormalityLevel(level)
    setSelectedPersona(getPersonaByFormalityLevel(level))
  }

  const handlePersonaSelect = (persona: Persona) => {
    setSelectedPersona(persona)
    const midLevel = Math.floor((persona.formalityRange[0] + persona.formalityRange[1]) / 2)
    setFormalityLevel(midLevel)
  }

  const handleSendMessage = (
    content: string,
    options?: {
      isScheduled?: boolean
      scheduledTime?: Date
      originalContent?: string
      wasGuarded?: boolean
      isEmoticon?: boolean
      emoticonId?: string
    },
  ) => {
    const newMessage: Message = {
      id: generateId(),
      content,
      originalContent: options?.originalContent,
      sender: "me",
      timestamp: new Date(),
      isScheduled: options?.isScheduled,
      scheduledTime: options?.scheduledTime,
      wasGuarded: options?.wasGuarded,
      isEmoticon: options?.isEmoticon,
      emoticonId: options?.emoticonId,
    }
    onSendMessage(newMessage)
  }

  const lastReceivedMessage = messages.filter((m) => m.sender === "other").pop()
  const lastReceivedMessageContent = lastReceivedMessage?.isEmoticon ? undefined : lastReceivedMessage?.content

  const handleReactToLastMessage = (emoji: string) => {
    if (lastReceivedMessage) {
      onAddReaction(lastReceivedMessage.id, emoji)
    }
  }

  const handleDeleteRoom = async () => {
    if (confirm("이 채팅방을 삭제하시겠습니까?")) {
      try {
        onDeleteRoom?.()
      } catch (error) {
        console.error("Failed to delete chat room:", error)
        alert("채팅방 삭제에 실패했습니다.")
      }
    }
  }

  return (
    <div className="flex flex-col h-full bg-[#A9BDCE]">
      <header className="flex items-center gap-3 px-4 py-3 bg-[#C5D8E8]">
        <Button variant="ghost" size="icon" onClick={onBack} className="text-gray-700 hover:bg-black/10">
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1 min-w-0 text-center">
          <h2 className="font-semibold text-gray-800">{room.name}</h2>
        </div>

        <div className="flex items-center gap-1">
          <Button variant="ghost" size="icon" className="text-gray-700 hover:bg-black/10">
            <Search className="h-5 w-5" />
          </Button>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="text-gray-700 hover:bg-black/10">
                <Menu className="h-5 w-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => setEnableAutoTransform(!enableAutoTransform)}>
                <Sparkles className="h-4 w-4 mr-2" />
                AI 말투 변환 {enableAutoTransform ? "끄기" : "켜기"}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => setShowReactionBot(!showReactionBot)}>
                <Smile className="h-4 w-4 mr-2" />
                리액션 봇 {showReactionBot ? "숨기기" : "표시"}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={handleDeleteRoom} className="text-destructive focus:text-destructive">
                <Trash2 className="h-4 w-4 mr-2" />
                채팅방 삭제
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <div ref={messagesContainerRef} className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="w-16 h-16 rounded-full bg-white/50 flex items-center justify-center mb-4">
              <span className="text-3xl">{selectedPersona.icon}</span>
            </div>
            <p className="text-foreground/70">{room.name}님과의 대화를 시작하세요</p>
          </div>
        ) : (
          messages.map((message) => (
            <MessageBubble
              key={message.id}
              message={message}
              onAddReaction={onAddReaction}
              roomAvatar={room.avatar}
              roomName={room.name}
            />
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <MessageInput
        room={room}
        persona={selectedPersona}
        onSendMessage={handleSendMessage}
        onReactToLastMessage={handleReactToLastMessage}
        lastReceivedMessage={lastReceivedMessageContent}
        onScrollToBottom={scrollToBottom}
        showReactionBot={showReactionBot}
        onToggleReactionBot={() => setShowReactionBot(!showReactionBot)}
        enableAutoTransform={enableAutoTransform}
        formalityLevel={formalityLevel}
      />
    </div>
  )
}
