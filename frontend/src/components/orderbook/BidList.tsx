import type { PriceLevel } from '../../types/realtime'
import styles from './OrderBook.module.css'

interface BidListProps {
    /** 서버가 내려주는 매수 호가 배열. 인덱스 0이 최우선(최고가) 호가. */
    bids: PriceLevel[]
    /** 매도/매수 통틀어 가장 큰 잔량. 잔량 바 길이 계산 기준. */
    maxQuantity: number
}

export default function BidList({ bids, maxQuantity }: BidListProps) {
    // 매수 호가는 서버가 이미 높은 가격 순으로 내려주므로 그대로 사용
    return (
        <ul className={styles.bidList}>
            {bids.map((level) => {
                const ratio = maxQuantity > 0 ? (level.quantity / maxQuantity) * 100 : 0

                return (
                    <li key={level.price} className={styles.row}>
                        <span className={styles.bidPrice}>
                            {level.price.toLocaleString()}
                        </span>
                        <span className={styles.quantity}>
                            {level.quantity.toLocaleString()}
                        </span>
                        <div className={styles.barTrack}>
                            <div
                                className={styles.bidBar}
                                style={{ width: `${ratio}%` }}
                            />
                        </div>
                    </li>
                )
            })}
        </ul>
    )
}
