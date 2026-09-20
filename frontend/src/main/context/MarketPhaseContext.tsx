import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import instance from '../../api/axios'

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

const PHASE_LABEL: Record<MarketPhase, string> = {
    MORNING_CALL: '동시호가',
    OPEN: '정규장',
    CLOSING_CALL: '동시호가',
    AFTER: 'AFTER',
    CLOSED: '장마감',
}

const MarketPhaseContext = createContext<MarketPhaseContextValue | null>(null)

export function MarketPhaseProvider({ children }: { children: ReactNode }) {
    const [phase, setPhase] = useState<MarketPhase | null>(null)
    const [label, setLabel] = useState<string | null>(null)
    const { subscribeDestination } = useRealtime()

    useEffect(() => {
        instance
            .get<{ phase: MarketPhase }>('/market/phase')
            .then((res) => {
                const p = res.data.phase
                setPhase(p)
                setLabel(PHASE_LABEL[p])
            })
            .catch((e) => console.error('초기 장 상태 조회 실패', e))
    }, [])

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
