import { useEffect, useState } from 'react'
import { useRealtime } from '../context/RealtimeContext'
import type { CandleData, DailyChartSnapshotMessage, DailyCandleTickMessage } from '../../types/candle'

/**
 * useDailyCandles
 * useMinuteCandles와 동일한 패턴. 백엔드 RedisDailyCandleEventSubscriber.java,
 * DailyChartSnapshotMessage.java, DailyCandleTickMessage.java 확인 결과
 * 같은 '/sub/stock/{code}' 토픽에 DAILY_CHART_SNAPSHOT(스냅샷)/
 * DAILY_CANDLE(갱신)이 함께 흘러옴.
 */
export function useDailyCandles(stockCode: string) {
    const { subscribeStock } = useRealtime()
    const [candles, setCandles] = useState<CandleData[]>([])

    useEffect(() => {
        setCandles([])

        return subscribeStock(stockCode, (tick: any) => {
            if (tick.tickMessageType === 'DAILY_CHART_SNAPSHOT') {
                setCandles((tick as DailyChartSnapshotMessage).candles)
            }

            if (tick.tickMessageType === 'DAILY_CANDLE') {
                const { candle } = tick as DailyCandleTickMessage
                setCandles(prev => {
                    const last = prev[prev.length - 1]
                    if (last && last.date === candle.date) {
                        return [...prev.slice(0, -1), candle]
                    }
                    return [...prev, candle]
                })
            }
        })
    }, [stockCode, subscribeStock])

    return candles
}
