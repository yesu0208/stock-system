import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { getStockClient } from '../../api/stompClient'
import { tokenStorage } from '../../utils/token'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import type { BidAskPriceTickMessage } from '../../types/bidAskPriceTickMessage'

export type StockTickMessage = TradePriceTickMessage | BidAskPriceTickMessage

interface RealtimeContextValue {
    subscribeStock: (code: string, onTick: (tick: StockTickMessage) => void) => () => void
    connected: boolean
}

const RealtimeContext = createContext<RealtimeContextValue | null>(null)

export function RealtimeProvider({ children }: { children: ReactNode }) {
    const [connected, setConnected] = useState(false)
    const listenersRef = useRef<Map<string, Set<(tick: StockTickMessage) => void>>>(new Map())
    const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map())

    const ensureSubscribed = useCallback((code: string) => {
        const client = getStockClient()
        if (!client.connected || subscriptionsRef.current.has(code)) return

        const sub = client.subscribe(`/sub/stock/${code}`, (message: IMessage) => {
            const tick: StockTickMessage = JSON.parse(message.body)
            listenersRef.current.get(code)?.forEach(fn => fn(tick))
        })
        subscriptionsRef.current.set(code, sub)
    }, [])

    useEffect(() => {
        //  로그인 안 된 상태면 STOMP 연결을 시도 x
        if (!tokenStorage.get()) return

        const client = getStockClient()

        client.onConnect = () => {
            setConnected(true)
            listenersRef.current.forEach((_set, code) => ensureSubscribed(code))
        }
        client.onWebSocketClose = () => setConnected(false)

        client.activate()

        return () => {
            subscriptionsRef.current.forEach(sub => sub.unsubscribe())
            subscriptionsRef.current.clear()
            client.deactivate()
        }
    }, [ensureSubscribed])

    const subscribeStock = useCallback(
        (code: string, onTick: (tick: StockTickMessage) => void) => {
            if (!listenersRef.current.has(code)) {
                listenersRef.current.set(code, new Set())
            }
            listenersRef.current.get(code)!.add(onTick)
            ensureSubscribed(code)

            return () => {
                const set = listenersRef.current.get(code)
                set?.delete(onTick)
                if (set && set.size === 0) {
                    listenersRef.current.delete(code)
                    subscriptionsRef.current.get(code)?.unsubscribe()
                    subscriptionsRef.current.delete(code)
                }
            }
        },
        [ensureSubscribed]
    )

    return (
        <RealtimeContext.Provider value={{ subscribeStock, connected }}>
            {children}
        </RealtimeContext.Provider>
    )
}

export function useRealtime() {
    const ctx = useContext(RealtimeContext)
    if (!ctx) throw new Error('useRealtime은 RealtimeProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
