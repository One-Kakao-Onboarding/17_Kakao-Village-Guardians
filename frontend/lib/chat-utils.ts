import { PERSONAS, type Persona, type ChatRoom } from "./types"

export function detectAggression(text: string): {
  isAggressive: boolean
  type?: string
  confidence: number
  suggestedReplacement?: string
} {
  // 어미 기반 패턴 분석
  const endingPatterns = [
    {
      pattern: /왜\s*그러니\??$/i,
      type: "비꼬기",
      weight: 0.85,
      replacement: "혹시 무슨 일 있나요? 제가 도와드릴 게 있을까요?",
    },
    { pattern: /왜\s*그래\??$/i, type: "비꼬기", weight: 0.8, replacement: "어떤 상황인지 여쭤봐도 될까요?" },
    { pattern: /뭐\s*하냐\??$/i, type: "공격성", weight: 0.75, replacement: "지금 어떤 일을 하고 계신가요?" },
    { pattern: /왜\s*이래\??$/i, type: "비꼬기", weight: 0.8, replacement: "무슨 일이 있으신 건가요?" },
    { pattern: /(니|냐)\??$/i, type: "반말 어미", weight: 0.6, replacement: null },
    { pattern: /참나$/i, type: "짜증", weight: 0.7, replacement: "조금 어려운 상황이네요." },
    { pattern: /됐어$/i, type: "거부", weight: 0.65, replacement: "괜찮습니다. 제가 다시 확인해볼게요." },
    { pattern: /아\s*됐어$/i, type: "수동적 공격", weight: 0.8, replacement: "괜찮습니다. 다음에 다시 말씀해 주세요." },
  ]

  // 전체 문장 패턴 분석
  const sentencePatterns = [
    {
      pattern: /참\s*잘\s*한다/gi,
      type: "비꼬기",
      weight: 0.9,
      replacement: "다음에는 조금 더 신경 써주시면 감사하겠습니다.",
    },
    { pattern: /잘\s*하시네요?/gi, type: "비꼬기", weight: 0.85, replacement: "노력해 주셔서 감사합니다." },
    { pattern: /대단하시네요?/gi, type: "비꼬기", weight: 0.85, replacement: "수고하셨습니다." },
    {
      pattern: /너\s*왜\s*그러니/gi,
      type: "비꼬기",
      weight: 0.9,
      replacement: "혹시 무슨 일 있나요? 제가 도와드릴 게 있을까요?",
    },
    { pattern: /뭐야\s*이게/gi, type: "공격성", weight: 0.8, replacement: "이 부분은 어떻게 된 건가요?" },
    { pattern: /했잖아/gi, type: "수동적 공격", weight: 0.75, replacement: "말씀드렸던 것처럼" },
    { pattern: /말했잖아/gi, type: "수동적 공격", weight: 0.8, replacement: "앞서 말씀드렸듯이" },
    { pattern: /알아서\s*해/gi, type: "수동적 공격", weight: 0.75, replacement: "편하신 대로 진행해 주세요." },
    { pattern: /마음대로/gi, type: "수동적 공격", weight: 0.7, replacement: "원하시는 대로 해주세요." },
    { pattern: /그러시든지/gi, type: "수동적 공격", weight: 0.75, replacement: "네, 알겠습니다." },
    { pattern: /답답해/gi, type: "감정 표현", weight: 0.8, replacement: "조금 더 설명이 필요할 것 같아요." },
    { pattern: /짜증나/gi, type: "감정 표현", weight: 0.85, replacement: "조금 어려운 상황이네요." },
    { pattern: /화[가나]/gi, type: "감정 표현", weight: 0.85, replacement: "아쉬운 점이 있어요." },
  ]

  let maxConfidence = 0
  let detectedType: string | undefined
  let suggestedReplacement: string | undefined

  // 어미 패턴 먼저 체크
  for (const { pattern, type, weight, replacement } of endingPatterns) {
    if (pattern.test(text)) {
      if (weight > maxConfidence) {
        maxConfidence = weight
        detectedType = type
        if (replacement) suggestedReplacement = replacement
      }
    }
  }

  // 전체 문장 패턴 체크
  for (const { pattern, type, weight, replacement } of sentencePatterns) {
    if (pattern.test(text)) {
      if (weight > maxConfidence) {
        maxConfidence = weight
        detectedType = type
        if (replacement) suggestedReplacement = replacement
      }
    }
  }

  return {
    isAggressive: maxConfidence >= 0.6,
    type: detectedType,
    confidence: maxConfidence,
    suggestedReplacement,
  }
}

