"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

interface AddFriendDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onAddFriend: (data: {
    friendLdap: string
    formalityLevel: number
    relationship: "boss" | "senior" | "colleague" | "friend" | "family"
    profileId?: string
  }) => Promise<void>
  activeProfileId?: string
}

export function AddFriendDialog({ open, onOpenChange, onAddFriend, activeProfileId }: AddFriendDialogProps) {
  const [ldap, setLdap] = useState("")
  const [relationship, setRelationship] = useState<"boss" | "senior" | "colleague" | "friend" | "family">("colleague")
  const [formalityLevel, setFormalityLevel] = useState(50)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!ldap.trim()) return

    setLoading(true)
    setError(null)

    try {
      await onAddFriend({
        friendLdap: ldap.trim(),
        formalityLevel,
        relationship,
        profileId: activeProfileId,
      })

      // Reset form and close dialog
      setLdap("")
      setRelationship("colleague")
      setFormalityLevel(50)
      onOpenChange(false)
    } catch (err: any) {
      console.error("Failed to add friend:", err)
      setError(err.message || "친구 추가에 실패했습니다")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>친구 추가</DialogTitle>
          <DialogDescription>
            새로운 친구를 추가하고 대화를 시작하세요
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="ldap">친구 LDAP</Label>
              <Input
                id="ldap"
                placeholder="ldap.id"
                value={ldap}
                onChange={(e) => setLdap(e.target.value)}
                disabled={loading}
                autoFocus
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="relationship">관계</Label>
              <Select
                value={relationship}
                onValueChange={(value: any) => setRelationship(value)}
                disabled={loading}
              >
                <SelectTrigger id="relationship">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="boss">상사</SelectItem>
                  <SelectItem value="senior">선배</SelectItem>
                  <SelectItem value="colleague">동료</SelectItem>
                  <SelectItem value="friend">친구</SelectItem>
                  <SelectItem value="family">가족</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {error && (
              <p className="text-sm text-red-500">{error}</p>
            )}
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={loading}
            >
              취소
            </Button>
            <Button type="submit" disabled={!ldap.trim() || loading}>
              {loading ? "추가 중..." : "추가"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
