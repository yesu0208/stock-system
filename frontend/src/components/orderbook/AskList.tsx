import { useStockRealtime } from '../../main/context/StockRealtimeContext'
import { useOrderPrice } from '../../main/context/OrderPriceContext'
import { useStock } from '../../main/context/StockContext'
import { usePendingOrders } from '../../main/context/PendingOrderContext'

import styles from './AskList.module.css'

interface PriceLevel {
    price: number
    quantity: number
}

interface AskListProps {
    asks: PriceLevel[]
    prevClosePrice?: number
    maxQty?: number
}

export default function AskList({
                                    asks,
                                    prevClosePrice = 0,
                                    maxQty = 0,
                                }: AskListProps) {

    const { priceTick: latestTick } = useStockRealtime()
    const { setSelectedPrice } = useOrderPrice()
    const { selectedStock } = useStock()
    const { pendingOrders } = usePendingOrders()

    const getPriceColor = (price: number) => {
        if (price > prevClosePrice) return '#FF6347'
        if (price < prevClosePrice) return '#4F9DFF'
        return 'white'
    }

    const getRateColor = (rate: number) => {
        if (rate === 0) return 'white'
        if (rate > 0) return '#FF6347'
        return '#4F9DFF'
    }

    const latestPrice = latestTick?.curPrice
    const lowPrice = latestTick?.lowPrice
    const highPrice = latestTick?.highPrice
    const startPrice = latestTick?.startPrice

    const rate =
        lowPrice && prevClosePrice
            ? ((lowPrice - prevClosePrice) / prevClosePrice) * 100
            : 0

    const highRate =
        highPrice && prevClosePrice
            ? ((highPrice - prevClosePrice) / prevClosePrice) * 100
            : 0

    const startRate =
        startPrice && prevClosePrice
            ? ((startPrice - prevClosePrice) / prevClosePrice) * 100
            : 0

    const tradeRate =
        latestTick?.totalTradingVolume &&
        latestTick?.prevDaySameTimeAccVolume
            ? (latestTick.totalTradingVolume /
            latestTick.prevDaySameTimeAccVolume) * 100
            : 0

    const getPriceBorder = (price: number, isEmpty: boolean): {
        outline: string
        outlineOffset: string
        zIndex: number
    } => {
        const none = { outline: 'none', outlineOffset: '0px', zIndex: 5 }
        if (isEmpty) return none

        if (latestPrice !== undefined && price === latestPrice)
            return { outline: '1px solid white', outlineOffset: '-1px', zIndex: 10 }

        return none
    }

    const getPriceTag = (price: number, isEmpty: boolean): { label: string; color: string } | null => {
        if (isEmpty) return null

        if (startPrice !== undefined && price === startPrice)
            return { label: '시가', color: '#888888' }
        if (highPrice !== undefined && price === highPrice)
            return { label: '고가', color: '#FF6347' }
        if (lowPrice !== undefined && price === lowPrice)
            return { label: '저가', color: '#4F9DFF' }

        return null
    }

    const getMyOrdersAtPrice = (price: number) =>
        pendingOrders.filter(
            (o) => !o.isAuto && o.stockCode === selectedStock.code && o.price === price
        )

    return (
        <div className={styles.container}>
            {asks.slice().reverse().map((a, idx, arr) => {

                const isEmpty = a.price === 0 || a.quantity === 0
                const widthPercent = maxQty > 0 ? (a.quantity / maxQty) * 100 : 0

                const isBottomRow = idx === arr.length - 1
                const isAboveBottom = idx === arr.length - 2
                const isTwoAboveBottom = idx === arr.length - 3
                const isThreeAboveBottom = idx === arr.length - 4
                const isFourAboveBottom = idx === arr.length - 5
                const isFiveAboveBottom = idx === arr.length - 6

                const borderStyle = getPriceBorder(a.price, isEmpty)
                const priceTag = getPriceTag(a.price, isEmpty)
                const myOrders = !isEmpty ? getMyOrdersAtPrice(a.price) : []

                return (
                    <div key={idx} className={styles.row}>
                        <div className={styles.left}>
                            {!isEmpty && (
                                <>
                                    <div className={styles.bar} style={{ width: `${widthPercent}%` }} />
                                    <span className={styles.quantityText}>{a.quantity.toLocaleString()}</span>

                                    {myOrders.length > 0 && (
                                        <div className={styles.myOrderBadgeWrap}>
                                            {myOrders.some((o) => o.side === 'BUY') && (
                                                <span className={styles.myOrderTagBuy}>매</span>
                                            )}
                                            {myOrders.some((o) => o.side === 'SELL') && (
                                                <span className={styles.myOrderTagSell}>매</span>
                                            )}

                                            <div className={styles.myOrderTooltip}>
                                                {myOrders.map((o) => (
                                                    <div key={o.orderId} className={styles.tooltipRow}>
                                                        <span className={o.side === 'BUY' ? styles.tooltipSideBuy : styles.tooltipSideSell}>
                                                            {o.side === 'BUY' ? '매수' : '매도'}
                                                        </span>
                                                        <span className={styles.tooltipQty}>
                                                            {o.remainingQty.toLocaleString()}주
                                                        </span>
                                                        <span className={styles.tooltipInfo}>
                                                            {o.quantityAhead != null
                                                                ? `내 앞 ${o.quantityAhead.toLocaleString()}주`
                                                                : '-'}
                                                        </span>
                                                    </div>
                                                ))}
                                            </div>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>

                        <div
                            className={styles.price}
                            onClick={() => {
                                if (!isEmpty) setSelectedPrice(a.price)
                            }}
                            style={{
                                color: !isEmpty ? getPriceColor(a.price) : 'white',
                                outline: borderStyle.outline,
                                outlineOffset: borderStyle.outlineOffset,
                                zIndex: borderStyle.zIndex,
                                cursor: !isEmpty ? 'pointer' : 'default',
                            }}
                        >
                            {priceTag && (
                                <>
                                    <span className={styles.priceTag} style={{ backgroundColor: priceTag.color }} />
                                    <span className={styles.priceTagTooltip}>{priceTag.label}</span>
                                </>
                            )}
                            {!isEmpty ? a.price.toLocaleString() : ''}
                        </div>

                        <div className={styles.right}>
                            {isFiveAboveBottom && (
                                <>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>거래량</span>
                                        <span className={styles.smallRowRight}>
                                            {latestTick?.totalTradingVolume?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>거래액</span>
                                        <span className={styles.smallRowRight}>
                                            {latestTick?.totalTradingValue != null
                                                ? (latestTick.totalTradingValue / 100000000).toFixed(1) + '억'
                                                : '-'}
                                        </span>
                                    </div>
                                </>
                            )}

                            {isFourAboveBottom && (
                                <>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>매수</span>
                                        <span className={styles.smallRowRight} style={{ color: '#FF6347' }}>
                                            {latestTick?.totalBuyVolume?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>매도</span>
                                        <span className={styles.smallRowRight} style={{ color: '#4F9DFF' }}>
                                            {latestTick?.totalSellVolume?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                </>
                            )}

                            {isThreeAboveBottom && (
                                <>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>전일동시</span>
                                        <span className={styles.smallRowRight}>
                                            {tradeRate.toFixed(2)}%
                                        </span>
                                    </div>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>기준가</span>
                                        <span className={styles.smallRowRight}>
                                            {latestTick?.prevClosePrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                </>
                            )}

                            {isTwoAboveBottom && (
                                <>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>당일시가</span>
                                        <span className={styles.smallRowRight} style={{ color: getRateColor(startRate) }}>
                                            {startPrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}></span>
                                        <span className={styles.smallRowRight} style={{ color: getRateColor(startRate) }}>
                                            {startRate.toFixed(2)}%
                                        </span>
                                    </div>
                                </>
                            )}

                            {isAboveBottom && (
                                <>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>당일고가</span>
                                        <span className={styles.smallRowRight} style={{ color: getRateColor(highRate) }}>
                                            {highPrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}></span>
                                        <span className={styles.smallRowRight} style={{ color: getRateColor(highRate) }}>
                                            {highRate.toFixed(2)}%
                                        </span>
                                    </div>
                                </>
                            )}

                            {isBottomRow && (
                                <>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}>당일저가</span>
                                        <span className={styles.smallRowRight} style={{ color: getRateColor(rate) }}>
                                            {lowPrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div className={styles.smallRow}>
                                        <span className={styles.smallRowLeft}></span>
                                        <span className={styles.smallRowRight} style={{ color: getRateColor(rate) }}>
                                            {rate.toFixed(2)}%
                                        </span>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                )
            })}
        </div>
    )
}
