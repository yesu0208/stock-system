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
const EMPTY: MinuteCandle[] = []

export function useMinuteCandles(stockCode: string) {
    const { subscribeStock } = useRealtime()
    // 캔들이 어느 종목 것인지 함께 저장 (종목 변경 직후 이전 종목 캔들이 반환되지 않도록)
    const [state, setState] = useState<{ code: string; candles: MinuteCandle[] }>({
        code: stockCode,
        candles: EMPTY,
    })

    useEffect(() => {
        // 종목 변경 시 별도 초기화 불필요: 반환 시 code가 다르면 빈 배열을 돌려줌
        return subscribeStock(stockCode, (tick: any) => {
            if (tick.tickMessageType === 'MINUTE_CHART_SNAPSHOT') {
                setState({ code: stockCode, candles: (tick as MinuteChartSnapshotMessage).candles })
            }

            if (tick.tickMessageType === 'MINUTE_CANDLE') {
                const { candle } = tick as MinuteCandleTickMessage
                setState(prev => {
                    const prevCandles = prev.code === stockCode ? prev.candles : EMPTY
                    const last = prevCandles[prevCandles.length - 1]
                    // 같은 분(date+time)이면 마지막 캔들 갱신, 새 분이면 추가
                    if (last && last.date === candle.date && last.time === candle.time) {
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