export function transformMessage(text: string, persona: Persona, room?: ChatRoom): string {
  let result = text
  const formalityLevel = room ? calculateFormalityLevel(room) : 50

  // 문장 시작 변환 - 격식 지수에 따라 다르게 적용
  const startTransforms: Record<string, Array<{ pattern: RegExp; replacement: string }>> = {
    "very-formal": [
      { pattern: /^응\.?\s*/i, replacement: "네, " },
      { pattern: /^ㅇㅇ\.?\s*/i, replacement: "네, 확인했습니다. " },
      { pattern: /^오케이\.?\s*/i, replacement: "네, 알겠습니다. " },
      { pattern: /^알겠어\.?\s*/i, replacement: "네, 알겠습니다. " },
      { pattern: /^알았어\.?\s*/i, replacement: "네, 알겠습니다. " },
      { pattern: /^그래\.?\s*/i, replacement: "네, 그렇게 하겠습니다. " },
      { pattern: /^ㅇㅋ\.?\s*/i, replacement: "네, 알겠습니다. " },
    ],
    formal: [
      { pattern: /^응\.?\s*/i, replacement: "네, " },
      { pattern: /^ㅇㅇ\.?\s*/i, replacement: "네, " },
      { pattern: /^오케이\.?\s*/i, replacement: "네, 확인했습니다. " },
      { pattern: /^알겠어\.?\s*/i, replacement: "네, 알겠습니다. " },
      { pattern: /^알았어\.?\s*/i, replacement: "네, 알겠습니다. " },
    ],
    "casual-polite": [
      { pattern: /^응\.?\s*/i, replacement: "네~ " },
      { pattern: /^ㅇㅇ\.?\s*/i, replacement: "네~ " },
    ],
    casual: [
      { pattern: /^네,?\s*/i, replacement: "응 " },
      { pattern: /^알겠습니다\.?\s*/i, replacement: "알겠어~ " },
    ],
    "very-casual": [
      { pattern: /^네,?\s*/i, replacement: "ㅇㅇ " },
      { pattern: /^알겠습니다\.?\s*/i, replacement: "ㅇㅋ " },
    ],
  }

  // 문장 끝 변환 - 격식 지수에 따라 다르게 적용
  const endTransforms: Record<string, Array<{ pattern: RegExp; replacement: string }>> = {
    "very-formal": [
      { pattern: /줄게\.?$/i, replacement: "드리겠습니다." },
      { pattern: /볼게\.?$/i, replacement: "보겠습니다." },
      { pattern: /할게\.?$/i, replacement: "하겠습니다." },
      { pattern: /갈게\.?$/i, replacement: "가겠습니다." },
      { pattern: /올게\.?$/i, replacement: "오겠습니다." },
      { pattern: /보낼게\.?$/i, replacement: "보내드리겠습니다." },
      { pattern: /연락할게\.?$/i, replacement: "연락드리겠습니다." },
      { pattern: /확인할게\.?$/i, replacement: "확인하겠습니다." },
      { pattern: /처리할게\.?$/i, replacement: "처리하겠습니다." },
      { pattern: /전달할게\.?$/i, replacement: "전달드리겠습니다." },
      { pattern: /고마워\.?$/i, replacement: "감사합니다." },
      { pattern: /미안\.?$/i, replacement: "죄송합니다." },
      { pattern: /미안해\.?$/i, replacement: "죄송합니다." },
      { pattern: /중에\s*줄게\.?$/i, replacement: "중에 드리겠습니다." },
      { pattern: /중으로\s*줄게\.?$/i, replacement: "중으로 드리겠습니다." },
    ],
    formal: [
      { pattern: /줄게\.?$/i, replacement: "드릴게요." },
      { pattern: /볼게\.?$/i, replacement: "볼게요." },
      { pattern: /할게\.?$/i, replacement: "할게요." },
      { pattern: /갈게\.?$/i, replacement: "갈게요." },
      { pattern: /올게\.?$/i, replacement: "올게요." },
      { pattern: /보낼게\.?$/i, replacement: "보내드릴게요." },
      { pattern: /연락할게\.?$/i, replacement: "연락드릴게요." },
      { pattern: /고마워\.?$/i, replacement: "감사합니다." },
      { pattern: /미안\.?$/i, replacement: "죄송해요." },
      { pattern: /중에\s*줄게\.?$/i, replacement: "중에 드릴게요." },
    ],
    "casual-polite": [
      { pattern: /줄게\.?$/i, replacement: "줄게요~" },
      { pattern: /할게\.?$/i, replacement: "할게요~" },
      { pattern: /고마워\.?$/i, replacement: "고마워요!" },
    ],
    casual: [
      { pattern: /드리겠습니다\.?$/i, replacement: "줄게~" },
      { pattern: /하겠습니다\.?$/i, replacement: "할게~" },
    ],
    "very-casual": [
      { pattern: /드리겠습니다\.?$/i, replacement: "줌ㅋ" },
      { pattern: /하겠습니다\.?$/i, replacement: "함ㅋ" },
    ],
  }

  // 시작 변환 적용
  const startList = startTransforms[persona.id] || []
  for (const { pattern, replacement } of startList) {
    result = result.replace(pattern, replacement)
  }

  // 끝 변환 적용
  const endList = endTransforms[persona.id] || []
  for (const { pattern, replacement } of endList) {
    result = result.replace(pattern, replacement)
  }

  return result
}

