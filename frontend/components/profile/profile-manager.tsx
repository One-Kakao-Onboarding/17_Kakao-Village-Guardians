"use client"

import { useState, useEffect, useRef } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Plus, Check, Pencil, Trash2, Sparkles, Camera } from "lucide-react"
import { type Profile, type ChatRoom, PERSONAS } from "@/lib/types"
import { cn } from "@/lib/utils"

interface ProfileManagerProps {
  profiles: Profile[]
  activeProfile: Profile | null
  onSelectProfile: (profile: Profile) => void
  onAddProfile: (profile: Omit<Profile, "id">) => void
  onEditProfile: (id: string, profile: Omit<Profile, "id">) => void
  onDeleteProfile: (id: string) => void
  chatRooms?: ChatRoom[]
}

function getRecommendedFriends(name: string, personaId: string, chatRooms: ChatRoom[]): ChatRoom[] {
  const selectedPersona = PERSONAS.find((p) => p.id === personaId)

  // 프로필 이름 키워드 분류
  const workKeywords = ["회사", "직장", "업무", "비즈니스", "워크", "오피스", "프로페셔널"]
  const friendKeywords = ["친구", "친목", "놀이", "일상", "사적", "프라이빗"]
  const familyKeywords = ["가족", "집", "홈", "패밀리"]

  const lowerName = name.toLowerCase()
  const isWorkProfile = workKeywords.some((k) => lowerName.includes(k))
  const isFriendProfile = friendKeywords.some((k) => lowerName.includes(k))
  const isFamilyProfile = familyKeywords.some((k) => lowerName.includes(k))

  // 점수 기반 추천 시스템
  const scored = chatRooms.map((room) => {
    let score = 0

    // 1. 프로필 이름 기반 매칭
    if (isWorkProfile) {
      if (room.relationship === "boss") score += 50
      else if (room.relationship === "senior") score += 40
      else if (room.relationship === "colleague") score += 30
    } else if (isFriendProfile) {
      if (room.relationship === "friend") score += 50
    } else if (isFamilyProfile) {
      if (room.relationship === "family") score += 50
    }

    // 2. 말투 모드 기반 매칭 (정중함 <-> 친근함)
    if (personaId === "very-formal" || personaId === "formal") {
      // 정중함 모드: 격식지수 50% 이상인 상사/선배 우선
      if (room.formalityLevel >= 50) score += 30
      if (room.relationship === "boss" || room.relationship === "senior") score += 20
    } else if (personaId === "casual" || personaId === "very-casual") {
      // 친근함/반말 모드: 격식지수 30% 이하인 친구/가족 우선
      if (room.formalityLevel <= 30) score += 30
      if (room.relationship === "friend" || room.relationship === "family") score += 20
    } else {
      // 중간 모드: 동료 및 중간 격식지수 우선
      if (room.formalityLevel >= 30 && room.formalityLevel <= 70) score += 20
      if (room.relationship === "colleague" || room.relationship === "senior") score += 10
    }

    // 3. 격식지수 범위 매칭 (페르소나의 격식 범위와 비교)
    if (selectedPersona) {
      const [minFormality, maxFormality] = selectedPersona.formalityRange
      if (room.formalityLevel >= minFormality - 20 && room.formalityLevel <= maxFormality + 20) {
        score += 15
      }
    }

    return { room, score }
  })

  // 점수 내림차순 정렬 후 상위 5명 반환
  return scored
    .sort((a, b) => b.score - a.score)
    .filter((item) => item.score > 0)
    .slice(0, 5)
    .map((item) => item.room)
}

