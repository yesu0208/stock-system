import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'

interface BidLevel {
    price: number
    quantity: number
    strength?: number
    amount?: number
}

interface BidListProps {
    bids: BidLevel[]
    tradeTicks?: TradePriceTickMessage[]
    prevClosePrice?: number
}

export default function BidList({
                                    bids,
                                    tradeTicks = [],
                                    prevClosePrice = 0,
                                }: BidListProps) {

    const maxQty = Math.max(
        1,
        ...bids.map(b => Number(b.quantity) || 0)
    )

    // 가격 색상
    const getPriceColor = (price: number) => {
        if (price > prevClosePrice) return '#FF6347'
        if (price < prevClosePrice) return '#4F9DFF'
        return 'white'
    }

    // 체결 타입 색상
    const getVolumeColor = (tick: TradePriceTickMessage) => {
        if (tick.tradingType === '1') return '#FF6347'
        if (tick.tradingType === '5') return '#4F9DFF'
        return 'white'
    }

    // 최신 18개 체결
    const ticksToShow = [...tradeTicks.slice(-18)].reverse()
    const latestPrice =
        typeof ticksToShow[0]?.curPrice === 'number'
            ? ticksToShow[0].curPrice
            : null

    // 2개씩 chunk
    const tickChunks: TradePriceTickMessage[][] = []
    let tickIndex = 0

    for (let i = 1; i < bids.length; i++) {
        const chunk = ticksToShow.slice(tickIndex, tickIndex + 2)
        tickChunks.push(chunk)
        tickIndex += 2
        if (tickIndex >= ticksToShow.length) break
    }

    // 체결강도
    const calcStrength = () => {
        if (tradeTicks.length === 0) return 0

        const latest = tradeTicks[tradeTicks.length - 1]
        const buy = Number(latest.totalBuyVolume)
        const sell = Number(latest.totalSellVolume)

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
        <div style={styles.container}>
            {bids.map((b, idx) => {

                // row는 유지, 내용만 숨김
                const isEmpty = b.price === 0 || b.quantity === 0

                const widthPercent =
                    maxQty > 0 ? (b.quantity / maxQty) * 100 : 0

                const rowTicks =
                    idx === 0 ? [] : tickChunks[idx - 1] || []

                const isLatest =
                    latestPrice !== null && b.price === latestPrice

                return (
                    <div key={idx} style={styles.row}>
                        {/* 왼쪽 */}
                        <div style={styles.left}>
                            {idx === 0 ? (
                                <>
                                    <div style={styles.topLeft}>
                                        <span>체결강도</span>
                                        <span style={{ color: getStrengthColor(strength) }}>
                                            {strength.toFixed(2)}%
                                        </span>
                                    </div>
                                    <div style={styles.bottomLeft}>
                                        <span>체결가</span>
                                        <span>체결량</span>
                                    </div>
                                </>
                            ) : (
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                                    {rowTicks.map((tick, i) => (
                                        <div
                                            key={i}
                                            style={{
                                                fontSize: '10px',
                                                lineHeight: '12px',
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                            }}
                                        >
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

                        {/* 중앙 가격 */}
                        <div
                            style={{
                                ...styles.price,
                                color: !isEmpty ? getPriceColor(b.price) : 'white',
                                boxShadow:
                                    isLatest && !isEmpty
                                        ? 'inset 0 0 0 1px white'
                                        : 'none',
                            }}
                        >
                            {!isEmpty ? b.price.toLocaleString() : ''}
                        </div>

                        {/* 오른쪽 잔량 */}
                        <div style={styles.right}>
                            {!isEmpty && (
                                <div
                                    style={{
                                        ...styles.bar,
                                        width: `${widthPercent}%`,
                                    }}
                                />
                            )}
                            <span style={styles.quantityText}>
                                {!isEmpty ? b.quantity : ''}
                            </span>
                        </div>
                    </div>
                )
            })}
        </div>
    )
}

const cellWidth = 120

const styles = {
    container: { display: 'block' as const },

    row: {
        display: 'flex' as const,
        height: '36px',
        backgroundColor: '#2B0B0B',
        color: '#FFF',
        fontSize: '14px',
        boxSizing: 'border-box' as const,
    },

    left: {
        width: `${cellWidth}px`,
        backgroundColor: '#111111',
        boxSizing: 'border-box' as const,
        fontSize: '12px',
        padding: '1px 2px',
        display: 'flex' as const,
        flexDirection: 'column' as const,
        justifyContent: 'flex-start' as const,
    },

    topLeft: {
        height: '14px',
        lineHeight: '14px',
        display: 'flex' as const,
        justifyContent: 'space-between' as const,
        fontSize: '10px',
    },

    bottomLeft: {
        height: '14px',
        lineHeight: '14px',
        display: 'flex' as const,
        justifyContent: 'space-between' as const,
        fontSize: '10px',
    },

    price: {
        width: `${cellWidth}px`,
        height: '36px',
        lineHeight: '33px',
        textAlign: 'center' as const,
        borderLeft: '1px solid #333',
        borderRight: '1px solid #333',
        borderTop: '0.1px solid #000',
        borderBottom: '0.1px solid #000',
        boxSizing: 'border-box' as const,
    },

    right: {
        position: 'relative' as const,
        width: `${cellWidth}px`,
        height: '36px',
        lineHeight: '27px',
        paddingLeft: '4px',
        backgroundColor: '#2B0B0B',
        boxSizing: 'border-box' as const,
        borderTop: '0.1px solid #000',
        borderBottom: '0.1px solid #000',
    },

    bar: {
        position: 'absolute' as const,
        left: 0,
        top: 0,
        bottom: 0,
        backgroundColor: '#5C1B1B',
        zIndex: 0,
        height: '100%',
    },

    quantityText: {
        position: 'relative' as const,
        zIndex: 1,
        marginLeft: '4px',
        verticalAlign: 'middle' as const,
    },
}
