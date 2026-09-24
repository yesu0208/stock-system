import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { IMessage, StompSubscription } from '@stomp/stompjs'
import { getStockClient, reconnectStomp } from '../../api/stompClient'
import { tokenStorage } from '../../utils/token'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import type { BidAskPriceTickMessage } from '../../types/bidAskPriceTickMessage'

export type StockTickMessage = TradePriceTickMessage | BidAskPriceTickMessage

interface RealtimeContextValue {
    /** 임의의 STOMP destination을 구독한다. 연결 전이면 연결될 때 자동 구독. */
    subscribeDestination: (destination: string, onMessage: (body: any) => void) => () => void
    /** /sub/stock/{code} 전용 편의 함수 */
    subscribeStock: (code: string, onTick: (tick: StockTickMessage) => void) => () => void
    /** 클라이언트 -> 서버로 STOMP 메시지를 보낸다 (예: 종목톡 입장/퇴장/전송) */
    publish: (destination: string, body: unknown) => void
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
        // 토큰 유무와 무관하게 연결한다.
        // 토큰이 없으면 익명 연결(공개 채널만 구독 가능), 있으면 인증 연결.
        const client = getStockClient()

        client.onConnect = () => {
            setConnected(true)
            listenersRef.current.forEach((_set, destination) => ensureSubscribed(destination))
        }
        client.onWebSocketClose = () => setConnected(false)

        client.activate()

        // 로그인/로그아웃으로 토큰이 바뀌면 연결 상태를 갱신
        const unsubscribeToken = tokenStorage.subscribe((token) => {
            setConnected(false)

            if (token) {
                // 로그인: 기존(익명) 연결에 인증 헤더를 새로 실어 재연결
                reconnectStomp()
                return
            }

            // 로그아웃: api/auth.ts의 logout()이 곧이어 disconnectStomp()를
            // 호출해 클라이언트를 정리하므로, 한 틱 뒤 새 토큰(없음) 기준으로
            // 다시 연결해 로그인 화면의 시세 표시를 이어간다.
            window.setTimeout(() => {
                const freshClient = getStockClient()
                freshClient.onConnect = () => {
                    setConnected(true)
                    listenersRef.current.forEach((_set, destination) => ensureSubscribed(destination))
                }
                freshClient.onWebSocketClose = () => setConnected(false)
                if (!freshClient.active) freshClient.activate()
            }, 0)
        })

        return () => {
            unsubscribeToken()
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
        (code: string, onTick: (tick: StockTickMessage) => void) => {
            // 실시간 틱은 공용 종목 채널로, 초기 스냅샷(호가·체결가·상세·일봉·분봉)은
            // 구독한 세션 전용 채널로 받음. 스냅샷을 종목 구독자 전원에게 다시 보내지 않기 위함.
            // 실시간 채널을 먼저 구독해, 스냅샷 이후의 틱을 놓치지 않게 함.
            const unsubscribeLive = subscribeDestination(`/sub/stock/${code}`, onTick)
            const unsubscribeSnapshot = subscribeDestination(`/user/sub/stock/${code}/snapshot`, onTick)

            return () => {
                unsubscribeLive()
                unsubscribeSnapshot()
            }
        },
        [subscribeDestination]
    )

    const publish = useCallback((destination: string, body: unknown) => {
        const client = getStockClient()
        if (!client.connected) return
        client.publish({ destination, body: JSON.stringify(body ?? {}) })
    }, [])

    return (
        <RealtimeContext.Provider value={{ subscribeDestination, subscribeStock, publish, connected }}>
            {children}
        </RealtimeContext.Provider>
    )
}

export function useRealtime() {
    const ctx = useContext(RealtimeContext)
    if (!ctx) throw new Error('useRealtime은 RealtimeProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
