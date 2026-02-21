import AskList from './AskList'
import BidList from './BidList'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'

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
}

export default function OrderBook({
                                      stockName,
                                      asks,
                                      bids,
                                      tradeTicks = [],
                                      prevClosePrice = 0,
                                      isReady,
                                  }: OrderBookProps) {

    const totalAsk = asks.reduce((sum, a) => sum + a.quantity, 0)
    const totalBid = bids.reduce((sum, b) => sum + b.quantity, 0)

    const latestPrice = tradeTicks[tradeTicks.length - 1]?.curPrice ?? 0
    const change = latestPrice - prevClosePrice
    const changePercent = prevClosePrice ? (change / prevClosePrice) * 100 : 0

    const changeColor =
        change > 0 ? '#FF6347' : change < 0 ? '#4F9DFF' : '#FFF'

    return (
        <div style={styles.wrapper}>
            {!isReady ? (
                <div style={styles.loading}>
                    호가창 생성중...
                </div>
            ) : (
                <div style={styles.container}>
                    <div style={styles.header}>
                        <h2 style={styles.stockName}>{stockName}</h2>
                        <div style={styles.priceInfo}>
                            <span style={styles.latestPrice}>
                                {latestPrice.toLocaleString()}
                            </span>
                            <span style={{ ...styles.change, color: changeColor }}>
                                {change > 0 ? '+' : ''}
                                {change.toLocaleString()} ({changePercent.toFixed(2)}%)
                            </span>
                        </div>
                    </div>

                    <AskList
                        asks={asks}
                        tradeTicks={tradeTicks}
                        prevClosePrice={prevClosePrice}
                    />

                    <BidList
                        bids={bids}
                        tradeTicks={tradeTicks}
                        prevClosePrice={prevClosePrice}
                    />

                    <div style={styles.footer}>
                        <span>{totalAsk}</span>
                        <span>총 잔량</span>
                        <span>{totalBid}</span>
                    </div>
                </div>
            )}
        </div>
    )
}

const styles = {
    wrapper: {
        padding: '16px',
        backgroundColor: '#1A1A1A',
        borderRadius: '8px',
        width: '350px',
        height: '870px',
        minHeight: '870px',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
    },
    container: {
        width: '300px',
        backgroundColor: '#121212',
        borderRadius: '8px',
        padding: '8px',
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
    },
    header: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        marginBottom: '8px',
    },
    stockName: {
        color: '#FFF',
        margin: 0,
    },
    priceInfo: {
        display: 'flex',
        gap: '8px',
        fontSize: '16px',
        marginTop: '4px',
    },
    latestPrice: {
        fontWeight: 'bold',
        color: '#FFF',
    },
    change: {
        fontWeight: 'bold',
    },
    footer: {
        display: 'flex',
        justifyContent: 'space-between',
        color: '#FFF',
        marginTop: '4px',
    },
    loading: {
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#888',
        fontSize: '14px',
    },
} as const
