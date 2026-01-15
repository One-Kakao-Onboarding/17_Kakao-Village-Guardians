"use client"

import { type Message, REACTION_EMOJIS } from "@/lib/types"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Clock, Shield, Smile } from "lucide-react"
import { useState } from "react"

interface MessageBubbleProps {
  message: Message
  onAddReaction: (messageId: string, emoji: string) => void
  roomAvatar?: string
  roomName?: string
}

export function MessageBubble({ message, onAddReaction, roomAvatar, roomName }: MessageBubbleProps) {
  const [showReactions, setShowReactions] = useState(false)
  const isMe = message.sender === "me"

  const formatTime = (date: Date) => {
    return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })
  }

  if (message.isEmoticon) {
    return (
      <div className={cn("flex gap-2 group", isMe ? "flex-row-reverse" : "flex-row")}>
        {/* 상대방 프로필 */}
        {!isMe && (
          <Avatar className="h-9 w-9 shrink-0">
            <AvatarImage src={roomAvatar || "/placeholder.svg"} alt={roomName} />
            <AvatarFallback className="bg-gray-300 text-gray-600 text-sm">{roomName?.[0]}</AvatarFallback>
          </Avatar>
        )}
        <div className={cn("flex flex-col", isMe ? "items-end" : "items-start")}>
          {/* 상대방 이름 */}
          {!isMe && <span className="text-xs text-foreground/70 mb-1 ml-1">{roomName}</span>}
          <div className="flex items-end gap-1">
            {/* 내 메시지: 시간 + 읽음 표시가 왼쪽 */}
            {isMe && (
              <div className="flex flex-col items-end text-xs text-foreground/50 mr-1">
                {message.isRead === false && <span className="text-[10px]">1</span>}
                <span>{formatTime(message.timestamp)}</span>
              </div>
            )}
            <img src={message.content || "/placeholder.svg"} alt="이모티콘" className="w-28 h-28 object-contain" />
            {/* 상대방 메시지: 시간이 오른쪽 */}
            {!isMe && <span className="text-xs text-foreground/50 ml-1">{formatTime(message.timestamp)}</span>}
          </div>
          {message.reactions && message.reactions.length > 0 && (
            <div className="flex gap-1 mt-1">
              {message.reactions.map((emoji, idx) => (
                <span key={idx} className="px-2 py-0.5 bg-white rounded-full text-sm shadow-sm">
                  {emoji}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className={cn("flex gap-2 group", isMe ? "flex-row-reverse" : "flex-row")}>
      {/* 상대방 프로필 */}
      {!isMe && (
        <Avatar className="h-9 w-9 shrink-0">
          <AvatarImage src={roomAvatar || "/placeholder.svg"} alt={roomName} />
          <AvatarFallback className="bg-gray-300 text-gray-600 text-sm">{roomName?.[0]}</AvatarFallback>
        </Avatar>
      )}
      <div className={cn("flex flex-col max-w-[70%]", isMe ? "items-end" : "items-start")}>
        {/* 상대방 이름 */}
        {!isMe && <span className="text-xs text-foreground/70 mb-1 ml-1">{roomName}</span>}

        <div className={cn("flex items-end gap-1", isMe ? "flex-row-reverse" : "flex-row")}>
          <div
            className={cn(
              "px-3 py-2 rounded-xl relative",
              isMe ? "bg-[#FEE500] text-[#1A1A1A] rounded-br-sm" : "bg-white text-[#1A1A1A] rounded-bl-sm shadow-sm",
            )}
          >
            {message.wasGuarded && (
              <div className="flex items-center gap-1 text-xs opacity-70 mb-1">
                <Shield className="h-3 w-3" />
                <span>순화된 메시지</span>
              </div>
            )}
            <p className="text-sm leading-relaxed">{message.content}</p>
            {message.originalContent && message.originalContent !== message.content && (
              <p className="text-xs opacity-60 mt-1 line-through">원문: {message.originalContent}</p>
            )}
            {message.isScheduled && message.scheduledTime && (
              <div className="flex items-center gap-1 text-xs opacity-70 mt-1">
                <Clock className="h-3 w-3" />
                <span>
                  {message.scheduledTime.toLocaleString("ko-KR", {
                    month: "short",
                    day: "numeric",
                    hour: "2-digit",
                    minute: "2-digit",
                  })}{" "}
                  예약
                </span>
              </div>
            )}
          </div>

          <div className={cn("flex flex-col text-xs text-foreground/50", isMe ? "items-end" : "items-start")}>
            <span>{formatTime(message.timestamp)}</span>
          </div>

          {/* 리액션 버튼 */}
          <Popover open={showReactions} onOpenChange={setShowReactions}>
            <PopoverTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6 opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <Smile className="h-4 w-4" />
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-2" side={isMe ? "left" : "right"}>
              <div className="flex gap-1">
                {REACTION_EMOJIS.map((emoji) => (
                  <button
                    key={emoji}
                    onClick={() => {
                      onAddReaction(message.id, emoji)
                      setShowReactions(false)
                    }}
                    className="p-1.5 hover:bg-muted rounded-md transition-colors text-lg"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            </PopoverContent>
          </Popover>
        </div>

        {message.reactions && message.reactions.length > 0 && (
          <div className="flex gap-1 mt-1">
            {message.reactions.map((emoji, idx) => (
              <span key={idx} className="px-2 py-0.5 bg-white rounded-full text-sm shadow-sm">
                {emoji}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
