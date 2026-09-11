import { useEffect, useState } from 'react'
import { useRealtime } from '../../main/context/RealtimeContext'
import { calcStockStats, isRealtimeStock } from '../../utils/stockUtils'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'

interface Props {
    stockCode: string
    stockName: string
}

export default function StockInfoPanel({ stockCode, stockName }: Props) {
    const { subscribeStock } = useRealtime()
    const [priceTick, setPriceTick] = useState<TradePriceTickMessage | null>(null)

    useEffect(() => {
        setPriceTick(null)
        return subscribeStock(stockCode, (tick: any) => {
            if (tick.tickMessageType === 'TRADEPRICE') {
                setPriceTick(tick as TradePriceTickMessage)
            }
        })
    }, [stockCode, subscribeStock])

    if (!isRealtimeStock(stockCode)) {
        return (
            <div style={styles.container}>
                <div style={styles.title}>{stockName}</div>
                <div style={styles.notice}>실시간 미지원 종목입니다.</div>
            </div>
        )
    }

    const stats = priceTick ? calcStockStats(true, priceTick, null) : null

    return (
        <div style={styles.container}>
            <div style={styles.title}>{stockName}</div>
            {!stats ? (
                <div style={styles.notice}>시세 수신 대기중...</div>
            ) : (
                <div style={styles.grid}>
                    <Row label="시가" value={stats.open.price} />
                    <Row label="고가" value={stats.high.price} />
                    <Row label="저가" value={stats.low.price} />
                    <Row label="거래량" value={stats.volume} />
                    <Row label="거래대금(백만)" value={stats.tradingValue} />
                </div>
            )}
        </div>
    )
}

function Row({ label, value }: { label: string; value: string }) {
    return (
        <div style={styles.row}>
            <span style={styles.label}>{label}</span>
            <span style={styles.value}>{value}</span>
        </div>
    )
}

const styles = {
    container: { padding: '12px 16px', backgroundColor: '#1A1A1A', borderRadius: '8px', color: '#FFF' },
    title: { fontSize: '14px', fontWeight: 'bold', marginBottom: '8px' },
    notice: { fontSize: '13px', color: '#888' },
    grid: { display: 'flex', flexDirection: 'column' as const, gap: '4px' },
    row: { display: 'flex', justifyContent: 'space-between', fontSize: '13px' },
    label: { color: '#AAA' },
    value: { color: '#FFF' },
} as const
