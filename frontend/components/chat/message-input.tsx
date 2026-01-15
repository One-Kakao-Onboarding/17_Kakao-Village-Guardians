"use client"

import type React from "react"

import { useState, useCallback, useRef, useMemo, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Send, Sparkles, Clock, Shield, Plus, Smile } from "lucide-react"
import type { ChatRoom, Persona, Emoticon } from "@/lib/types"
import {
  detectAggression,
  softenMessage,
  transformMessage,
  shouldScheduleMessage,
  suggestReactions,
  suggestQuickReplies,
  generateAISuggestion,
} from "@/lib/chat-utils"
import { ReactionBot } from "./reaction-bot"
import { EmoticonPicker } from "./emoticon-picker"
import { cn } from "@/lib/utils"
import { aiApi } from "@/lib/api/services"
import type { TransformResponse } from "@/lib/api/types"

interface MessageInputProps {
  room: ChatRoom
  persona: Persona
  onSendMessage: (
    content: string,
    options?: {
      isScheduled?: boolean
      scheduledTime?: Date
      originalContent?: string
      wasGuarded?: boolean
      isEmoticon?: boolean
      emoticonId?: string
    },
  ) => void
  onReactToLastMessage?: (emoji: string) => void
  lastReceivedMessage?: string
  onScrollToBottom?: () => void
  showReactionBot?: boolean
  onToggleReactionBot?: () => void
  enableAutoTransform?: boolean
  formalityLevel?: number
}

