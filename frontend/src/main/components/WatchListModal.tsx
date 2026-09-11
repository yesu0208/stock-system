import Modal from '../../components/Modal'
import { useWatchList } from '../context/WatchListContext'

/**
 * WatchListModal
 * 4단계 WatchListContext(REST CRUD, /api/v1/watchlist)를 그대로 사용.
 * 이 모달은 UI만 제공하고, 실제 등록/해지 로직(및 성공/실패 토스트)은
 * WatchListContext 안에 이미 구현되어 있음.
 *
 * onSelectStock은 옵션이며, 이번 단계에서는 호출부(StockInfoPanel)가
 * 아직 selectedStock setter에 접근할 수 없어 연결하지 않음.
 */

interface Props {
    show: boolean
    onClose: () => void
    onSelectStock?: (stockCode: string) => void
}

export default function WatchListModal({ show, onClose, onSelectStock }: Props) {
    const { watchList, loading, removeStock } = useWatchList()

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '320px', maxHeight: '60vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>관심종목</h3>

                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {loading ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                    ) : watchList.length === 0 ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                            등록된 관심종목이 없습니다.
                        </div>
                    ) : (
                        watchList.map(item => (
                            <div key={item.stockCode} style={styles.row}>
                                <button
                                    onClick={() => onSelectStock?.(item.stockCode)}
                                    style={styles.stockButton}
                                    disabled={!onSelectStock}
                                >
                                    {item.stockName} <span style={{ color: '#666' }}>({item.stockCode})</span>
                                </button>
                                <button onClick={() => removeStock(item.stockCode)} style={styles.removeButton}>
                                    삭제
                                </button>
                            </div>
                        ))
                    )}
                </div>

                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    row: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 4px', borderBottom: '1px solid #262626' },
    stockButton: { background: 'none', border: 'none', color: '#FFF', fontSize: '13px', cursor: 'pointer', textAlign: 'left' as const, padding: 0, flex: 1 },
    removeButton: { padding: '2px 8px', fontSize: '11px', backgroundColor: '#552222', color: '#FF8A80', border: '1px solid #773333', borderRadius: '4px', cursor: 'pointer' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '12px', width: '100%' },
} as const
