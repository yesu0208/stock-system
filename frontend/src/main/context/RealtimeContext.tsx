import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { getStockClient } from '../../api/stompClient'
import { tokenStorage } from '../../utils/token'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import type { BidAskPriceTickMessage } from '../../types/bidAskPriceTickMessage'

export type StockTickMessage = TradePriceTickMessage | BidAskPriceTickMessage

interface RealtimeContextValue {
    /** 임의의 STOMP destination을 구독한다. 연결 전이면 연결될 때 자동 구독. */
    subscribeDestination: (destination: string, onMessage: (body: any) => void) => () => void
    /** /sub/stock/{code} 전용 편의 함수 */
    subscribeStock: (code: string, onTick: (tick: StockTickMessage) => void) => () => void
    connected: boolean
}

const RealtimeContext = createContext<RealtimeContextValue | null>(null)

export function RealtimeProvider({ children }: { children: ReactNode }) {
    const [connected, setConnected] = useState(false)
    const listenersRef = useRef<Map<string, Set<(body: any) => void>>>(new Map())
    const subscriptionsRef = useRef<Map<string, StompSubscription>>(new Map())

    const ensureSubscribed = useCallback((destination: string) => {
        const client = getStockClient()
        if (!client.connected || subscriptionsRef.current.has(destination)) return

        const sub = client.subscribe(destination, (message: IMessage) => {
            const body = JSON.parse(message.body)
            listenersRef.current.get(destination)?.forEach(fn => fn(body))
        })
        subscriptionsRef.current.set(destination, sub)
    }, [])

    useEffect(() => {
        if (!tokenStorage.get()) return

        const client = getStockClient()

        client.onConnect = () => {
            setConnected(true)
            listenersRef.current.forEach((_set, destination) => ensureSubscribed(destination))
        }
        client.onWebSocketClose = () => setConnected(false)

        client.activate()

        return () => {
            subscriptionsRef.current.forEach(sub => sub.unsubscribe())
            subscriptionsRef.current.clear()
            client.deactivate()
        }
    }, [ensureSubscribed])

    const subscribeDestination = useCallback(
        (destination: string, onMessage: (body: any) => void) => {
            if (!listenersRef.current.has(destination)) {
                listenersRef.current.set(destination, new Set())
            }
            listenersRef.current.get(destination)!.add(onMessage)
            ensureSubscribed(destination)

            return () => {
                const set = listenersRef.current.get(destination)
                set?.delete(onMessage)
                if (set && set.size === 0) {
                    listenersRef.current.delete(destination)
                    subscriptionsRef.current.get(destination)?.unsubscribe()
                    subscriptionsRef.current.delete(destination)
                }
            }
        },
        [ensureSubscribed]
    )

    const subscribeStock = useCallback(
        (code: string, onTick: (tick: StockTickMessage) => void) =>
            subscribeDestination(`/sub/stock/${code}`, onTick),
        [subscribeDestination]
    )

    return (
        <RealtimeContext.Provider value={{ subscribeDestination, subscribeStock, connected }}>
            {children}
        </RealtimeContext.Provider>
    )
}

export function useRealtime() {
    const ctx = useContext(RealtimeContext)
    if (!ctx) throw new Error('useRealtime은 RealtimeProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
