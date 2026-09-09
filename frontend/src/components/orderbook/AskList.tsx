import type { PriceLevel } from '../../types/realtime'
import styles from './OrderBook.module.css'

interface AskListProps {
    /** 서버가 내려주는 매도 호가 배열. 인덱스 0이 최우선(최저가) 호가. */
    asks: PriceLevel[]
    /** 매도/매수 통틀어 가장 큰 잔량. 잔량 바 길이 계산 기준. */
    maxQuantity: number
}

export default function AskList({ asks, maxQuantity }: AskListProps) {
    // 화면에는 낮은 가격(체결에 가까운 가격)이 아래쪽에 오도록, 매도 호가는 배열을 뒤집어서 높은 가격이 위쪽에 오게 그림.
    const rows = [...asks].reverse()

    return (
        <ul className={styles.askList}>
            {rows.map((level) => {
                const ratio = maxQuantity > 0 ? (level.quantity / maxQuantity) * 100 : 0

                return (
                    <li key={level.price} className={styles.row}>
                        <div className={styles.barTrack}>
                            <div
                                className={styles.askBar}
                                style={{ width: `${ratio}%` }}
                            />
                        </div>
                        <span className={styles.askPrice}>
                            {level.price.toLocaleString()}
                        </span>
                        <span className={styles.quantity}>
                            {level.quantity.toLocaleString()}
                        </span>
                    </li>
                )
            })}
        </ul>
    )
}
