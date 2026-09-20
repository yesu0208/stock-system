import { useEffect } from 'react'
import { useRealtime } from '../context/RealtimeContext'
import { useUser } from '../context/UserContext'
import { useMsg } from '../context/MsgContext'

interface EventMessage {
    message: string
}

interface CloseMessage {
    session: 'REGULAR' | 'AFTER'
    message: string
}

/**
 * 로그인 후에만 의미 있는 계좌/장운영 알림 3종을 구독
 * - /sub/market/rank: RP·당일 수익률 정산
 * - /sub/market/interest: 신용거래 이자 반영
 * - /sub/market/close: 정규장/애프터마켓 미체결 정리·정산 완료
 */
export default function MarketEventsListener() {
    const { subscribeDestination } = useRealtime()
    const { refresh } = useUser()
    const { success } = useMsg()

    useEffect(() => {
        return subscribeDestination('/sub/market/rank', (data: EventMessage) => {
            refresh()
            success(data.message)
        })
    }, [subscribeDestination, refresh, success])

    useEffect(() => {
        return subscribeDestination('/sub/market/interest', (data: EventMessage) => {
            success(data.message)
        })
    }, [subscribeDestination, success])

    useEffect(() => {
        return subscribeDestination('/sub/market/close', (data: CloseMessage) => {
            success(data.message)
        })
    }, [subscribeDestination, success])

    return null
}
