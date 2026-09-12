import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { getInvestorTrend } from '../../api/marketInfo'
import type { InvestorTrendDto } from '../../types/marketWidgets'

interface Props {
    show: boolean
    onClose: () => void
}

export default function InvestorModal({ show, onClose }: Props) {
    const [market, setMarket] = useState<'KOSPI' | 'KOSDAQ'>('KOSPI')
    const [trendType, setTrendType] = useState<'time' | 'day'>('time')
    const [data, setData] = useState<InvestorTrendDto[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!show) return
        setLoading(true)
        getInvestorTrend(market, trendType, 1)
            .then(res => setData(res.data))
            .catch(() => setData([]))
            .finally(() => setLoading(false))
    }, [show, market, trendType])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '380px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>투자자별 매매동향</h3>

                <div style={{ display: 'flex', gap: '8px', justifyContent: 'center', marginBottom: '12px' }}>
                    {(['KOSPI', 'KOSDAQ'] as const).map(m => (
                        <button key={m} onClick={() => setMarket(m)} style={{ ...styles.toggle, ...(market === m ? styles.toggleActive : {}) }}>
                            {m}
                        </button>
                    ))}
                    {(['time', 'day'] as const).map(t => (
                        <button key={t} onClick={() => setTrendType(t)} style={{ ...styles.toggle, ...(trendType === t ? styles.toggleActive : {}) }}>
                            {t === 'time' ? '시간별' : '일별'}
                        </button>
                    ))}
                </div>

                <div style={{ flex: 1, overflowY: 'auto' }}>
                    <div style={styles.header}>
                        <span>시간/날짜</span><span>개인</span><span>외국인</span><span>기관</span>
                    </div>
                    {loading ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                    ) : (
                        data.map((d, i) => (
                            <div key={i} style={styles.row}>
                                <span>{d.dateOrTime}</span>
                                <span style={{ color: d.individual >= 0 ? '#FF6347' : '#4F9DFF' }}>{d.individual.toLocaleString()}</span>
                                <span style={{ color: d.foreigner >= 0 ? '#FF6347' : '#4F9DFF' }}>{d.foreigner.toLocaleString()}</span>
                                <span style={{ color: d.institution >= 0 ? '#FF6347' : '#4F9DFF' }}>{d.institution.toLocaleString()}</span>
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
    toggle: { padding: '4px 10px', fontSize: '12px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    toggleActive: { backgroundColor: '#4F9DFF', borderColor: '#4F9DFF' },
    header: { display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr 1fr', fontSize: '11px', color: '#888', padding: '4px', borderBottom: '1px solid #333' },
    row: { display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr 1fr', fontSize: '12px', padding: '6px 4px', borderBottom: '1px solid #262626' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
