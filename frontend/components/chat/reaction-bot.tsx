"use client"

import { useState, useEffect, useMemo } from "react"
import { Button } from "@/components/ui/button"
import { Bot, X, Sparkles, Zap } from "lucide-react"
import { cn } from "@/lib/utils"
import { analyzeMessageEmotion } from "@/lib/chat-utils"
import type { ChatRoom } from "@/lib/types"

interface ReactionBotProps {
  lastMessage?: string
  suggestedReactions: string[]
  suggestedQuickReplies: string[]
  onReact: (emoji: string) => void
  onQuickReply: (text: string) => void
  onDismiss: () => void
  isVisible: boolean
  room?: ChatRoom
}

export function ReactionBot({
  lastMessage,
  suggestedReactions,
  suggestedQuickReplies,
  onReact,
  onQuickReply,
  onDismiss,
  isVisible,
  room,
}: ReactionBotProps) {
  const [show, setShow] = useState(false)

  useEffect(() => {
    if (isVisible && lastMessage) {
      const timer = setTimeout(() => setShow(true), 500)
      return () => clearTimeout(timer)
    } else {
      setShow(false)
    }
  }, [isVisible, lastMessage])

  const emotion = useMemo(() => {
    if (!lastMessage) return "neutral"
    return analyzeMessageEmotion(lastMessage)
  }, [lastMessage])

  const emotionLabel = useMemo(() => {
    switch (emotion) {
      case "positive":
        return "긍정적"
      case "negative":
        return "위로가 필요해요"
      case "surprise":
        return "놀라운 소식"
      case "congratulation":
        return "축하할 일이에요"
      case "support":
        return "응원 메시지"
      default:
        return null
    }
  }, [emotion])

  if (!show || !lastMessage) return null

  return (
    <div
      className={cn(
        "mb-3 p-3 rounded-xl border border-[#FEE500]/30 bg-[#FEE500]/10",
        "animate-in slide-in-from-bottom-2 fade-in duration-300",
      )}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-1.5 text-xs text-[#3C1E1E]">
          <Bot className="h-3.5 w-3.5" />
          <span className="font-medium">리액션 봇</span>
          <Sparkles className="h-3 w-3 text-[#FEE500]" />
          {emotionLabel && (
            <span className="ml-1 px-1.5 py-0.5 bg-[#FEE500]/30 rounded text-[10px]">{emotionLabel}</span>
          )}
        </div>
        <Button variant="ghost" size="icon" className="h-5 w-5" onClick={onDismiss}>
          <X className="h-3 w-3" />
        </Button>
      </div>

      <div className="space-y-2">
        {/* 리액션 이모지 - 감정에 따라 정렬됨 */}
        <div className="flex items-center gap-2">
          <span className="text-[10px] text-gray-500 w-12">이모지</span>
          <div className="flex gap-1">
            {suggestedReactions.slice(0, 4).map((emoji) => (
              <button
                key={emoji}
                onClick={() => onReact(emoji)}
                className="p-1.5 hover:bg-[#FEE500]/30 rounded-md transition-all text-lg hover:scale-110 active:scale-95"
              >
                {emoji}
              </button>
            ))}
          </div>
        </div>

        {suggestedQuickReplies.length > 0 && (
          <div className="flex items-center gap-2">
            <span className="text-[10px] text-gray-500 w-12 flex items-center gap-0.5">
              <Zap className="h-2.5 w-2.5" />
              빠른응답
            </span>
            <div className="flex gap-1.5 flex-wrap">
              {suggestedQuickReplies.slice(0, 2).map((reply, idx) => (
                <Button
                  key={idx}
                  variant="secondary"
                  size="sm"
                  className="text-xs h-6 px-2 bg-[#FEE500]/20 hover:bg-[#FEE500]/40 border-0"
                  onClick={() => onQuickReply(reply)}
                >
                  {reply.length > 15 ? reply.substring(0, 15) + "..." : reply}
                </Button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
