"use client"

import { useState } from "react"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { EMOTICON_PACKS, type Emoticon } from "@/lib/types"
import { cn } from "@/lib/utils"

interface EmoticonPickerProps {
  onSelect: (emoticon: Emoticon) => void
}

export function EmoticonPicker({ onSelect }: EmoticonPickerProps) {
  const [selectedPack, setSelectedPack] = useState(EMOTICON_PACKS[0].name)

  return (
    <div className="w-80 p-3">
      <Tabs value={selectedPack} onValueChange={setSelectedPack}>
        <TabsList className="w-full grid mb-3" style={{ gridTemplateColumns: `repeat(${EMOTICON_PACKS.length}, 1fr)` }}>
          {EMOTICON_PACKS.map((pack) => (
            <TabsTrigger key={pack.name} value={pack.name} className="text-xs">
              {pack.name}
            </TabsTrigger>
          ))}
        </TabsList>
        {EMOTICON_PACKS.map((pack) => (
          <TabsContent key={pack.name} value={pack.name} className="mt-0">
            <div className="grid grid-cols-4 gap-2 max-h-[240px] overflow-y-auto">
              {pack.emoticons.map((emoticon) => (
                <button
                  key={emoticon.id}
                  type="button"
                  onClick={() => onSelect(emoticon)}
                  className={cn(
                    "p-1 rounded-lg hover:bg-[#FEE500]/20 transition-colors",
                    "flex flex-col items-center gap-1",
                  )}
                >
                  {/* 카카오프렌즈 스타일 큰 이모티콘 이미지 */}
                  <img
                    src={emoticon.imageUrl || "/placeholder.svg"}
                    alt={emoticon.name}
                    className="w-14 h-14 object-contain"
                  />
                  <span className="text-[10px] text-muted-foreground truncate w-full text-center">{emoticon.name}</span>
                </button>
              ))}
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
