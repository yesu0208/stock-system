import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'

interface PriceLevel {
    price: number
    quantity: number
}

interface AskListProps {
    asks: PriceLevel[]
    tradeTicks?: TradePriceTickMessage[]
    prevClosePrice?: number
}

export default function AskList({
                                    asks,
                                    tradeTicks = [],
                                    prevClosePrice = 0,
                                }: AskListProps) {

    const maxQty = Math.max(
        1,
        ...asks.map(a => Number(a.quantity) || 0)
    )

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

    const latestTick =
        tradeTicks.length > 0
            ? tradeTicks[tradeTicks.length - 1]
            : null

    const latestPrice = latestTick?.curPrice
    const lowPrice = latestTick?.lowPrice

    const rate =
        lowPrice && prevClosePrice
            ? ((lowPrice - prevClosePrice) / prevClosePrice) * 100
            : 0

    return (
        <div style={styles.container}>
            {asks.slice().reverse().map((a, idx, arr) => {

                const isEmpty = a.price === 0 || a.quantity === 0

                const widthPercent =
                    maxQty > 0 ? (a.quantity / maxQty) * 100 : 0

                const isLatest =
                    latestPrice !== undefined && a.price === latestPrice

                const isBottomRow = idx === arr.length - 1
                const isAboveBottom = idx === arr.length - 2
                const isTwoAboveBottom = idx === arr.length - 3
                const isThreeAboveBottom = idx === arr.length - 4
                const isFourAboveBottom = idx === arr.length - 5
                const isFiveAboveBottom = idx === arr.length - 6

                const highRate =
                    latestTick?.highPrice && prevClosePrice
                        ? ((latestTick.highPrice - prevClosePrice) / prevClosePrice) * 100
                        : 0

                const startRate =
                    latestTick?.startPrice && prevClosePrice
                        ? ((latestTick.startPrice - prevClosePrice) / prevClosePrice) * 100
                        : 0

                const tradeRate =
                    latestTick?.totalTradingVolume &&
                    latestTick?.prevDaySameTimeAccVolume
                        ? (latestTick.totalTradingVolume /
                        latestTick.prevDaySameTimeAccVolume) * 100
                        : 0

                return (
                    <div key={idx} style={styles.row}>
                        {/* 왼쪽 잔량 */}
                        <div style={styles.left}>
                            {!isEmpty && (
                                <>
                                    <div
                                        style={{
                                            ...styles.bar,
                                            width: `${widthPercent}%`,
                                        }}
                                    />
                                    <span style={styles.quantityText}>
                                        {a.quantity}
                                    </span>
                                </>
                            )}
                        </div>

                        {/* 중앙 가격 */}
                        <div
                            style={{
                                ...styles.price,
                                color: !isEmpty ? getPriceColor(a.price) : 'white',
                                boxShadow:
                                    isLatest && !isEmpty
                                        ? '0 0 0 1px white'
                                        : 'none',
                            }}
                        >
                            {!isEmpty ? a.price.toLocaleString() : ''}
                        </div>

                        {/* 오른쪽 정보 영역 */}
                        <div style={styles.right}>

                            {isFiveAboveBottom && (
                                <>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>거래량</span>
                                        <span style={styles.smallRowRight}>
                                            {latestTick?.totalTradingVolume?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>거래액</span>
                                        <span style={styles.smallRowRight}>
                                            {latestTick?.totalTradingValue != null
                                                ? (latestTick.totalTradingValue / 100000000).toFixed(1) + '억'
                                                : '-'}
                                        </span>
                                    </div>
                                </>
                            )}

                            {isFourAboveBottom && (
                                <>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>매수</span>
                                        <span style={{ ...styles.smallRowRight, color: '#FF6347' }}>
                                            {latestTick?.totalBuyVolume?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>매도</span>
                                        <span style={{ ...styles.smallRowRight, color: '#4F9DFF' }}>
                                            {latestTick?.totalSellVolume?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                </>
                            )}

                            {isThreeAboveBottom && (
                                <>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>전일동시</span>
                                        <span style={styles.smallRowRight}>
                                            {tradeRate.toFixed(2)}%
                                        </span>
                                    </div>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>기준가</span>
                                        <span style={styles.smallRowRight}>
                                            {latestTick?.prevClosePrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                </>
                            )}

                            {isTwoAboveBottom && (
                                <>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>당일시가</span>
                                        <span style={styles.smallRowRight}>
                                            {latestTick?.startPrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}></span>
                                        <span style={{ ...styles.smallRowRight, color: getRateColor(startRate) }}>
                                            {startRate.toFixed(2)}%
                                        </span>
                                    </div>
                                </>
                            )}

                            {isAboveBottom && (
                                <>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>당일고가</span>
                                        <span style={styles.smallRowRight}>
                                            {latestTick?.highPrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}></span>
                                        <span style={{ ...styles.smallRowRight, color: getRateColor(highRate) }}>
                                            {highRate.toFixed(2)}%
                                        </span>
                                    </div>
                                </>
                            )}

                            {isBottomRow && (
                                <>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}>당일저가</span>
                                        <span style={styles.smallRowRight}>
                                            {lowPrice?.toLocaleString() ?? '-'}
                                        </span>
                                    </div>
                                    <div style={styles.smallRow}>
                                        <span style={styles.smallRowLeft}></span>
                                        <span style={{ ...styles.smallRowRight, color: getRateColor(rate) }}>
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

const cellWidth = 120

const styles = {
    container: {
        display: 'block' as const,
    },

    row: {
        display: 'flex' as const,
        height: '36px',
        backgroundColor: '#0B1A2B',
        color: '#FFF',
        fontSize: '14px',
        boxSizing: 'border-box' as const,
    },

    left: {
        position: 'relative' as const,
        width: `${cellWidth}px`,
        backgroundColor: '#0B1A2B',
        boxSizing: 'border-box' as const,
        paddingRight: '4px',
        display: 'flex',
        justifyContent: 'flex-end',
        alignItems: 'center',
        borderTop: '0.1px solid #000',
        borderBottom: '0.1px solid #000',
    },

    price: {
        width: `${cellWidth}px`,
        height: '36px',
        lineHeight: '36px',
        textAlign: 'center' as const,
        borderLeft: '1px solid #333',
        borderRight: '1px solid #333',
        borderTop: '0.1px solid #000',
        borderBottom: '0.1px solid #000',
        boxSizing: 'border-box' as const,
        position: 'relative' as const,
        zIndex: 5,
    },

    right: {
        width: `${cellWidth}px`,
        padding: '2px 4px',
        backgroundColor: '#111111',
        boxSizing: 'border-box' as const,
        borderTop: '0.1px solid #000',
        borderBottom: '0.1px solid #000',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'flex-start',
    },

    smallRow: {
        height: '14px',
        lineHeight: '14px',
        fontSize: '10px',
        display: 'flex' as const,
        justifyContent: 'flex-end',
        alignItems: 'center',
        gap: '4px',
    },

    smallRowLeft: {
        flex: 1,
        textAlign: 'left' as const,
    },

    smallRowRight: {
        flexShrink: 0,
        textAlign: 'right' as const,
    },

    bar: {
        position: 'absolute' as const,
        right: 0,
        top: 0,
        bottom: 0,
        backgroundColor: '#2E3B50',
        zIndex: 0,
        height: '100%',
    },

    quantityText: {
        position: 'relative' as const,
        zIndex: 1,
        marginRight: '4px',
    },
} as const
