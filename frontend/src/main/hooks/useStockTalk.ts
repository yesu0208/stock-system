import { useEffect, useState } from 'react'
import { useRealtime } from '../context/RealtimeContext'
import type { StockTalkMessage, StockTalkJoinResponse } from '../../types/stockTalk'

/**
 * useStockTalk
 * 구독: /sub/stock-talk/{ticker}(실시간 채팅), /user/sub/stock-talk/history(입장 시 최근 50개)
 * 발행: /app/stock-talk/{ticker}/join, /leave, /send (body: { content })
 *
 * RealtimeContext의 subscribeDestination + publish(이번 단계에서 추가)를 재사용
 */
export function useStockTalk(stockCode: string) {
    const { subscribeDestination, publish, connected } = useRealtime()
    const [messages, setMessages] = useState<StockTalkMessage[]>([])
    const [participantCount, setParticipantCount] = useState(0)

    useEffect(() => {
        if (!connected) return

        setMessages([])

        const unsubHistory = subscribeDestination('/user/sub/stock-talk/history', (data: StockTalkJoinResponse) => {
            if (data.ticker !== stockCode) return
            setMessages(data.messages)
            setParticipantCount(data.participantCount)
        })

        const unsubLive = subscribeDestination(`/sub/stock-talk/${stockCode}`, (data: StockTalkMessage) => {
            setMessages(prev => [...prev, data])
            setParticipantCount(data.participantCount)
        })

        publish(`/app/stock-talk/${stockCode}/join`, {})

        return () => {
            publish(`/app/stock-talk/${stockCode}/leave`, {})
            unsubHistory()
            unsubLive()
        }
    }, [stockCode, connected, subscribeDestination, publish])

    const sendMessage = (content: string) => {
        if (!content.trim()) return
        publish(`/app/stock-talk/${stockCode}/send`, { content })
    }

    return { messages, participantCount, sendMessage }
}
