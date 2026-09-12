import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { getPopularStocks } from '../../api/marketInfo'
import type { PopularStock } from '../../types/marketWidgets'

interface Props {
    show: boolean
    onClose: () => void
    onSelectStock?: (code: string) => void
}

export default function HotStocksModal({ show, onClose, onSelectStock }: Props) {
    const [stocks, setStocks] = useState<PopularStock[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!show) return
        setLoading(true)
        getPopularStocks().then(setStocks).catch(() => setStocks([])).finally(() => setLoading(false))
    }, [show])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '320px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>인기 종목</h3>
                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {loading ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                    ) : (
                        stocks.map(s => (
                            <button
                                key={s.code}
                                onClick={() => onSelectStock?.(s.code)}
                                style={styles.row}
                                disabled={!onSelectStock}
                            >
                                <span style={{ width: '24px', color: '#888' }}>{s.rank}</span>
                                <span style={{ flex: 1, textAlign: 'left' }}>{s.name}</span>
                                <span style={{ color: s.direction === '상승' ? '#FF6347' : s.direction === '하락' ? '#4F9DFF' : '#FFF' }}>
                                    {s.price}
                                </span>
                            </button>
                        ))
                    )}
                </div>
                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    row: { display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '8px 4px', borderBottom: '1px solid #262626', background: 'none', border: 'none', color: '#FFF', fontSize: '13px', cursor: 'pointer' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
