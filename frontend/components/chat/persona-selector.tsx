"use client"

import { type Persona, PERSONAS } from "@/lib/types"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Check, ChevronDown, Sparkles } from "lucide-react"

interface PersonaSelectorProps {
  selectedPersona: Persona
  recommendedPersona?: Persona
  onSelect: (persona: Persona) => void
}

export function PersonaSelector({ selectedPersona, recommendedPersona, onSelect }: PersonaSelectorProps) {
  return (
    <Popover>
      <PopoverTrigger asChild>
        <Button variant="outline" className="gap-2 bg-transparent">
          <span className="text-lg">{selectedPersona.icon}</span>
          <span>{selectedPersona.name}</span>
          <ChevronDown className="h-4 w-4 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-64 p-2" align="start">
        <div className="space-y-1">
          {PERSONAS.map((persona) => {
            const isRecommended = recommendedPersona?.id === persona.id
            const isSelected = selectedPersona.id === persona.id

            return (
              <button
                key={persona.id}
                onClick={() => onSelect(persona)}
                className={cn(
                  "w-full flex items-center gap-3 p-2 rounded-lg transition-colors text-left",
                  isSelected ? "bg-primary/10" : "hover:bg-muted",
                )}
              >
                <span className="text-xl">{persona.icon}</span>
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className={cn("font-medium text-sm", isSelected && "text-primary")}>{persona.name}</span>
                    {isRecommended && (
                      <span className="flex items-center gap-1 text-xs text-primary bg-primary/10 px-1.5 py-0.5 rounded-full">
                        <Sparkles className="h-3 w-3" />
                        AI 추천
                      </span>
                    )}
                  </div>
                  <span className="text-xs text-muted-foreground">{persona.description}</span>
                </div>
                {isSelected && <Check className="h-4 w-4 text-primary" />}
              </button>
            )
          })}
        </div>
      </PopoverContent>
    </Popover>
  )
}
