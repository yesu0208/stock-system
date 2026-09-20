import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'

export type MarketPhase = 'MORNING_CALL' | 'OPEN' | 'CLOSING_CALL' | 'AFTER' | 'CLOSED'

interface MarketPhaseMessage {
    phase: MarketPhase
    label: string
}

interface MarketPhaseContextValue {
    phase: MarketPhase | null
    label: string | null
}

export function getPhaseColor(phase: MarketPhase | null): 'green' | 'yellow' | 'gray' {
    switch (phase) {
        case 'OPEN':
        case 'AFTER':
            return 'green'
        case 'MORNING_CALL':
        case 'CLOSING_CALL':
            return 'yellow'
        case 'CLOSED':
        default:
            return 'gray'
    }
}

const MarketPhaseContext = createContext<MarketPhaseContextValue | null>(null)

export function MarketPhaseProvider({ children }: { children: ReactNode }) {
    const [phase, setPhase] = useState<MarketPhase | null>(null)
    const [label, setLabel] = useState<string | null>(null)
    const { subscribeDestination } = useRealtime()

    useEffect(() => {
        return subscribeDestination('/sub/market/phase', (data: MarketPhaseMessage) => {
            setPhase(data.phase)
            setLabel(data.label)
        })
    }, [subscribeDestination])

    return (
        <MarketPhaseContext.Provider value={{ phase, label }}>
            {children}
        </MarketPhaseContext.Provider>
    )
}

export function useMarketPhase() {
    const ctx = useContext(MarketPhaseContext)
    if (!ctx) throw new Error('useMarketPhase는 MarketPhaseProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
