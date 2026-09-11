import { useEffect, useState } from 'react'
import { useRealtime } from '../context/RealtimeContext'
import type { MinuteCandle, MinuteChartSnapshotMessage, MinuteCandleTickMessage } from '../../types/candle'

/**
 * useMinuteCandles
 * 백엔드 RedisMinuteCandleEventSubscriber.java, MinuteChartSnapshotMessage.java,
 * MinuteCandleTickMessage.java 확인 결과, '/sub/stock/{code}' 토픽에
 * 시세뿐 아니라 분봉 스냅샷(최초 접속 시 과거 캔들 전체, MINUTE_CHART_SNAPSHOT)과
 * 분봉 갱신(1건씩, MINUTE_CANDLE)도 함께 흘러옴.
 * RealtimeContext(5단계)의 subscribeStock을 그대로 재사용
 */
export function useMinuteCandles(stockCode: string) {
    const { subscribeStock } = useRealtime()
    const [candles, setCandles] = useState<MinuteCandle[]>([])

    useEffect(() => {
        setCandles([])

        return subscribeStock(stockCode, (tick: any) => {
            if (tick.tickMessageType === 'MINUTE_CHART_SNAPSHOT') {
                setCandles((tick as MinuteChartSnapshotMessage).candles)
            }

            if (tick.tickMessageType === 'MINUTE_CANDLE') {
                const { candle } = tick as MinuteCandleTickMessage
                setCandles(prev => {
                    const last = prev[prev.length - 1]
                    // 같은 분(date+time)이면 마지막 캔들 갱신, 새 분이면 추가
                    if (last && last.date === candle.date && last.time === candle.time) {
                        return [...prev.slice(0, -1), candle]
                    }
                    return [...prev, candle]
                })
            }
        })
    }, [stockCode, subscribeStock])

    return candles
}
