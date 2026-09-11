import { useState } from 'react'
import Modal from '../../components/Modal'
import { createAlert, cancelAlert } from '../../api/alert'
import { useAlert } from '../context/AlertContext'
import { useMsg } from '../context/MsgContext'
import { stockNameMap } from '../../constants/stocks'
import type { AlertDirection } from '../../types/alert'

interface Props {
    show: boolean
    onClose: () => void
    stockCode: string
    stockName: string
    curPrice?: number
}

export default function AlertModal({ show, onClose, stockCode, stockName, curPrice }: Props) {
    const { alerts } = useAlert()
    const { success, error } = useMsg()

    const [direction, setDirection] = useState<AlertDirection>('ABOVE')
    const [triggerPrice, setTriggerPrice] = useState(curPrice ?? 0)
    const [submitting, setSubmitting] = useState(false)

    if (!show) return null

    const myAlerts = alerts.filter(a => a.stockCode === stockCode)

    const handleCreate = async () => {
        setSubmitting(true)
        try {
            await createAlert({ stockCode, direction, triggerPrice })
            success('알림이 등록되었습니다.')
        } catch (e: any) {
            error(`알림 등록 실패: ${e.response?.data?.message ?? e.message}`)
        } finally {
            setSubmitting(false)
        }
    }

    const handleCancel = async (alertId: number) => {
        try {
            await cancelAlert(alertId, stockCode)
            success('알림이 해지되었습니다.')
        } catch (e: any) {
            error(`알림 해지 실패: ${e.response?.data?.message ?? e.message}`)
        }
    }

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '320px' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>{stockName} 목표가 알림</h3>

                <div style={{ display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center', marginBottom: '12px' }}>
                    {(['ABOVE', 'BELOW'] as const).map(d => (
                        <button
                            key={d}
                            onClick={() => setDirection(d)}
                            style={{ ...styles.toggleButton, ...(direction === d ? styles.toggleActive : {}) }}
                        >
                            {d === 'ABOVE' ? '이상' : '이하'}
                        </button>
                    ))}
                    <input
                        type="number"
                        value={triggerPrice}
                        onChange={e => setTriggerPrice(Number(e.target.value))}
                        style={styles.input}
                        className="no-spinner"
                    />
                    <span style={{ fontSize: '12px', color: '#AAA' }}>원</span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
                    <button onClick={handleCreate} style={styles.submitButton} disabled={submitting}>
                        알림 등록
                    </button>
                </div>

                <div style={{ borderTop: '1px solid #333', paddingTop: '8px' }}>
                    <h4 style={{ color: '#AAA', margin: '0 0 6px 0', fontSize: '13px' }}>등록된 알림</h4>
                    {myAlerts.length === 0 ? (
                        <div style={{ color: '#666', fontSize: '12px', textAlign: 'center' }}>등록된 알림 없음</div>
                    ) : (
                        myAlerts.map(a => (
                            <div key={a.alertId} style={styles.alertRow}>
                                <span>
                                    {stockNameMap[a.stockCode] ?? a.stockCode} {a.triggerPrice.toLocaleString()}원 {a.direction === 'ABOVE' ? '이상' : '이하'}
                                </span>
                                <button onClick={() => handleCancel(a.alertId)} style={styles.cancelSmallButton}>
                                    해지
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
    toggleButton: { padding: '4px 8px', fontSize: '12px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    toggleActive: { backgroundColor: '#4F9DFF', borderColor: '#4F9DFF' },
    input: { padding: '6px', fontSize: '13px', borderRadius: '4px', border: '1px solid #333', backgroundColor: '#222', color: '#FFF', width: '90px', textAlign: 'center' as const },
    submitButton: { padding: '8px 16px', fontSize: '14px', borderRadius: '6px', border: 'none', backgroundColor: '#4F9DFF', color: '#FFF', cursor: 'pointer', fontWeight: 600 },
    alertRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px', padding: '6px 0', borderBottom: '1px solid #262626' },
    cancelSmallButton: { padding: '2px 8px', fontSize: '11px', backgroundColor: '#552222', color: '#FF8A80', border: '1px solid #773333', borderRadius: '4px', cursor: 'pointer' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '12px', width: '100%' },
} as const
