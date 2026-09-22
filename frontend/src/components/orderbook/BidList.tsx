import { useStockRealtime } from '../../main/context/StockRealtimeContext'
import { useOrderPrice } from '../../main/context/OrderPriceContext'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'

import styles from './BidList.module.css'

interface BidLevel {
    price: number
    quantity: number
}

interface BidListProps {
    bids: BidLevel[]
    prevClosePrice?: number
    maxQty?: number
    accTicks?: TradePriceTickMessage[]
}

export default function BidList({
                                    bids,
                                    prevClosePrice = 0,
                                    maxQty = 0,
                                    accTicks = [],
                                }: BidListProps) {

    const { priceTick } = useStockRealtime()
    const { setSelectedPrice } = useOrderPrice()

    const getPriceColor = (price: number) => {
        if (price > prevClosePrice) return '#FF6347'
        if (price < prevClosePrice) return '#4F9DFF'
        return 'white'
    }

    const getVolumeColor = (tick: TradePriceTickMessage) => {
        if (tick.tradingType === '1') return '#FF6347'
        if (tick.tradingType === '5') return '#4F9DFF'
        return 'white'
    }

    const latestPrice = priceTick?.curPrice
    const startPrice = priceTick?.startPrice
    const highPrice = priceTick?.highPrice
    const lowPrice = priceTick?.lowPrice

    const getPriceBorder = (price: number, isEmpty: boolean): {
        outline: string
        outlineOffset: string
        zIndex: number
    } => {
        const none = { outline: 'none', outlineOffset: '0px', zIndex: 0 }
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

    const ticksToShow = accTicks.slice(0, bids.length * 2)

    const tickChunks: TradePriceTickMessage[][] = []
    let tickIndex = 0
    for (let i = 1; i < bids.length; i++) {
        const chunk = ticksToShow.slice(tickIndex, tickIndex + 2)
        tickChunks.push(chunk)
        tickIndex += 2
        if (tickIndex >= ticksToShow.length) break
    }

    const calcStrength = () => {
        if (!priceTick) return 0
        const buy = Number(priceTick.totalBuyVolume)
        const sell = Number(priceTick.totalSellVolume)
        if (!buy || buy <= 0) return 0
        return +((buy / sell) * 100).toFixed(2)
    }

    const strength = calcStrength()

    const getStrengthColor = (value: number) => {
        if (value === 100) return 'white'
        if (value > 100) return '#FF6347'
        return '#4F9DFF'
    }

    return (
        <div className={styles.container}>
            {bids.map((b, idx) => {

                const isEmpty = b.price === 0 || b.quantity === 0
                const widthPercent = maxQty > 0 ? (b.quantity / maxQty) * 100 : 0
                const rowTicks = idx === 0 ? [] : tickChunks[idx - 1] || []

                const borderStyle = getPriceBorder(b.price, isEmpty)
                const priceTag = getPriceTag(b.price, isEmpty)

                return (
                    <div key={idx} className={styles.row}>
                        <div className={styles.left}>
                            {idx === 0 ? (
                                <>
                                    <div className={styles.topLeft}>
                                        <span>체결강도</span>
                                        <span style={{ color: getStrengthColor(strength) }}>
                                            {strength.toFixed(2)}%
                                        </span>
                                    </div>
                                    <div className={styles.bottomLeft}>
                                        <span>체결가</span>
                                        <span>체결량</span>
                                    </div>
                                </>
                            ) : (
                                <div className={styles.tickList}>
                                    {rowTicks.map((tick, i) => (
                                        <div key={i} className={styles.tickRow}>
                                            <span style={{ color: getPriceColor(tick.curPrice) }}>
                                                {tick.curPrice.toLocaleString()}
                                            </span>
                                            <span style={{ color: getVolumeColor(tick) }}>
                                                {Number(tick.tradingVolumeTick).toLocaleString()}
                                            </span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>

                        <div
                            className={styles.price}
                            onClick={() => {
                                if (!isEmpty) setSelectedPrice(b.price)
                            }}
                            style={{
                                color: !isEmpty ? getPriceColor(b.price) : 'white',
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
                            {!isEmpty ? b.price.toLocaleString() : ''}
                        </div>

                        <div className={styles.right}>
                            {!isEmpty && (
                                <div className={styles.bar} style={{ width: `${widthPercent}%` }} />
                            )}
                            <span className={styles.quantityText}>
                                {!isEmpty ? b.quantity.toLocaleString() : ''}
                            </span>
                        </div>
                    </div>
                )
            })}
        </div>
    )
}
