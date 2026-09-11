import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import { useMsg } from './MsgContext'
import type { AlertResponseMessage, AlertFiredResponse } from '../../types/alert'

interface AlertContextValue {
    alerts: AlertResponseMessage[]
    lastFired: AlertFiredResponse | null
}

const AlertContext = createContext<AlertContextValue | null>(null)

export function AlertProvider({ children }: { children: ReactNode }) {
    const [alerts, setAlerts] = useState<AlertResponseMessage[]>([])
    const [lastFired, setLastFired] = useState<AlertFiredResponse | null>(null)
    const { subscribeDestination } = useRealtime()
    const { info } = useMsg()

    useEffect(() => {
        const unsubList = subscribeDestination('/user/sub/alert', (data: AlertResponseMessage[]) => {
            setAlerts(data)
        })

        const unsubFired = subscribeDestination('/user/sub/alert/fired', (data: AlertFiredResponse) => {
            setLastFired(data)
            const directionText = data.direction === 'ABOVE' ? '이상' : '이하'
            info(
                `${data.stockCode} 목표가(${data.triggerPrice.toLocaleString()}원 ${directionText}) 도달! ` +
                `현재가 ${data.currentPrice.toLocaleString()}원`
            )
        })

        return () => {
            unsubList()
            unsubFired()
        }
    }, [subscribeDestination, info])

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
