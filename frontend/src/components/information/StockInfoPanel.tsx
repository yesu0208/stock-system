import { useEffect, useState } from 'react'
import { useRealtime } from '../../main/context/RealtimeContext'
import { calcStockStats, isRealtimeStock } from '../../utils/stockUtils'
import type { TradePriceTickMessage } from '../../types/tradePriceTickMessage'
import AlertModal from '../../main/components/AlertModal'
import WatchListModal from '../../main/components/WatchListModal'
import DiscussionModal from '../../main/components/DiscussionModal'
import StockTalkModal from '../../main/components/StockTalkModal'
import NewsModal from '../../main/components/NewsModal'
import { useWatchList } from '../../main/context/WatchListContext'
import styles from './StockInfoPanel.module.css'

interface Props {
    stockCode: string
    stockName: string
}

export default function StockInfoPanel({ stockCode, stockName }: Props) {
    const { subscribeStock } = useRealtime()
    const [priceTick, setPriceTick] = useState<TradePriceTickMessage | null>(null)
    const [showAlertModal, setShowAlertModal] = useState(false)
    const [showWatchListModal, setShowWatchListModal] = useState(false)
    const [showDiscussionModal, setShowDiscussionModal] = useState(false)
    const [showStockTalkModal, setShowStockTalkModal] = useState(false)
    const [showNewsModal, setShowNewsModal] = useState(false)

    const { isWatched, addStock, removeStock } = useWatchList()
    const watched = isWatched(stockCode)

    const handleToggleWatch = () => {
        if (watched) removeStock(stockCode)
        else addStock(stockCode, stockName)
    }

    useEffect(() => {
        setPriceTick(null)
        return subscribeStock(stockCode, (tick: any) => {
            if (tick.tickMessageType === 'TRADEPRICE') {
                setPriceTick(tick as TradePriceTickMessage)
            }
        })
    }, [stockCode, subscribeStock])

    if (!isRealtimeStock(stockCode)) {
        return (
            <div className={styles.container}>
                <div className={styles.titleRow}>
                    <span className={styles.title}>{stockName}</span>
                    <div className={styles.buttonGroup}>
                        <button onClick={handleToggleWatch} className={styles.alertButton}>
                            {watched ? '★ 관심' : '☆ 관심'}
                        </button>
                        <button onClick={() => setShowWatchListModal(true)} className={styles.alertButton}>목록</button>
                        <button onClick={() => setShowAlertModal(true)} className={styles.alertButton}>🔔 알림</button>
                        <button onClick={() => setShowDiscussionModal(true)} className={styles.alertButton}>💬 토론</button>
                        <button onClick={() => setShowStockTalkModal(true)} className={styles.alertButton}>💭 종목톡</button>
                        <button onClick={() => setShowNewsModal(true)} className={styles.alertButton}>📰 뉴스</button>
                    </div>
                </div>
                <div className={styles.notice}>실시간 미지원 종목입니다.</div>
                <AlertModal
                    show={showAlertModal}
                    onClose={() => setShowAlertModal(false)}
                    stockCode={stockCode}
                    stockName={stockName}
                />
                <WatchListModal
                    show={showWatchListModal}
                    onClose={() => setShowWatchListModal(false)}
                />
                <DiscussionModal
                    show={showDiscussionModal}
                    onClose={() => setShowDiscussionModal(false)}
                    stockCode={stockCode}
                    stockName={stockName}
                />
                <StockTalkModal
                    show={showStockTalkModal}
                    onClose={() => setShowStockTalkModal(false)}
                    stockCode={stockCode}
                    stockName={stockName}
                />
                <NewsModal
                    show={showNewsModal}
                    onClose={() => setShowNewsModal(false)}
                    keyword={stockName}
                />
            </div>
        )
    }

    const stats = priceTick ? calcStockStats(true, priceTick, null) : null

    return (
        <div className={styles.container}>
            <div className={styles.titleRow}>
                <span className={styles.title}>{stockName}</span>
                <div className={styles.buttonGroup}>
                    <button onClick={handleToggleWatch} className={styles.alertButton}>
                        {watched ? '★ 관심' : '☆ 관심'}
                    </button>
                    <button onClick={() => setShowWatchListModal(true)} className={styles.alertButton}>목록</button>
                    <button onClick={() => setShowAlertModal(true)} className={styles.alertButton}>🔔 알림</button>
                    <button onClick={() => setShowDiscussionModal(true)} className={styles.alertButton}>💬 토론</button>
                    <button onClick={() => setShowStockTalkModal(true)} className={styles.alertButton}>💭 종목톡</button>
                    <button onClick={() => setShowNewsModal(true)} className={styles.alertButton}>📰 뉴스</button>
                </div>
            </div>
            {!stats ? (
                <div className={styles.notice}>시세 수신 대기중...</div>
            ) : (
                <div className={styles.grid}>
                    <Row label="시가" value={stats.open.price} />
                    <Row label="고가" value={stats.high.price} />
                    <Row label="저가" value={stats.low.price} />
                    <Row label="거래량" value={stats.volume} />
                    <Row label="거래대금(백만)" value={stats.tradingValue} />
                </div>
            )}
            <AlertModal
                show={showAlertModal}
                onClose={() => setShowAlertModal(false)}
                stockCode={stockCode}
                stockName={stockName}
                curPrice={priceTick?.curPrice}
            />
            <WatchListModal
                show={showWatchListModal}
                onClose={() => setShowWatchListModal(false)}
            />
            <DiscussionModal
                show={showDiscussionModal}
                onClose={() => setShowDiscussionModal(false)}
                stockCode={stockCode}
                stockName={stockName}
            />
            <StockTalkModal
                show={showStockTalkModal}
                onClose={() => setShowStockTalkModal(false)}
                stockCode={stockCode}
                stockName={stockName}
            />
            <NewsModal
                show={showNewsModal}
                onClose={() => setShowNewsModal(false)}
                keyword={stockName}
            />
        </div>
    )
}

function Row({ label, value }: { label: string; value: string }) {
    return (
        <div className={styles.row}>
            <span className={styles.label}>{label}</span>
            <span className={styles.value}>{value}</span>
        </div>
    )
}