export function softenMessage(text: string): string {
  const softeningMap: Array<{ pattern: RegExp; replacement: string }> = [
    { pattern: /너\s*왜\s*그러니\??/gi, replacement: "혹시 무슨 일 있나요? 제가 도와드릴 게 있을까요?" },
    { pattern: /참\s*잘\s*한다/gi, replacement: "조금 더 신경 써주시면 감사하겠습니다." },
    { pattern: /잘\s*하시네요?/gi, replacement: "노력해 주셔서 감사합니다." },
    { pattern: /대단하시네요?/gi, replacement: "수고하셨습니다." },
    { pattern: /아\s*됐어/gi, replacement: "괜찮습니다. 다음에 다시 말씀해 주세요." },
    { pattern: /뭐야/gi, replacement: "어떻게 된 건가요?" },
    { pattern: /왜\s*안\s*해/gi, replacement: "혹시 진행이 어려우신 부분이 있으신가요?" },
    { pattern: /했잖아/gi, replacement: "말씀드렸던 것처럼" },
    { pattern: /말했잖아/gi, replacement: "앞서 말씀드렸듯이" },
    { pattern: /몰라/gi, replacement: "확인이 필요할 것 같아요" },
    { pattern: /알아서\s*해/gi, replacement: "편하신 대로 진행해 주세요" },
    { pattern: /답답해/gi, replacement: "조금 더 논의가 필요할 것 같아요" },
    { pattern: /짜증나/gi, replacement: "조금 어려운 상황이네요" },
    { pattern: /화[가나]/gi, replacement: "아쉬운 점이 있어요" },
    { pattern: /왜\s*이래/gi, replacement: "어떤 상황인지 여쭤봐도 될까요?" },
    { pattern: /마음대로/gi, replacement: "원하시는 대로" },
    { pattern: /그러시든지/gi, replacement: "네, 알겠습니다" },
  ]

  let result = text

  for (const { pattern, replacement } of softeningMap) {
    result = result.replace(pattern, replacement)
  }

  return result
}

