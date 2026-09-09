import { useStock } from '../../context/StockContext'
import { useRealtime } from '../../context/RealtimeContext'
import { calcStockStats, DIRECTION_CLASS } from '../../utils/stockUtils'
import './TopBar.css'

export default function TopBar() {
    const { selectedStock } = useStock()
    const { priceTick } = useRealtime()
    const stats = calcStockStats(priceTick)

    return (
        <div className="top-bar">
            <div className="cell stock-cell">
                <div className="stock-info">
                    <span className="name">{selectedStock.name}</span>
                    <span className="meta">{selectedStock.code}</span>
                </div>
            </div>

            <div className="cell price-cell">
                <span className={`cur ${stats ? DIRECTION_CLASS[stats.cur.direction] : ''}`}>
                    {stats?.cur.price ?? '-'}
                </span>
                <span className={`diff ${stats ? DIRECTION_CLASS[stats.cur.direction] : ''}`}>
                    {stats ? `${stats.cur.diffText} (${stats.cur.rateText})` : '-'}
                </span>
            </div>

            <div className="cell stat-cell">
                <span className="label">시가</span>
                <span className="value">{stats?.open.price ?? '-'}</span>
            </div>
            <div className="cell stat-cell">
                <span className="label">고가</span>
                <span className="value">{stats?.high.price ?? '-'}</span>
            </div>
            <div className="cell stat-cell">
                <span className="label">저가</span>
                <span className="value">{stats?.low.price ?? '-'}</span>
            </div>
            <div className="cell stat-cell">
                <span className="label">거래량</span>
                <span className="value">{stats?.volume ?? '-'}</span>
            </div>
        </div>
    )
}
