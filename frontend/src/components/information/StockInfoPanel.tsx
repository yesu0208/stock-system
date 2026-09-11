import { useEffect, useState } from 'react'
import { useRealtime } from '../../main/context/RealtimeContext'
import { calcStockStats, isRealtimeStock } from '../../utils/stockUtils'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import styles from './StockInfoPanel.module.css'

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
            <div className={styles.container}>
                <div className={styles.title}>{stockName}</div>
                <div className={styles.notice}>실시간 미지원 종목입니다.</div>
            </div>
        )
    }

    const stats = priceTick ? calcStockStats(true, priceTick, null) : null

    return (
        <div className={styles.container}>
            <div className={styles.title}>{stockName}</div>
            {!stats ? (
                <div className={styles.notice}>시세 수신 대기중...</div>
            ) : (
                <div className={styles.grid}>
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
        <div className={styles.row}>
            <span className={styles.label}>{label}</span>
            <span className={styles.value}>{value}</span>
        </div>
    )
}
