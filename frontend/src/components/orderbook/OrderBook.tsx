import { useRealtime } from '../../context/RealtimeContext'
import AskList from './AskList'
import BidList from './BidList'
import styles from './OrderBook.module.css'

export default function OrderBook() {
    const { orderbook, connected } = useRealtime()

    const asks = orderbook?.asks ?? []
    const bids = orderbook?.bids ?? []

    const maxQuantity = Math.max(
        0,
        ...asks.map((a) => a.quantity),
        ...bids.map((b) => b.quantity)
    )

    const isEmpty = asks.length === 0 && bids.length === 0

    return (
        <div className={styles.wrapper}>
            {!connected || isEmpty ? (
                <div className={styles.loading}>불러오는 중입니다</div>
            ) : (
                <div className={styles.book}>
                    <AskList asks={asks} maxQuantity={maxQuantity} />
                    <div className={styles.spread} />
                    <BidList bids={bids} maxQuantity={maxQuantity} />
                </div>
            )}
        </div>
    )
}
