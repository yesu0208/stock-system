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
const EMPTY: CandleData[] = []

export function useDailyCandles(stockCode: string) {
    const { subscribeStock } = useRealtime()
    // 캔들이 어느 종목 것인지 함께 저장 (종목 변경 직후 이전 종목 캔들이 반환되지 않도록)
    const [state, setState] = useState<{ code: string; candles: CandleData[] }>({
        code: stockCode,
        candles: EMPTY,
    })

    useEffect(() => {
        // 종목 변경 시 별도 초기화 불필요: 반환 시 code가 다르면 빈 배열을 돌려줌
        return subscribeStock(stockCode, (tick: any) => {
            if (tick.tickMessageType === 'DAILY_CHART_SNAPSHOT') {
                setState({ code: stockCode, candles: (tick as DailyChartSnapshotMessage).candles })
            }

            if (tick.tickMessageType === 'DAILY_CANDLE') {
                const { candle } = tick as DailyCandleTickMessage
                setState(prev => {
                    const prevCandles = prev.code === stockCode ? prev.candles : EMPTY
                    const last = prevCandles[prevCandles.length - 1]
                    if (last && last.date === candle.date) {
                        return { code: stockCode, candles: [...prevCandles.slice(0, -1), candle] }
                    }
                    return { code: stockCode, candles: [...prevCandles, candle] }
                })
            }
        })
    }, [stockCode, subscribeStock])

    // 종목이 바뀐 첫 렌더에서는 이전 종목 캔들 대신 빈 배열 반환
    return state.code === stockCode ? state.candles : EMPTY
}
