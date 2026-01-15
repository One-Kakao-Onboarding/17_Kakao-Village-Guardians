"use client"

import type React from "react"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { initializeApiClient } from "@/lib/api/client"
import { userApi } from "@/lib/api/services"

interface LoginScreenProps {
  onLogin: (ldap: string, userName: string) => void
}

export function LoginScreen({ onLogin }: LoginScreenProps) {
  const [ldap, setLdap] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!ldap.trim()) return

    setLoading(true)
    setError(null)

    try {
      // Initialize API client with LDAP
      initializeApiClient(ldap.trim())

      // Try to verify LDAP by fetching user info
      try {
        const user = await userApi.getCurrentUser()
        // Call onLogin with LDAP and user name from API
        onLogin(ldap.trim(), user.name)
      } catch (apiError) {
        // If API fails, allow login with LDAP as name (dev mode)
        console.warn('API not available, using dev mode:', apiError)
        onLogin(ldap.trim(), ldap.trim())
      }
    } catch (err: any) {
      console.error('Login error:', err)
      setError(err.message || 'LDAP 인증에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden"
      style={{
        background: "linear-gradient(180deg, #B8D4E8 0%, #D4E4ED 30%, #F5EEE6 70%, #F9F3EC 100%)",
      }}
    >
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[10%] left-[5%] w-32 h-16 bg-white/30 rounded-full blur-xl" />
        <div className="absolute top-[15%] left-[20%] w-48 h-20 bg-white/25 rounded-full blur-xl" />
        <div className="absolute top-[8%] right-[10%] w-40 h-16 bg-white/30 rounded-full blur-xl" />
        <div className="absolute top-[20%] right-[25%] w-36 h-14 bg-white/20 rounded-full blur-xl" />
        <div className="absolute bottom-[15%] left-[10%] w-44 h-18 bg-white/20 rounded-full blur-xl" />
        <div className="absolute bottom-[20%] right-[15%] w-36 h-14 bg-white/25 rounded-full blur-xl" />
      </div>

      <div className="w-full max-w-sm bg-gradient-to-b from-[#D4E8F2]/80 to-white/90 backdrop-blur-sm rounded-3xl shadow-xl p-8 relative z-10">
        <div className="flex flex-col items-center mb-8">
          <h1 className="text-3xl font-bold mb-6">
            <span className="text-[#5DADE2]">Persona</span>
            <span className="text-[#F4D03F]"> T</span>
            <span className="text-[#F5B7B1]">a</span>
            <span className="text-[#5DADE2]">l</span>
            <span className="text-[#F4D03F]">k</span>
          </h1>

          <div className="w-32 h-32 rounded-full border-4 border-white shadow-lg flex items-center justify-center bg-white mb-2 overflow-hidden">
            <img
              src="/춘식.png"
              alt="춘식이"
              className="w-28 h-28 object-contain"
            />
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Input
              id="ldap"
              type="text"
              placeholder="LDAP"
              value={ldap}
              onChange={(e) => setLdap(e.target.value)}
              className="h-12 text-base bg-white/80 border-gray-200 rounded-xl text-center placeholder:text-gray-400"
              autoFocus
              disabled={loading}
            />
          </div>
          {error && (
            <p className="text-sm text-red-500 text-center">{error}</p>
          )}
          <Button
            type="submit"
            className="w-full h-12 text-base bg-[#F9D342] hover:bg-[#F0C832] text-gray-800 font-medium rounded-xl"
            disabled={!ldap.trim() || loading}
          >
            {loading ? "인증 중..." : "시작하기"}
          </Button>
        </form>

      </div>
    </div>
  )
}
