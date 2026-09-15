import { useRef, useEffect, useState } from 'react'
import AskList from './AskList'
import BidList from './BidList'
import { useStockRealtime } from '../../main/context/StockRealtimeContext'
import { useStock } from '../../main/context/StockContext'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'

import styles from './OrderBook.module.css'

const MAX_TICKS = 40

export default function OrderBook() {

    const { selectedStock } = useStock()
    const { priceTick, orderbook, connected } = useStockRealtime()

    const scrollRef = useRef<HTMLDivElement>(null)
    const initialScrollDone = useRef(false)

    const [accTicks, setAccTicks] = useState<TradePriceTickMessage[]>([])

    const asks = orderbook?.asks ?? []
    const bids = orderbook?.bids ?? []

    const maxQty = Math.max(
        1,
        ...asks.map(a => a.quantity),
        ...bids.map(b => b.quantity)
    )

    const totalAsk = asks.reduce((sum, a) => sum + a.quantity, 0)
    const totalBid = bids.reduce((sum, b) => sum + b.quantity, 0)

    useEffect(() => {
        setAccTicks([])
        initialScrollDone.current = false
    }, [selectedStock.code])

    useEffect(() => {
        if (!priceTick) return
        setAccTicks(prev => [priceTick, ...prev].slice(0, MAX_TICKS))
    }, [priceTick])

    useEffect(() => {
        if (initialScrollDone.current) return
        if (!scrollRef.current) return
        if (asks.length === 0 && bids.length === 0) return

        const el = scrollRef.current
        el.scrollTop = (el.scrollHeight - el.clientHeight) / 2
        initialScrollDone.current = true
    }, [asks, bids])

    return (
        <div className={styles.container}>
            {!selectedStock.realtimeSupported ? (
                <div className={styles.unsupportedOverlay}>
                    <p className={styles.unsupportedMsg}>
                        실시간 호가를 지원하지 않는 종목입니다
                    </p>
                </div>
            ) : !connected || (asks.length === 0 && bids.length === 0) ? (
                <div className={styles.loading}>
                    불러오는 중입니다
                </div>
            ) : (
                <div
                    className={`${styles.inner} ${styles.fadeIn}`}
                    key={selectedStock.code}
                >
                    <div className={styles.header}>
                        호가
                    </div>

                    <div className={styles.scrollArea} ref={scrollRef}>
                        <AskList
                            asks={asks}
                            prevClosePrice={priceTick?.prevClosePrice ?? 0}
                            maxQty={maxQty}
                        />
                        <BidList
                            bids={bids}
                            prevClosePrice={priceTick?.prevClosePrice ?? 0}
                            maxQty={maxQty}
                            accTicks={accTicks}
                        />
                    </div>

                    <div className={styles.footer}>
                        <span className={styles.footerAsk}>{totalAsk.toLocaleString()}</span>
                        <span className={styles.footerLabel}>총 잔량</span>
                        <span className={styles.footerBid}>{totalBid.toLocaleString()}</span>
                    </div>
                </div>
            )}
        </div>
    )
}
