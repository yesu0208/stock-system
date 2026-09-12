import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { getUpjongs } from '../../api/marketInfo'
import type { UpjongInfo } from '../../types/marketWidgets'

interface Props {
    show: boolean
    onClose: () => void
}

export default function SectorModal({ show, onClose }: Props) {
    const [upjongs, setUpjongs] = useState<UpjongInfo[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!show) return
        setLoading(true)
        getUpjongs().then(res => setUpjongs(res.items)).catch(() => setUpjongs([])).finally(() => setLoading(false))
    }, [show])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '360px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>업종별 시세</h3>
                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {loading ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                    ) : (
                        upjongs.map(u => (
                            <div key={u.no} style={styles.row}>
                                <span style={{ flex: 1 }}>{u.name}</span>
                                <span style={{ color: u.changeRate.startsWith('-') ? '#4F9DFF' : '#FF6347' }}>{u.changeRate}</span>
                                <span style={{ fontSize: '11px', color: '#888', marginLeft: '8px' }}>
                                    상승 {u.rise} 보합 {u.steady} 하락 {u.fall}
                                </span>
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
    row: { display: 'flex', alignItems: 'center', fontSize: '12px', padding: '8px 4px', borderBottom: '1px solid #262626', flexWrap: 'wrap' as const, gap: '4px' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
