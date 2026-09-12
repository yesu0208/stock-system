import Modal from '../../components/Modal'
import { useMarketData } from '../context/MarketDataContext'
import type { MarketIndexInfo } from '../../types/marketMain'

interface Props {
    show: boolean
    onClose: () => void
}

function IndexRow({ index }: { index: MarketIndexInfo }) {
    const up = index.direction === '상승'
    const down = index.direction === '하락'
    const color = up ? '#FF6347' : down ? '#4F9DFF' : '#FFF'

    return (
        <div style={styles.indexBlock}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                <span style={{ fontSize: '14px', fontWeight: 600 }}>{index.name}</span>
                <span style={{ fontSize: '16px', fontWeight: 700, color }}>{index.currentIndex}</span>
            </div>
            <div style={{ fontSize: '12px', color, textAlign: 'right' }}>
                {index.changeValue} ({index.changeRate})
            </div>
            <div style={styles.subGrid}>
                <span>상한 {index.breadth.upperLimit}</span>
                <span>상승 {index.breadth.rise}</span>
                <span>보합 {index.breadth.steady}</span>
                <span>하락 {index.breadth.fall}</span>
                <span>하한 {index.breadth.lowerLimit}</span>
            </div>
            <div style={styles.subGrid}>
                <span>개인 {index.investorTrend.personal}</span>
                <span>외국인 {index.investorTrend.foreigner}</span>
                <span>기관 {index.investorTrend.institution}</span>
            </div>
        </div>
    )
}

export default function IndexModal({ show, onClose }: Props) {
    const { marketMain } = useMarketData()

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '340px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>시장 지수</h3>
                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {!marketMain ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                            시황 수신 대기중...
                        </div>
                    ) : (
                        <>
                            <IndexRow index={marketMain.kospi} />
                            <IndexRow index={marketMain.kosdaq} />
                            <IndexRow index={marketMain.kospi200} />
                        </>
                    )}
                </div>
                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    indexBlock: { borderBottom: '1px solid #262626', padding: '10px 4px' },
    subGrid: { display: 'flex', gap: '8px', fontSize: '11px', color: '#888', marginTop: '4px', flexWrap: 'wrap' as const },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