export function generateAISuggestion(text: string, persona: Persona, room: ChatRoom): string | null {
  const trimmedText = text.trim()
  const formalityLevel = calculateFormalityLevel(room)
  const relationship = room.relationship

  // 상사/선배 대상 정중함 모드
  const formalSuggestions: Record<string, string> = {
    알겠어: "네, 확인했습니다. 말씀하신 내용 반영하여 진행하겠습니다.",
    알았어: "네, 확인했습니다. 말씀하신 대로 처리하겠습니다.",
    응: "네, 알겠습니다.",
    ㅇㅇ: "네, 확인했습니다.",
    오케이: "네, 알겠습니다. 바로 진행하겠습니다.",
    ㅇㅋ: "네, 알겠습니다.",
    그래: "네, 그렇게 하겠습니다.",
    고마워: "감사합니다.",
    미안: "죄송합니다.",
    확인: "확인했습니다.",
    좋아: "네, 좋습니다. 진행하겠습니다.",
  }

  // 친구/가족 대상 친근함 모드
  const casualSuggestions: Record<string, string> = {
    네: "ㅇㅇ",
    알겠습니다: "ㅇㅋㅇㅋ 알겠어~",
    확인했습니다: "확인~",
    감사합니다: "ㅋㅋ 고마워!",
  }

  // 격식 지수에 따른 제안 선택
  if (formalityLevel >= 70 && (persona.id === "very-formal" || persona.id === "formal")) {
    for (const [key, value] of Object.entries(formalSuggestions)) {
      if (trimmedText.toLowerCase() === key.toLowerCase() || trimmedText === key) {
        return value
      }
    }
  } else if (formalityLevel <= 30 && (persona.id === "very-casual" || persona.id === "casual")) {
    for (const [key, value] of Object.entries(casualSuggestions)) {
      if (trimmedText.toLowerCase() === key.toLowerCase() || trimmedText === key) {
        return value
      }
    }
  }

  // 기본 제안 (짧은 입력)
  const shortInputs: Record<string, Record<string, string>> = {
    "very-formal": formalSuggestions,
    formal: {
      알겠어: "네, 알겠습니다.",
      알았어: "네, 알겠습니다.",
      응: "네, 확인했습니다.",
      ㅇㅇ: "네",
      오케이: "네, 확인했습니다.",
      ㅇㅋ: "네, 알겠습니다.",
      그래: "네, 그렇게 할게요.",
      고마워: "감사합니다!",
      미안: "죄송해요.",
    },
    "casual-polite": {
      알겠어: "네, 알겠어요~",
      응: "네~",
      오케이: "네, 알겠어요!",
      그래: "네, 그럴게요!",
    },
    casual: casualSuggestions,
    "very-casual": {
      네: "ㅇㅇ",
      알겠습니다: "ㅇㅋㅇㅋ",
    },
  }

  const suggestions = shortInputs[persona.id]
  if (suggestions) {
    for (const [key, value] of Object.entries(suggestions)) {
      if (trimmedText.toLowerCase() === key.toLowerCase() || trimmedText === key) {
        return value
      }
    }
  }

  // 문장 전체 변환을 제안 (정중함 모드일 때)
  if (trimmedText.length > 2 && (persona.id === "very-formal" || persona.id === "formal")) {
    const transformed = transformMessage(trimmedText, persona, room)
    if (transformed !== trimmedText) {
      return transformed
    }
  }

  return null
}

export function analyzeMessageEmotion(
  text: string,
): "positive" | "negative" | "neutral" | "surprise" | "congratulation" | "support" {
  const positiveWords = ["감사", "고마", "좋아", "최고", "잘했", "훌륭", "대단", "멋지", "좋은", "행복", "기뻐"]
  const negativeWords = ["아쉽", "힘들", "어려", "안타깝", "슬프", "걱정", "힘내", "속상", "우울", "지쳐"]
  const surpriseWords = ["진짜", "헐", "대박", "와", "놀라", "세상에", "믿기", "어떻게"]
  const congratsWords = ["축하", "성공", "합격", "완료", "완성", "승진", "결혼", "생일"]
  const supportWords = ["응원", "파이팅", "화이팅", "할 수 있", "믿어", "잘 될"]

  if (congratsWords.some((w) => text.includes(w))) return "congratulation"
  if (supportWords.some((w) => text.includes(w))) return "support"
  if (negativeWords.some((w) => text.includes(w))) return "negative"
  if (surpriseWords.some((w) => text.includes(w))) return "surprise"
  if (positiveWords.some((w) => text.includes(w))) return "positive"

  return "neutral"
}

