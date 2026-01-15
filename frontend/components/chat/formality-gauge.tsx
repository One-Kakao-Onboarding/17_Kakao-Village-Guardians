"use client"

import type React from "react"
import { useEffect, useState } from "react"
import { cn } from "@/lib/utils"

interface FormalityGaugeProps {
  level: number
  onChange?: (level: number) => void
  disabled?: boolean
  compact?: boolean
}

export function FormalityGauge({ level, onChange, disabled = false, compact = false }: FormalityGaugeProps) {
  const [currentLevel, setCurrentLevel] = useState(level ?? 50)

  useEffect(() => {
    setCurrentLevel(level ?? 50)
  }, [level])

  const getColor = (value: number) => {
    if (value >= 80) return { bg: "bg-red-500", text: "text-red-500", fill: "#ef4444" }
    if (value >= 60) return { bg: "bg-orange-500", text: "text-orange-500", fill: "#f97316" }
    if (value >= 40) return { bg: "bg-yellow-500", text: "text-yellow-500", fill: "#eab308" }
    if (value >= 20) return { bg: "bg-cyan-500", text: "text-cyan-500", fill: "#06b6d4" }
    return { bg: "bg-blue-500", text: "text-blue-500", fill: "#3b82f6" }
  }

  const getLabel = (value: number) => {
    if (value >= 80) return "매우 격식"
    if (value >= 60) return "격식"
    if (value >= 40) return "보통"
    if (value >= 20) return "친근"
    return "매우 친근"
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = Number(e.target.value)
    setCurrentLevel(newValue)
    onChange?.(newValue)
  }

  const colors = getColor(currentLevel)

  if (compact) {
    return (
      <div className="flex items-center gap-2">
        <div className="relative w-8 h-16">
          {/* 온도계 SVG */}
          <svg viewBox="0 0 32 64" className="w-full h-full">
            {/* 온도계 외곽 */}
            <rect x="10" y="4" width="12" height="44" rx="6" fill="white" stroke="#e5e7eb" strokeWidth="2" />
            {/* 온도계 구 부분 */}
            <circle cx="16" cy="52" r="10" fill="white" stroke="#e5e7eb" strokeWidth="2" />
            {/* 온도 수치 (구 안의 원) */}
            <circle cx="16" cy="52" r="7" fill={colors.fill} />
            {/* 온도 바 */}
            <rect
              x="13"
              y={48 - (currentLevel / 100) * 40}
              width="6"
              height={(currentLevel / 100) * 40 + 4}
              rx="3"
              fill={colors.fill}
            />
            {/* 눈금 */}
            <line x1="8" y1="12" x2="10" y2="12" stroke="#d1d5db" strokeWidth="1" />
            <line x1="8" y1="24" x2="10" y2="24" stroke="#d1d5db" strokeWidth="1" />
            <line x1="8" y1="36" x2="10" y2="36" stroke="#d1d5db" strokeWidth="1" />
          </svg>
          {/* 귀여운 얼굴 표시 */}
          <div className="absolute -top-1 -right-1 w-4 h-4 bg-white rounded-full shadow-sm border border-border flex items-center justify-center text-[8px]">
            {currentLevel >= 60 ? "😊" : currentLevel >= 30 ? "😉" : "😎"}
          </div>
        </div>
        <div className="flex flex-col">
          <span className={cn("text-xs font-bold", colors.text)}>{currentLevel}°</span>
          <span className="text-[10px] text-muted-foreground">{getLabel(currentLevel)}</span>
        </div>
        {!disabled && (
          <input
            type="range"
            min="0"
            max="100"
            value={currentLevel}
            onChange={handleChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
          />
        )}
      </div>
    )
  }

  // 기존 전체 모드
  return (
    <div className="flex items-center gap-3 p-3 bg-card rounded-lg border border-border">
      <div className="relative w-6 h-12">
        <svg viewBox="0 0 24 48" className="w-full h-full">
          <rect x="8" y="4" width="8" height="32" rx="4" fill="white" stroke="#e5e7eb" strokeWidth="1.5" />
          <circle cx="12" cy="40" r="6" fill="white" stroke="#e5e7eb" strokeWidth="1.5" />
          <circle cx="12" cy="40" r="4" fill={colors.fill} />
          <rect
            x="10"
            y={36 - (currentLevel / 100) * 28}
            width="4"
            height={(currentLevel / 100) * 28 + 4}
            rx="2"
            fill={colors.fill}
          />
        </svg>
      </div>
      <div className="flex-1">
        <div className="flex items-center justify-between mb-1">
          <span className="text-sm font-medium text-foreground">격식 지수</span>
          <span className={cn("text-xs px-2 py-0.5 rounded-full text-white", colors.bg)}>{getLabel(currentLevel)}</span>
        </div>
        <div className="relative">
          <div className="h-2 bg-muted rounded-full overflow-hidden">
            <div
              className={cn("h-full transition-all duration-300", colors.bg)}
              style={{ width: `${currentLevel}%` }}
            />
          </div>
          {!disabled && (
            <input
              type="range"
              min="0"
              max="100"
              value={currentLevel}
              onChange={handleChange}
              className="absolute inset-0 w-full opacity-0 cursor-pointer"
            />
          )}
        </div>
        <div className="flex justify-between mt-1 text-xs text-muted-foreground">
          <span>친근</span>
          <span>격식</span>
        </div>
      </div>
    </div>
  )
}