export function MessageInput({
  room,
  persona,
  onSendMessage,
  onReactToLastMessage,
  lastReceivedMessage,
  onScrollToBottom,
  showReactionBot = true,
  onToggleReactionBot,
  enableAutoTransform = false,
  formalityLevel = 50,
}: MessageInputProps) {
  const [content, setContent] = useState("")
  const [showScheduleDialog, setShowScheduleDialog] = useState(false)
  const [scheduledTime, setScheduledTime] = useState<Date | null>(null)
  const [scheduleReason, setScheduleReason] = useState<string>("")
  const [showEmoticonPicker, setShowEmoticonPicker] = useState(false)
  const [isAggressionFlashing, setIsAggressionFlashing] = useState(false)
  const [showTransformSuggestion, setShowTransformSuggestion] = useState(false)
  const [transformResult, setTransformResult] = useState<TransformResponse | null>(null)
  const [isTransforming, setIsTransforming] = useState(false)
  const isSendingRef = useRef(false)
  const isComposingRef = useRef(false)

  const aiSuggestion = useMemo(() => {
    if (!content.trim()) return null
    return generateAISuggestion(content, persona, room)
  }, [content, persona, room])

  const [reactionSuggestions, setReactionSuggestions] = useState<{
    suggestedEmojis: string[]
    quickResponses: Array<{ text: string; icon: string }>
  }>({
    suggestedEmojis: [],
    quickResponses: [],
  })

  // API를 통한 리액션 추천
  useEffect(() => {
    if (lastReceivedMessage && showReactionBot) {
      const fetchReactionSuggestions = async () => {
        try {
          const result = await aiApi.reactionSuggest({
            message: lastReceivedMessage,
            relationship: room.relationship,
            formalityLevel: formalityLevel || 50,
          })
          console.log('[Reaction Suggest] API response:', result)
          setReactionSuggestions({
            suggestedEmojis: result.suggestedEmojis || [],
            quickResponses: result.quickResponses || [],
          })
        } catch (error) {
          console.error('Failed to fetch reaction suggestions:', error)
          // 폴백: 로컬 함수 사용
          setReactionSuggestions({
            suggestedEmojis: suggestReactions(lastReceivedMessage, room),
            quickResponses: [],
          })
        }
      }
      fetchReactionSuggestions()
    }
  }, [lastReceivedMessage, room, formalityLevel, showReactionBot])

  const suggestedReactions = reactionSuggestions.suggestedEmojis
  const suggestedQuickRepliesArr = reactionSuggestions.quickResponses.map((r) => r.text)

  const handleContentChange = useCallback((value: string) => {
    setContent(value)

    // 공격성 감지 시 플래시 효과
    if (value.trim()) {
      const { isAggressive } = detectAggression(value)
      if (isAggressive) {
        setIsAggressionFlashing(true)
        setTimeout(() => setIsAggressionFlashing(false), 2000)
      } else {
        setIsAggressionFlashing(false)
      }
    } else {
      setIsAggressionFlashing(false)
    }
  }, [])

  const handleSend = useCallback(async () => {
    if (isSendingRef.current || !content.trim() || isComposingRef.current) return
    isSendingRef.current = true

    console.log('[handleSend] Started with enableAutoTransform:', enableAutoTransform)

    // AI 말투 변환 체크 (공격성 감지 시에도 동일한 변환 로직 사용)
    let finalText = content
    if (enableAutoTransform && !isTransforming) {
      try {
        setIsTransforming(true)
        console.log('[AI Transform] Calling API with:', {
          text: content,
          personaId: persona.id,
          formalityLevel,
          relationship: room.relationship,
          roomId: room.id,
        })

        const result = await aiApi.transform({
          text: content,
          personaId: persona.id,
          formalityLevel: formalityLevel,
          relationship: room.relationship,
          roomId: room.id,
        })

        console.log('[AI Transform] API response:', result)
        setTransformResult(result)

        // 원본과 변환된 텍스트가 다른 경우 항상 제안
        if (result.transformedText !== content) {
          console.log('[AI Transform] Showing suggestion dialog')
          setShowTransformSuggestion(true)
          isSendingRef.current = false
          setIsTransforming(false)
          return
        }
      } catch (error) {
        console.error('[AI Transform] Failed to transform message:', error)
        // 변환 실패 시 원본 메시지로 계속 진행
      } finally {
        setIsTransforming(false)
      }
    } else {
      console.log('[AI Transform] Skipped - enableAutoTransform:', enableAutoTransform, 'isTransforming:', isTransforming)
    }

    const scheduleCheck = shouldScheduleMessage(room)
    if (scheduleCheck.should && scheduleCheck.suggestedTime) {
      setScheduledTime(scheduleCheck.suggestedTime)
      setScheduleReason(scheduleCheck.reason || "")
      setShowScheduleDialog(true)
      isSendingRef.current = false
      return
    }

    console.log('[handleSend] Sending with finalText:', finalText)
    sendFinalMessage(finalText)
  }, [content, room, enableAutoTransform, isTransforming, persona, formalityLevel])

  const sendFinalMessage = useCallback(
    (
      finalContent: string,
      options?: {
        isScheduled?: boolean
        scheduledTime?: Date
        wasGuarded?: boolean
        isEmoticon?: boolean
        emoticonId?: string
      },
    ) => {
      onSendMessage(finalContent, {
        ...options,
        originalContent: content !== finalContent ? content : undefined,
      })
      // 상태 초기화
      setContent("")
      setShowScheduleDialog(false)
      setIsAggressionFlashing(false)
      isSendingRef.current = false
      setTimeout(() => onScrollToBottom?.(), 100)
    },
    [content, onSendMessage, onScrollToBottom],
  )

  const handleUseSuggestion = useCallback(() => {
    if (aiSuggestion) {
      setContent(aiSuggestion)
    }
  }, [aiSuggestion])

  const handleReactionBotReact = useCallback(
    (emoji: string) => {
      onReactToLastMessage?.(emoji)
    },
    [onReactToLastMessage],
  )

  const handleReactionBotQuickReply = useCallback(
    (text: string) => {
      const transformed = transformMessage(text, persona, room)
      sendFinalMessage(transformed)
    },
    [persona, room, sendFinalMessage],
  )

  const handleEmoticonSelect = useCallback(
    (emoticon: Emoticon) => {
      sendFinalMessage(emoticon.imageUrl, {
        isEmoticon: true,
        emoticonId: emoticon.id,
      })
      setShowEmoticonPicker(false)
    },
    [sendFinalMessage],
  )

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (e.nativeEvent.isComposing || isComposingRef.current) return

      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault()
        handleSend()
      }
    },
    [handleSend],
  )

  const handleCompositionStart = useCallback(() => {
    isComposingRef.current = true
  }, [])

  const handleCompositionEnd = useCallback(() => {
    isComposingRef.current = false
  }, [])

  return (
    <div className="bg-white p-2">
      {showReactionBot && lastReceivedMessage && !content && (
        <ReactionBot
          lastMessage={lastReceivedMessage}
          suggestedReactions={suggestedReactions}
          suggestedQuickReplies={suggestedQuickRepliesArr}
          onReact={handleReactionBotReact}
          onQuickReply={handleReactionBotQuickReply}
          onDismiss={onToggleReactionBot}
          isVisible={true}
          room={room}
        />
      )}

      {aiSuggestion && (
        <div className="mb-2 p-2 bg-[#FEE500]/20 rounded-lg border border-[#FEE500]/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm text-[#3C1E1E]">
              <div className="flex items-center gap-1 px-2 py-0.5 bg-[#FEE500] rounded-full">
                <Sparkles className="h-3 w-3" />
                <span className="text-xs font-medium">AI 제안</span>
              </div>
              <p className="text-sm">{aiSuggestion}</p>
            </div>
            <Button
              size="sm"
              variant="ghost"
              onClick={handleUseSuggestion}
              className="text-xs text-[#3C1E1E] hover:bg-[#FEE500]/30"
            >
              입력창에 적용
            </Button>
          </div>
        </div>
      )}

      <div className="flex items-end gap-2">
        <Button variant="ghost" size="icon" className="shrink-0 h-10 w-10 text-gray-500 hover:bg-gray-100">
          <Plus className="h-6 w-6" />
        </Button>

        <div
          className={cn(
            "flex-1 relative bg-[#F5F5F5] rounded-full px-4 py-2 transition-all",
            isAggressionFlashing && "ring-2 ring-red-500 animate-pulse",
          )}
        >
          <Textarea
            value={content}
            onChange={(e) => handleContentChange(e.target.value)}
            placeholder="메시지를 입력하세요"
            className="min-h-[24px] max-h-[80px] resize-none border-0 bg-transparent p-0 focus-visible:ring-0 text-sm"
            onKeyDown={handleKeyDown}
            onCompositionStart={handleCompositionStart}
            onCompositionEnd={handleCompositionEnd}
          />
        </div>

        <div className="relative">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="shrink-0 h-10 w-10 text-gray-500 hover:bg-gray-100"
            onClick={() => setShowEmoticonPicker(!showEmoticonPicker)}
          >
            <Smile className="h-6 w-6" />
          </Button>

          {showEmoticonPicker && (
            <div className="absolute bottom-12 right-0 z-50 bg-white rounded-lg shadow-lg border">
              <EmoticonPicker onSelect={handleEmoticonSelect} />
            </div>
          )}
        </div>

        <Button
          type="button"
          onClick={handleSend}
          disabled={!content.trim() || isTransforming}
          size="icon"
          className="shrink-0 h-10 w-10 rounded-full bg-[#FEE500] hover:bg-[#FDD800] text-[#3C1E1E] disabled:bg-gray-200 disabled:text-gray-400"
        >
          {isTransforming ? (
            <Sparkles className="h-5 w-5 animate-pulse" />
          ) : (
            <Send className="h-5 w-5" />
          )}
        </Button>
      </div>

      <Dialog open={showTransformSuggestion} onOpenChange={setShowTransformSuggestion}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-[#FEE500]" />
              AI가 말투를 다듬었어요
            </DialogTitle>
            <DialogDescription>
              {transformResult?.suggestionReason || "더 적절한 표현으로 변환했습니다."}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="p-3 bg-muted/50 rounded-lg border">
              <p className="text-sm font-medium mb-1 text-muted-foreground">원본 메시지:</p>
              <p className="text-sm">{transformResult?.originalText}</p>
            </div>
            <div className="p-3 bg-[#FEE500]/10 rounded-lg border border-[#FEE500]/30">
              <p className="text-sm font-medium mb-1 text-[#3C1E1E]">변환된 메시지:</p>
              <p className="text-sm text-gray-700">{transformResult?.transformedText}</p>
            </div>
            {transformResult?.changes && transformResult.changes.length > 0 && (
              <div className="p-3 bg-blue-50 rounded-lg border border-blue-200">
                <p className="text-sm font-medium mb-2 text-blue-600">변경 사항:</p>
                <ul className="space-y-1">
                  {transformResult.changes.map((change, index) => (
                    <li key={index} className="text-xs text-gray-600">
                      • {change.description}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
          <DialogFooter className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => {
                setShowTransformSuggestion(false)
                sendFinalMessage(content)
              }}
            >
              원본 보내기
            </Button>
            <Button
              className="bg-[#FEE500] hover:bg-[#FDD800] text-[#3C1E1E]"
              onClick={() => {
                setShowTransformSuggestion(false)
                sendFinalMessage(transformResult?.transformedText || content)
              }}
            >
              <Sparkles className="h-4 w-4 mr-2" />
              변환된 메시지 보내기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={showScheduleDialog} onOpenChange={setShowScheduleDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-[#FEE500]" />
              예약 발송을 추천드려요
            </DialogTitle>
            <DialogDescription>
              {scheduleReason}{" "}
              {scheduledTime &&
                `${scheduledTime.toLocaleDateString("ko-KR", { weekday: "long" })} 오전 ${scheduledTime.getHours()}시에 예약 발송하시겠어요?`}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => {
                setShowScheduleDialog(false)
                sendFinalMessage(transformedContent || content)
              }}
            >
              지금 보내기
            </Button>
            <Button
              className="bg-[#FEE500] hover:bg-[#FDD800] text-[#3C1E1E]"
              onClick={() => {
                sendFinalMessage(transformedContent || content, {
                  isScheduled: true,
                  scheduledTime: scheduledTime!,
                })
              }}
            >
              예약 발송하기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