export function suggestReactions(messageContent: string, room: ChatRoom): string[] {
  const emotion = analyzeMessageEmotion(messageContent)

  // 감정에 따른 이모지 우선순위 변경
  switch (emotion) {
    case "congratulation":
      return ["🎉", "👏", "💯", "🥳"]
    case "positive":
      return ["❤️", "👍", "🙏", "😊"]
    case "negative":
      return ["😢", "🙏", "❤️", "💪"]
    case "surprise":
      return ["😮", "🔥", "💯", "😱"]
    case "support":
      return ["🔥", "💪", "👏", "❤️"]
    default:
      return ["👍", "❤️", "👏", "😊"]
  }
}

export function suggestTextReactions(messageContent: string, room: ChatRoom): string[] {
  const emotion = analyzeMessageEmotion(messageContent)

  switch (emotion) {
    case "congratulation":
      return ["축하드려요!", "대박!!", "정말 잘됐네요!"]
    case "positive":
      return ["감사합니다!", "좋네요~", "다행이에요!"]
    case "negative":
      return ["고생하셨어요ㅠㅠ", "힘내세요!", "괜찮아요~"]
    case "surprise":
      return ["헐 진짜요?", "오 대박!!", "세상에..."]
    case "support":
      return ["화이팅!", "응원해요!", "잘 될 거예요!"]
    default:
      return ["네~", "알겠어요!", "확인했어요!"]
  }
}

export function suggestQuickReplies(messageContent: string, room: ChatRoom, persona: Persona): string[] {
  const replies: string[] = []
  const emotion = analyzeMessageEmotion(messageContent)

  // 질문에 대한 응답
  if (messageContent.includes("?") || messageContent.includes("까요") || messageContent.includes("할래")) {
    if (room.relationship === "boss" || room.relationship === "senior") {
      replies.push("네, 알겠습니다.")
      replies.push("확인 후 말씀드리겠습니다.")
    } else {
      replies.push("응, 알겠어!")
      replies.push("좋아, 그렇게 하자!")
    }
  }

  // 감사 표현에 대한 응답
  if (messageContent.includes("감사") || messageContent.includes("고마")) {
    if (room.relationship === "boss" || room.relationship === "senior") {
      replies.push("별말씀을요.")
      replies.push("감사합니다.")
    } else {
      replies.push("별거 아니야~")
      replies.push("응응!")
    }
  }

  // 업무 관련 퀵 리플라이
  if (messageContent.includes("회의") || messageContent.includes("자료") || messageContent.includes("보고")) {
    replies.push("지금 확인했습니다!")
    replies.push("잠시만요!")
  }

  // 감정 기반 퀵 리플라이
  if (emotion === "negative") {
    replies.push("고생하셨어요ㅠㅠ")
  } else if (emotion === "congratulation") {
    replies.push("축하드려요!")
  }

  // 기본 퀵 리플라이
  if (replies.length === 0) {
    if (room.relationship === "boss" || room.relationship === "senior") {
      replies.push("지금 확인했습니다!")
      replies.push("잠시만요!")
    } else {
      replies.push("ㅇㅋ!")
      replies.push("알겠어~")
    }
  }

  return replies.slice(0, 3)
}

export function getPersonaByFormalityLevel(level: number): Persona {
  const persona = PERSONAS.find((p) => level >= p.formalityRange[0] && level <= p.formalityRange[1])
  return persona || PERSONAS[2]
}

