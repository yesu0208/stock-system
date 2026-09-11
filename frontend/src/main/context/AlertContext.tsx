import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import { getStockClient } from '../../api/stompClient'
import { tokenStorage } from '../../utils/token'
import { useMsg } from './MsgContext'
import type { AlertResponseMessage, AlertFiredResponse } from '../../types/alert'

/**
 * A의 TradePage.tsx가 계좌/주문/체결 등 모든 구독을 getStockClient()
 * 하나로 처리하고 있어 이 Context도 동일한 클라이언트를 재사용
 * (getOrderClient()가 실제로 쓰이는 곳이 기존 코드에 없어 보이는데, 이 부분은
 * 실제 배포 환경에서 한 번 더 확인이 필요)
 */

interface AlertContextValue {
    alerts: AlertResponseMessage[]
    lastFired: AlertFiredResponse | null
}

const AlertContext = createContext<AlertContextValue | null>(null)

export function AlertProvider({ children }: { children: ReactNode }) {
    const [alerts, setAlerts] = useState<AlertResponseMessage[]>([])
    const [lastFired, setLastFired] = useState<AlertFiredResponse | null>(null)
    const { info } = useMsg()

    useEffect(() => {
        if (!tokenStorage.get()) return

        const client = getStockClient()
        let listSub: StompSubscription | undefined
        let firedSub: StompSubscription | undefined

        client.onConnect = () => {
            listSub = client.subscribe('/user/sub/alert', (message) => {
                const data: AlertResponseMessage[] = JSON.parse(message.body)
                setAlerts(data)
            })

            firedSub = client.subscribe('/user/sub/alert/fired', (message) => {
                const data: AlertFiredResponse = JSON.parse(message.body)
                setLastFired(data)

                const directionText = data.direction === 'ABOVE' ? '이상' : '이하'
                info(
                    `${data.stockCode} 목표가(${data.triggerPrice.toLocaleString()}원 ${directionText}) 도달! ` +
                    `현재가 ${data.currentPrice.toLocaleString()}원`
                )
            })
        }

        client.activate()

        return () => {
            listSub?.unsubscribe()
            firedSub?.unsubscribe()
            client.deactivate()
        }
    }, [info])

    return (
        <AlertContext.Provider value={{ alerts, lastFired }}>
            {children}
        </AlertContext.Provider>
    )
}

export function useAlert() {
    const ctx = useContext(AlertContext)
    if (!ctx) throw new Error('useAlert는 AlertProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
