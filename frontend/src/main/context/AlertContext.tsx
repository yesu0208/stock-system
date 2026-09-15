import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import { useMsg } from './MsgContext'
import { createAlert, cancelAlert as cancelAlertApi } from '../../api/alert'
import type { AlertResponseMessage, AlertFiredResponse, AlertDirection } from '../../types/alert'

interface ActionResult {
    success: boolean
    message: string
}

interface AlertContextValue {
    alerts: AlertResponseMessage[]
    lastFired: AlertFiredResponse | null
    registerAlert: (stockCode: string, triggerPrice: number, direction: AlertDirection) => Promise<ActionResult>
    cancelAlert: (alertId: number, stockCode: string) => Promise<ActionResult>
    getAlertsBySymbol: (stockCode: string) => AlertResponseMessage[]
}

const AlertContext = createContext<AlertContextValue | null>(null)

export function AlertProvider({ children }: { children: ReactNode }) {
    const [alerts, setAlerts] = useState<AlertResponseMessage[]>([])
    const [lastFired, setLastFired] = useState<AlertFiredResponse | null>(null)
    const { subscribeDestination } = useRealtime()
    const { info, success, error } = useMsg()

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

    const registerAlert = useCallback(
        async (stockCode: string, triggerPrice: number, direction: AlertDirection): Promise<ActionResult> => {
            try {
                await createAlert({ stockCode, direction, triggerPrice })
                success('알림이 등록되었습니다.')
                return { success: true, message: '알림이 등록되었습니다.' }
            } catch (e: any) {
                const msg = e.response?.data?.message ?? '알림 등록에 실패했습니다.'
                error(msg)
                return { success: false, message: msg }
            }
        },
        [success, error]
    )

    const cancelAlert = useCallback(
        async (alertId: number, stockCode: string): Promise<ActionResult> => {
            try {
                await cancelAlertApi(alertId, stockCode)
                success('알림이 해지되었습니다.')
                return { success: true, message: '알림이 해지되었습니다.' }
            } catch (e: any) {
                const msg = e.response?.data?.message ?? '알림 해지에 실패했습니다.'
                error(msg)
                return { success: false, message: msg }
            }
        },
        [success, error]
    )

    const getAlertsBySymbol = useCallback(
        (stockCode: string) => alerts.filter(a => a.stockCode === stockCode),
        [alerts]
    )

    return (
        <AlertContext.Provider value={{ alerts, lastFired, registerAlert, cancelAlert, getAlertsBySymbol }}>
            {children}
        </AlertContext.Provider>
    )
}

export function useAlert() {
    const ctx = useContext(AlertContext)
    if (!ctx) throw new Error('useAlert는 AlertProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