export function calculateFormalityLevel(room: ChatRoom): number {
  // room에 formalityLevel이 있으면 해당 값 사용
  if (room.formalityLevel !== undefined) {
    return room.formalityLevel
  }

  switch (room.relationship) {
    case "boss":
      return 95
    case "senior":
      return 70
    case "colleague":
      return 50
    case "friend":
      return 5
    case "family":
      return 10
    default:
      return 50
  }
}

export function shouldScheduleMessage(room: ChatRoom): { should: boolean; suggestedTime?: Date; reason?: string } {
  const now = new Date()
  const hour = now.getHours()
  const day = now.getDay()

  if (hour >= 22 || hour < 7) {
    if (room.relationship === "boss" || room.relationship === "senior") {
      const suggestedTime = new Date()
      suggestedTime.setHours(9, 0, 0, 0)
      if (hour >= 22) {
        suggestedTime.setDate(suggestedTime.getDate() + 1)
      }
      return {
        should: true,
        suggestedTime,
        reason: `밤 늦은 시간에 ${room.relationship === "boss" ? "상사" : "선배"}님께 메시지를 보내시려고 합니다.`,
      }
    }
  }

  if (day === 0 || day === 6) {
    if (room.relationship === "boss") {
      const suggestedTime = new Date()
      const daysUntilMonday = day === 0 ? 1 : 2
      suggestedTime.setDate(suggestedTime.getDate() + daysUntilMonday)
      suggestedTime.setHours(9, 0, 0, 0)
      return {
        should: true,
        suggestedTime,
        reason: "주말에 상사님께 메시지를 보내시려고 합니다.",
      }
    }
  }

  return { should: false }
}

export function generateContextualResponse(userMessage: string, room: ChatRoom): string {
  const isBossOrSenior = room.relationship === "boss" || room.relationship === "senior"
  const isFriend = room.relationship === "friend"
  const isFamily = room.relationship === "family"

  const responses: Record<string, { formal: string; casual: string }> = {
    알겠어: {
      formal: "네, 알겠습니다. 바로 조치하겠습니다.",
      casual: "ㅇㅇ 알겠음 이따 봐!",
    },
    알았어: {
      formal: "네, 알겠습니다. 확인 후 진행하겠습니다.",
      casual: "ㅇㅋㅇㅋ 알겠어ㅋㅋ",
    },
    고마워: {
      formal: "별말씀을요. 더 필요하신 게 있으시면 말씀해주세요.",
      casual: "ㅋㅋ 별거 아니야~ 언제든!",
    },
    미안: {
      formal: "괜찮습니다. 신경 쓰지 마세요.",
      casual: "ㅋㅋ 괜찮아 괜찮아~",
    },
    확인: {
      formal: "네, 확인했습니다.",
      casual: "ㅇㅇ 확인~",
    },
  }

  for (const [keyword, reply] of Object.entries(responses)) {
    if (userMessage.includes(keyword)) {
      if (isBossOrSenior) {
        return reply.formal
      } else if (isFriend || isFamily) {
        return reply.casual
      }
    }
  }

  if (userMessage.includes("?") || userMessage.includes("까요") || userMessage.includes("할래")) {
    if (isBossOrSenior) {
      return "네, 말씀하신 대로 진행하겠습니다."
    } else {
      return "ㅇㅋㅇㅋ 그러자ㅋㅋ"
    }
  }

  if (isBossOrSenior) {
    const formalReplies = [
      "네, 확인했습니다.",
      "알겠습니다. 진행하겠습니다.",
      "네, 말씀하신 대로 하겠습니다.",
      "확인 후 보고드리겠습니다.",
    ]
    return formalReplies[Math.floor(Math.random() * formalReplies.length)]
  } else {
    const casualReplies = ["ㅇㅇ 알겠어ㅋㅋ", "ㅋㅋㅋ 오키~", "ㅇㅋ 그러자!", "ㅎㅎ 알겠어 이따 봐!"]
    return casualReplies[Math.floor(Math.random() * casualReplies.length)]
  }
}

export function generateId(): string {
  return Math.random().toString(36).substring(2, 9)
}
