import AskList from './AskList'
import BidList from './BidList'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import styles from './OrderBook.module.css'

interface PriceLevel {
    price: number
    quantity: number
}

interface OrderBookProps {
    stockName: string
    asks: PriceLevel[]
    bids: PriceLevel[]
    tradeTicks?: TradePriceTickMessage[]
    prevClosePrice?: number
    isReady: boolean
    isRealtimeSupported?: boolean
}

export default function OrderBook({
                                      stockName,
                                      asks,
                                      bids,
                                      tradeTicks = [],
                                      prevClosePrice = 0,
                                      isReady,
                                      isRealtimeSupported = true,
                                  }: OrderBookProps) {

    const totalAsk = asks.reduce((sum, a) => sum + a.quantity, 0)
    const totalBid = bids.reduce((sum, b) => sum + b.quantity, 0)

    const latestPrice = tradeTicks[tradeTicks.length - 1]?.curPrice ?? 0
    const change = latestPrice - prevClosePrice
    const changePercent = prevClosePrice ? (change / prevClosePrice) * 100 : 0

    const changeClass =
        change > 0 ? styles.changeUp : change < 0 ? styles.changeDown : styles.changeFlat

    const maxQty = Math.max(
        1,
        ...asks.map(a => a.quantity),
        ...bids.map(b => b.quantity)
    )

    return (
        <div className={styles.wrapper}>
            {/* 실시간 미지원 종목이면 무한 로딩 대신 명확한 안내 */}
            {!isRealtimeSupported ? (
                <div className={styles.loading}>
                    실시간 미지원 종목입니다. 호가창을 제공하지 않습니다.
                </div>
            ) : !isReady ? (
                <div className={styles.loading}>
                    호가창 생성중...
                </div>
            ) : (
                <div className={styles.container}>
                    <div className={styles.header}>
                        <h2 className={styles.stockName}>{stockName}</h2>
                        <div className={styles.priceInfo}>
                        <span className={styles.latestPrice}>
                            {latestPrice.toLocaleString()}
                        </span>
                            <span className={`${styles.change} ${changeClass}`}>
                            {change > 0 ? '+' : ''}
                                {change.toLocaleString()} ({changePercent.toFixed(2)}%)
                        </span>
                        </div>
                    </div>

                    <AskList
                        asks={asks}
                        tradeTicks={tradeTicks}
                        prevClosePrice={prevClosePrice}
                        maxQty={maxQty}
                    />

                    <BidList
                        bids={bids}
                        tradeTicks={tradeTicks}
                        prevClosePrice={prevClosePrice}
                        maxQty={maxQty}
                    />

                    <div className={styles.footer}>
                        <span>{totalAsk}</span>
                        <span>총 잔량</span>
                        <span>{totalBid}</span>
                    </div>
                </div>
            )}
        </div>
    )
}