export function ProfileManager({
  profiles,
  activeProfile,
  onSelectProfile,
  onAddProfile,
  onEditProfile,
  onDeleteProfile,
  chatRooms = [],
}: ProfileManagerProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [editingProfile, setEditingProfile] = useState<Profile | null>(null)
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [defaultPersona, setDefaultPersona] = useState(PERSONAS[1].id)
  const [avatar, setAvatar] = useState<string>("")
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [recommendedFriends, setRecommendedFriends] = useState<ChatRoom[]>([])
  const [selectedFriends, setSelectedFriends] = useState<Set<string>>(new Set())

  useEffect(() => {
    if (name.trim() && chatRooms.length > 0) {
      const recommended = getRecommendedFriends(name, defaultPersona, chatRooms)
      setRecommendedFriends(recommended)
      setSelectedFriends(new Set(recommended.map((r) => r.id)))
    } else {
      setRecommendedFriends([])
      setSelectedFriends(new Set())
    }
  }, [name, defaultPersona, chatRooms])

  const handleOpenDialog = (profile?: Profile) => {
    if (profile) {
      setEditingProfile(profile)
      setName(profile.name)
      setDescription(profile.description)
      setDefaultPersona(profile.defaultPersona)
      setAvatar(profile.avatar || "")
    } else {
      setEditingProfile(null)
      setName("")
      setDescription("")
      setDefaultPersona(PERSONAS[1].id)
      setAvatar("")
    }
    setSelectedFriends(new Set())
    setIsDialogOpen(true)
  }

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      const reader = new FileReader()
      reader.onloadend = () => {
        setAvatar(reader.result as string)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleSave = () => {
    const profileData = {
      name,
      description,
      defaultPersona,
      avatar: avatar || `/placeholder.svg?height=40&width=40&query=${encodeURIComponent(name)} avatar`,
      assignedFriends: Array.from(selectedFriends),
    }

    if (editingProfile) {
      onEditProfile(editingProfile.id, profileData)
    } else {
      onAddProfile(profileData)
    }
    setIsDialogOpen(false)
  }

  const toggleFriendSelection = (friendId: string) => {
    setSelectedFriends((prev) => {
      const newSet = new Set(prev)
      if (newSet.has(friendId)) {
        newSet.delete(friendId)
      } else {
        newSet.add(friendId)
      }
      return newSet
    })
  }

  const toggleSelectAll = () => {
    if (selectedFriends.size === recommendedFriends.length) {
      setSelectedFriends(new Set())
    } else {
      setSelectedFriends(new Set(recommendedFriends.map((r) => r.id)))
    }
  }

  const selectedPersona = PERSONAS.find((p) => p.id === defaultPersona)

  const getRelationshipLabel = (relationship: ChatRoom["relationship"]) => {
    const labels: Record<string, string> = {
      boss: "상사",
      senior: "선배",
      colleague: "동료",
      friend: "친구",
      family: "가족",
    }
    return labels[relationship] || relationship
  }

  const getFormalityLabel = (level: number) => {
    if (level >= 80) return "매우 높음"
    if (level >= 60) return "높음"
    if (level >= 40) return "보통"
    if (level >= 20) return "낮음"
    return "매우 낮음"
  }

  return (
    <div className="p-4 border-b border-border">
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg font-semibold text-foreground">멀티프로필</h2>
        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button variant="outline" size="sm" onClick={() => handleOpenDialog()}>
              <Plus className="h-4 w-4 mr-1" />
              추가
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>{editingProfile ? "프로필 수정" : "새 프로필 만들기"}</DialogTitle>
              <DialogDescription>상황에 맞는 프로필을 만들어 사용해보세요.</DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label>프로필 사진</Label>
                <div className="flex items-center gap-4">
                  <div
                    className="relative w-20 h-20 rounded-full cursor-pointer group"
                    onClick={handleAvatarClick}
                  >
                    {avatar ? (
                      <img src={avatar} alt="프로필" className="w-full h-full rounded-full object-cover" />
                    ) : (
                      <div className="w-full h-full rounded-full bg-primary/10 flex items-center justify-center text-primary font-semibold text-2xl">
                        {name[0]?.toUpperCase() || "P"}
                      </div>
                    )}
                    <div className="absolute inset-0 rounded-full bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                      <Camera className="h-6 w-6 text-white" />
                    </div>
                  </div>
                  <div className="flex-1">
                    <Button type="button" variant="outline" size="sm" onClick={handleAvatarClick}>
                      사진 선택
                    </Button>
                    <p className="text-xs text-muted-foreground mt-1">
                      PNG, JPEG, GIF 지원 (최대 5MB)
                    </p>
                  </div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileChange}
                    className="hidden"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="name">프로필 이름</Label>
                <Input
                  id="name"
                  placeholder="예: 회사용, 친구용, 가족용"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
                <p className="text-[10px] text-muted-foreground">
                  '회사', '업무' 입력 시 직장 동료를, '친구', '일상' 입력 시 친구를 추천해요
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">설명</Label>
                <Input
                  id="description"
                  placeholder="이 프로필에 대한 설명"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label>기본 말투 모드</Label>
                <Select value={defaultPersona} onValueChange={setDefaultPersona}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {PERSONAS.map((persona) => (
                      <SelectItem key={persona.id} value={persona.id}>
                        <div className="flex items-center gap-2">
                          <span>{persona.icon}</span>
                          <span>{persona.name}</span>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {PERSONAS.find((p) => p.id === defaultPersona) && (
                  <p className="text-xs text-muted-foreground">
                    {PERSONAS.find((p) => p.id === defaultPersona)?.description}
                    {(defaultPersona === "very-formal" || defaultPersona === "formal") &&
                      " - 상사/선배와의 대화에 추천"}
                    {(defaultPersona === "casual" || defaultPersona === "very-casual") &&
                      " - 친구/가족과의 대화에 추천"}
                  </p>
                )}
              </div>

              {name.trim() && recommendedFriends.length > 0 && (
                <div className="space-y-3 pt-2 border-t border-border">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Sparkles className="h-4 w-4 text-yellow-500" />
                      <Label className="text-sm font-medium">이 프로필과 어울리는 친구</Label>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="text-xs h-7 px-2"
                      onClick={toggleSelectAll}
                    >
                      {selectedFriends.size === recommendedFriends.length ? "전체 해제" : "일괄 적용"}
                    </Button>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    채팅 로그 100개를 분석하여 '{name}' 프로필의 말투(
                    {PERSONAS.find((p) => p.id === defaultPersona)?.name})에 어울리는 친구를 추천했어요.
                  </p>

                  <div className="flex gap-2 overflow-x-auto pb-2 -mx-1 px-1">
                    {recommendedFriends.map((friend) => {
                      const isSelected = selectedFriends.has(friend.id)
                      return (
                        <button
                          key={friend.id}
                          type="button"
                          onClick={() => toggleFriendSelection(friend.id)}
                          className={cn(
                            "flex-shrink-0 flex flex-col items-center gap-1.5 p-3 rounded-xl transition-all min-w-[90px]",
                            "border-2",
                            isSelected
                              ? "border-primary bg-primary/5"
                              : "border-transparent bg-muted/50 hover:bg-muted",
                          )}
                        >
                          <div className="relative">
                            <Avatar className="h-12 w-12 ring-2 ring-background">
                              <AvatarImage src={friend.avatar || "/placeholder.svg"} alt={friend.name} />
                              <AvatarFallback className="bg-primary/10 text-primary">{friend.name[0]}</AvatarFallback>
                            </Avatar>
                            {isSelected && (
                              <div className="absolute -bottom-1 -right-1 w-5 h-5 bg-primary rounded-full flex items-center justify-center shadow-sm">
                                <Check className="h-3 w-3 text-primary-foreground" />
                              </div>
                            )}
                          </div>
                          <span className="text-xs font-medium truncate w-full text-center">{friend.name}</span>
                          <span className="text-[10px] text-muted-foreground px-1.5 py-0.5 bg-muted rounded-full">
                            {getRelationshipLabel(friend.relationship)}
                          </span>
                          <span className="text-[9px] text-muted-foreground/70">격식 {friend.formalityLevel}%</span>
                        </button>
                      )
                    })}
                  </div>

                  {selectedFriends.size > 0 && (
                    <p className="text-xs text-primary">{selectedFriends.size}명의 친구가 이 프로필에 적용됩니다.</p>
                  )}
                </div>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                취소
              </Button>
              <Button onClick={handleSave} disabled={!name.trim()}>
                {editingProfile ? "수정" : "만들기"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-2">
        {profiles.map((profile) => {
          const persona = PERSONAS.find((p) => p.id === profile.defaultPersona)
          const isActive = activeProfile?.id === profile.id

          return (
            <div key={profile.id} className="relative group">
              <button
                onClick={() => onSelectProfile(profile)}
                className={cn(
                  "flex flex-col items-center gap-1 p-2 rounded-lg transition-colors min-w-[72px]",
                  isActive ? "bg-primary/10 ring-2 ring-primary" : "hover:bg-muted",
                )}
              >
                <div className="relative">
                  <Avatar className="h-10 w-10">
                    <AvatarImage src={profile.avatar || "/placeholder.svg"} alt={profile.name} />
                    <AvatarFallback className="bg-primary/10 text-primary text-sm">{profile.name[0]}</AvatarFallback>
                  </Avatar>
                  {isActive && (
                    <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-primary rounded-full flex items-center justify-center">
                      <Check className="h-3 w-3 text-primary-foreground" />
                    </div>
                  )}
                </div>
                <span className="text-xs font-medium truncate w-full text-center">{profile.name}</span>
                <span className="text-[10px] text-muted-foreground">{persona?.icon}</span>
              </button>

              <div className="absolute top-0 right-0 opacity-0 group-hover:opacity-100 transition-opacity flex gap-0.5">
                <button
                  onClick={(e) => {
                    e.stopPropagation()
                    handleOpenDialog(profile)
                  }}
                  className="p-1 rounded bg-muted hover:bg-muted/80"
                >
                  <Pencil className="h-3 w-3" />
                </button>
                {profiles.length > 1 && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      onDeleteProfile(profile.id)
                    }}
                    className="p-1 rounded bg-destructive/10 hover:bg-destructive/20 text-destructive"
                  >
                    <Trash2 className="h-3 w-3" />
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
