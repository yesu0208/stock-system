import { useState } from 'react'
import { createPortal } from 'react-dom'
import { useStockRealtime } from '../../main/context/StockRealtimeContext'
import { useOrderPrice } from '../../main/context/OrderPriceContext'
import { useStock } from '../../main/context/StockContext'
import { usePendingOrders, type PendingOrder } from '../../main/context/PendingOrderContext'

import styles from './AskList.module.css'

const ORIGIN_LABEL: Record<string, string> = {
    OTOCO_ENTRY: 'OTOCO 진입',
    OTOCO_TAKE_PROFIT: 'OTOCO 익절',
    OTOCO_STOP_LOSS: 'OTOCO 손절',
    TRAILING_STOP: '트레일링',
    AUTO_ORDER: '자동주문',
}

function formatTimeOnly(isoString: string): string {
    const d = new Date(isoString)
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

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

    const [hoveredBadge, setHoveredBadge] = useState<{
        x: number
        y: number
        orders: PendingOrder[]
    } | null>(null)

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
                                        <div
                                            className={styles.myOrderBadgeWrap}
                                            onMouseEnter={(e) => {
                                                const rect = e.currentTarget.getBoundingClientRect()
                                                setHoveredBadge({
                                                    x: rect.left + rect.width / 2,
                                                    y: rect.top,
                                                    orders: myOrders,
                                                })
                                            }}
                                            onMouseLeave={() => setHoveredBadge(null)}
                                        >
                                            {myOrders.some((o) => o.side === 'BUY') && (
                                                <span className={styles.myOrderTagBuy}>매</span>
                                            )}
                                            {myOrders.some((o) => o.side === 'SELL') && (
                                                <span className={styles.myOrderTagSell}>매</span>
                                            )}
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

            {hoveredBadge && createPortal(
                <div
                    className={styles.myOrderTooltipPortal}
                    style={{ left: hoveredBadge.x, top: hoveredBadge.y }}
                >
                    <div className={styles.tooltipHeader}>
                        <span className={styles.tooltipHeaderPrice}>
                            {hoveredBadge.orders[0].price.toLocaleString()}원
                        </span>
                        <span className={styles.tooltipHeaderLabel}>미체결 주문 현황</span>
                    </div>

                    {hoveredBadge.orders.map((o) => (
                        <div key={o.orderId} className={styles.tooltipRow}>
                            <div className={styles.tooltipRowTop}>
                                <span className={o.side === 'BUY' ? styles.tooltipSideBuy : styles.tooltipSideSell}>
                                    {o.side === 'BUY' ? '매수' : '매도'}
                                </span>
                                <span className={`${styles.tooltipLeverage} ${o.leverage === 1 ? styles.cash : styles.lev}`}>
                                    {o.leverage === 1 ? '현금' : `${o.leverage}x`}
                                </span>
                                {o.origin && o.origin !== 'MANUAL' && (
                                    <span className={styles.tooltipOrigin}>
                                        {ORIGIN_LABEL[o.origin] ?? o.origin}
                                    </span>
                                )}
                                <span className={styles.tooltipTimeText}>{formatTimeOnly(o.time)}</span>
                            </div>
                            <div className={styles.tooltipRowTop}>
                                <span className={styles.tooltipQty}>
                                    {o.remainingQty.toLocaleString()}주
                                </span>
                                <span className={styles.tooltipInfo}>
                                    {o.quantityAhead != null
                                        ? `내 앞 ${o.quantityAhead.toLocaleString()}주`
                                        : '-'}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>,
                document.body
            )}
        </div>
    )
}
