import { useState, useCallback } from 'react'
import { aiApi } from '@/lib/api/services'
import type { ChatRoom, Persona } from '@/lib/types'

export function useAiTransform() {
  const [isTransforming, setIsTransforming] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const transform = useCallback(
    async (text: string, persona: Persona, room: ChatRoom) => {
      if (!text.trim()) return text

      setIsTransforming(true)
      setError(null)

      try {
        const result = await aiApi.transform({
          text,
          personaId: persona.id,
          formalityLevel: room.formalityLevel,
          relationship: room.relationship,
          roomKeywords: room.keywords,
        })
        return result.transformedText
      } catch (err: any) {
        console.error('Transform error:', err)
        setError(err.message || 'Failed to transform message')
        // Fallback to original text
        return text
      } finally {
        setIsTransforming(false)
      }
    },
    []
  )

  const checkEmotionGuard = useCallback(async (text: string) => {
    if (!text.trim()) {
      return {
        isAggressive: false,
        aggressionScore: 0,
      }
    }

    try {
      const result = await aiApi.emotionGuard({ text })
      return result
    } catch (err: any) {
      console.error('Emotion guard error:', err)
      return {
        isAggressive: false,
        aggressionScore: 0,
      }
    }
  }, [])

  const getSuggestions = useCallback(
    async (message: string, room: ChatRoom) => {
      if (!message.trim()) return null

      try {
        const result = await aiApi.reactionSuggest({
          message,
          relationship: room.relationship,
          formalityLevel: room.formalityLevel,
        })
        return result
      } catch (err: any) {
        console.error('Reaction suggest error:', err)
        return null
      }
    },
    []
  )

  return {
    transform,
    checkEmotionGuard,
    getSuggestions,
    isTransforming,
    error,
  }
}
