import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { getOrderHistory, getOrderCancelHistory, getUnfilledOrders, getTradeHistory } from '../../api/orderHistory'
import { stockNameMap } from '../../constants/stocks'
import type { OrderHistoryItem, TradeHistoryItem } from '../../types/history'

/**
 * OrderHistoryModal
 * 백엔드 OrderHistoryController.java(페이지네이션 + 기간 필터 지원)를
 * 확인한 결과 완전히 별도의 REST 조회 기능이라, 기존 인라인 목록은
 * 건드리지 않고 새 모달로 분리
 */

type TabType = 'TRADES' | 'ORDERS' | 'UNFILLED' | 'CANCELS'

const ORDER_STATUS_LABEL: Record<string, string> = {
    OPEN: '대기',
    PARTIAL: '부분체결',
    FILLED: '체결완료',
    CANCELED: '취소됨',
}

interface Props {
    show: boolean
    onClose: () => void
}

export default function OrderHistoryModal({ show, onClose }: Props) {
    const [tab, setTab] = useState<TabType>('TRADES')
    const [page, setPage] = useState(0)
    const [orders, setOrders] = useState<OrderHistoryItem[]>([])
    const [trades, setTrades] = useState<TradeHistoryItem[]>([])
    const [hasNext, setHasNext] = useState(false)
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!show) return
        setPage(0)
        setOrders([])
        setTrades([])
    }, [show, tab])

    useEffect(() => {
        if (!show) return

        setLoading(true)
        const fetcher =
            tab === 'TRADES' ? getTradeHistory
                : tab === 'ORDERS' ? getOrderHistory
                    : tab === 'UNFILLED' ? getUnfilledOrders
                        : getOrderCancelHistory

        fetcher({ page, size: 20 })
            .then(res => {
                if (tab === 'TRADES') {
                    setTrades(prev => (page === 0 ? res.items as TradeHistoryItem[] : [...prev, ...(res.items as TradeHistoryItem[])]))
                } else {
                    setOrders(prev => (page === 0 ? res.items as OrderHistoryItem[] : [...prev, ...(res.items as OrderHistoryItem[])]))
                }
                setHasNext(res.hasNext)
            })
            .catch(() => setHasNext(false))
            .finally(() => setLoading(false))
    }, [show, tab, page])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '480px', maxHeight: '70vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>주문/체결 내역</h3>

                <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', marginBottom: '12px' }}>
                    {(['TRADES', 'ORDERS', 'UNFILLED', 'CANCELS'] as const).map(t => (
                        <button
                            key={t}
                            onClick={() => setTab(t)}
                            style={{ ...styles.tabButton, ...(tab === t ? styles.tabActive : {}) }}
                        >
                            {t === 'TRADES' ? '체결' : t === 'ORDERS' ? '전체주문' : t === 'UNFILLED' ? '미체결' : '취소'}
                        </button>
                    ))}
                </div>

                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {tab === 'TRADES'
                        ? trades.map(t => (
                            <div key={t.tradeId} style={styles.row}>
                                <span style={{ color: t.tradeType === 'BUY' ? '#FF6347' : '#4F9DFF' }}>
                                    {t.tradeType === 'BUY' ? '매수' : '매도'}
                                </span>
                                <span>{stockNameMap[t.stockCode] ?? t.stockCode}</span>
                                <span>{t.tradePrice.toLocaleString()}원 · {t.tradeQuantity}주</span>
                                <span style={styles.time}>{new Date(t.executedAt).toLocaleString('ko-KR')}</span>
                            </div>
                        ))
                        : orders.map(o => (
                            <div key={o.orderId} style={styles.row}>
                                <span style={{ color: o.orderType === 'BUY' ? '#FF6347' : '#4F9DFF' }}>
                                    {o.orderType === 'BUY' ? '매수' : '매도'}
                                </span>
                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                <span>
                                    {o.orderPrice.toLocaleString()}원 · {o.orderQuantity - o.remainingQuantity}/{o.orderQuantity}주
                                </span>
                                <span style={{ color: '#AAA' }}>{ORDER_STATUS_LABEL[o.orderStatus] ?? o.orderStatus}</span>
                                <span style={styles.time}>{new Date(o.orderTime).toLocaleString('ko-KR')}</span>
                            </div>
                        ))}

                    {!loading && ((tab === 'TRADES' && trades.length === 0) || (tab !== 'TRADES' && orders.length === 0)) && (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>내역 없음</div>
                    )}
                </div>

                {hasNext && (
                    <button onClick={() => setPage(p => p + 1)} style={styles.moreButton} disabled={loading}>
                        더 보기
                    </button>
                )}

                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    tabButton: { padding: '6px 10px', fontSize: '12px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    tabActive: { backgroundColor: '#4F9DFF', borderColor: '#4F9DFF' },
    row: { display: 'flex', gap: '8px', alignItems: 'center', fontSize: '12px', padding: '8px 4px', borderBottom: '1px solid #262626', flexWrap: 'wrap' as const },
    time: { marginLeft: 'auto', color: '#666', fontSize: '11px' },
    moreButton: { padding: '8px', fontSize: '13px', backgroundColor: '#222', color: '#FFF', border: '1px solid #333', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
