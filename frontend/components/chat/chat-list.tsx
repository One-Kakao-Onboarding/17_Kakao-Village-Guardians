"use client"

import { useState, useRef } from "react"
import type { ChatRoom, Profile } from "@/lib/types"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { ProfileManager } from "@/components/profile/profile-manager"
import { AddFriendDialog } from "./add-friend-dialog"
import { UserPlus, LogOut, Camera } from "lucide-react"

interface ChatListProps {
  rooms: ChatRoom[]
  selectedRoom: ChatRoom | null
  onSelectRoom: (room: ChatRoom) => void
  profiles: Profile[]
  activeProfile: Profile | null
  onSelectProfile: (profile: Profile) => void
  onAddProfile: (profile: Omit<Profile, "id">) => void
  onEditProfile: (id: string, profile: Omit<Profile, "id">) => void
  onDeleteProfile: (id: string) => void
  onAddFriend: (data: {
    friendLdap: string
    formalityLevel: number
    relationship: "boss" | "senior" | "colleague" | "friend" | "family"
  }) => Promise<void>
  onDeleteChatRoom: (roomId: string) => Promise<void>
  onLogout: () => void
  userName: string
  userAvatar: string | null
  onAvatarChange: (file: File) => void
}

export function ChatList({
  rooms,
  selectedRoom,
  onSelectRoom,
  profiles,
  activeProfile,
  onSelectProfile,
  onAddProfile,
  onEditProfile,
  onDeleteProfile,
  onAddFriend,
  onDeleteChatRoom,
  onLogout,
  userName,
  userAvatar,
  onAvatarChange,
}: ChatListProps) {
  const [addFriendOpen, setAddFriendOpen] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      onAvatarChange(file)
    }
  }
  const formatTime = (date?: Date) => {
    if (!date) return ""
    const now = new Date()
    const diff = now.getTime() - date.getTime()
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))

    if (days === 0) {
      return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" })
    } else if (days === 1) {
      return "어제"
    } else if (days < 7) {
      return ["일", "월", "화", "수", "목", "금", "토"][date.getDay()] + "요일"
    } else {
      return `${date.getMonth() + 1}/${date.getDate()}`
    }
  }

  const getRelationshipBadge = (relationship: ChatRoom["relationship"]) => {
    const badges = {
      boss: { label: "상사", variant: "default" as const },
      senior: { label: "선배", variant: "secondary" as const },
      colleague: { label: "동료", variant: "outline" as const },
      friend: { label: "친구", variant: "outline" as const },
      family: { label: "가족", variant: "outline" as const },
    }
    return badges[relationship]
  }

  return (
    <div className="flex flex-col h-full">
      <div className="px-4 py-3 border-b border-border bg-card flex items-center justify-between">
        <div className="flex items-center gap-2 flex-1 min-w-0">
          <div
            className="relative w-8 h-8 rounded-full cursor-pointer group"
            onClick={handleAvatarClick}
            title="프로필 사진 변경"
          >
            {userAvatar ? (
              <img src={userAvatar} alt={userName} className="w-full h-full rounded-full object-cover" />
            ) : (
              <div className="w-full h-full rounded-full bg-primary/10 flex items-center justify-center text-primary font-semibold text-sm">
                {userName[0]?.toUpperCase() || "U"}
              </div>
            )}
            <div className="absolute inset-0 rounded-full bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Camera className="h-4 w-4 text-white" />
            </div>
          </div>
          <span className="text-sm font-medium text-foreground truncate">{userName}</span>
        </div>
        <Button
          size="sm"
          variant="ghost"
          onClick={onLogout}
          className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground"
          title="로그아웃"
        >
          <LogOut className="h-4 w-4" />
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleFileChange}
          className="hidden"
        />
      </div>

      <ProfileManager
        profiles={profiles}
        activeProfile={activeProfile}
        onSelectProfile={onSelectProfile}
        onAddProfile={onAddProfile}
        onEditProfile={onEditProfile}
        onDeleteProfile={onDeleteProfile}
        chatRooms={rooms}
      />

      <div className="px-4 py-2 border-b border-border bg-muted/30 flex items-center justify-between">
        <h3 className="text-sm font-medium text-muted-foreground">채팅 목록</h3>
        <Button
          size="sm"
          variant="ghost"
          onClick={() => setAddFriendOpen(true)}
          className="h-8 w-8 p-0"
        >
          <UserPlus className="h-4 w-4" />
        </Button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {rooms.map((room) => {
          const badge = getRelationshipBadge(room.relationship)
          return (
            <button
              key={room.id}
              onClick={() => onSelectRoom(room)}
              className={cn(
                "w-full flex items-center gap-3 p-4 hover:bg-muted/50 transition-colors text-left",
                selectedRoom?.id === room.id && "bg-muted",
              )}
            >
              <Avatar className="h-12 w-12">
                <AvatarImage src={room.avatar || "/placeholder.svg"} alt={room.name} />
                <AvatarFallback className="bg-primary/10 text-primary">{room.name[0]}</AvatarFallback>
              </Avatar>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-foreground truncate">{room.name}</span>
                  <Badge variant={badge.variant} className="text-xs px-1.5 py-0">
                    {badge.label}
                  </Badge>
                  {room.isGroup && (
                    <Badge variant="outline" className="text-xs px-1.5 py-0">
                      그룹
                    </Badge>
                  )}
                </div>
                <p className="text-sm text-muted-foreground truncate">
                  {room.lastMessage || "새로운 대화를 시작하세요"}
                </p>
              </div>
              <div className="flex flex-col items-end gap-1">
                <span className="text-xs text-muted-foreground">{formatTime(room.lastMessageTime)}</span>
                {(room.unreadCount ?? 0) > 0 && (
                  <span className="flex items-center justify-center w-5 h-5 text-xs font-medium text-primary-foreground bg-primary rounded-full">
                    {room.unreadCount}
                  </span>
                )}
              </div>
            </button>
          )
        })}
      </div>

      <AddFriendDialog
        open={addFriendOpen}
        onOpenChange={setAddFriendOpen}
        onAddFriend={onAddFriend}
        activeProfileId={activeProfile?.id}
      />
    </div>
  )
}
