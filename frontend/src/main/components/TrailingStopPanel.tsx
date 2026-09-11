import { useEffect, useRef, useState } from 'react'
import Modal from '../../components/Modal'
import { placeTrailingStop, cancelTrailingStop } from '../../api/trailingStop'
import { stockNameMap } from '../../constants/stocks'
import type {
    TrailingStopType,
    TrailingStopResponseMessage,
    TrailingStopResultResponse,
    TrailingStopCancelResultResponse,
} from '../../types/trailingStop'

/**
 * TrailingStopPanel
 *
 * TrailingStopResponseMessage에는 상태(status) 필드가 없어서,
 * OTOCO처럼 "취소 가능한 상태만" 걸러내지 못하고 목록에 있는 항목은 전부 취소 가능하다고 가정
 * (이미 체결/취소된 건은 findAll에서 안 내려온다는 전제)
 * 취소 성공 시엔 서버가 목록을 다시 push해줘서 자동 갱신되므로 실패했을 때만 결과 모달로 안내
 */

interface Props {
    stockCode: string
    stockName: string
    curPrice?: number
    leverageRatio: 'SPOT' | 'X1_5' | 'X2' | 'X2_5'
    trailingStops: TrailingStopResponseMessage[]
    trailingStopResult: TrailingStopResultResponse | null
    trailingStopCancelResult: TrailingStopCancelResultResponse | null
}

export default function TrailingStopPanel({
                                              stockCode,
                                              stockName,
                                              curPrice,
                                              leverageRatio,
                                              trailingStops,
                                              trailingStopResult,
                                              trailingStopCancelResult,
                                          }: Props) {
    const [trailingStopType, setTrailingStopType] = useState<TrailingStopType>('SELL')
    const [orderQuantity, setOrderQuantity] = useState(1)
    const [stopPercent, setStopPercent] = useState(3)
    const [basePrice, setBasePrice] = useState(curPrice ?? 0)

    const [confirmOpen, setConfirmOpen] = useState(false)
    const [resultModal, setResultModal] = useState<{ success: boolean; message: string } | null>(null)
    const [submitting, setSubmitting] = useState(false)
    const [cancelingId, setCancelingId] = useState<number | null>(null)

    const initializedRef = useRef(false)

    useEffect(() => {
        if (!initializedRef.current && curPrice !== undefined) {
            setBasePrice(curPrice)
            initializedRef.current = true
        }
    }, [curPrice])

    useEffect(() => {
        initializedRef.current = false
    }, [stockCode])

    useEffect(() => {
        if (!trailingStopResult) return
        if (trailingStopResult.responseType === 'ERROR') {
            setResultModal({ success: false, message: trailingStopResult.errorMessage ?? '알 수 없는 오류' })
        }
    }, [trailingStopResult])

    useEffect(() => {
        if (!trailingStopCancelResult) return
        if (trailingStopCancelResult.responseType === 'ERROR') {
            setResultModal({ success: false, message: `취소 실패: ${trailingStopCancelResult.errorMessage ?? '알 수 없는 오류'}` })
        }
    }, [trailingStopCancelResult])

    const handleSubmit = async () => {
        setSubmitting(true)
        try {
            await placeTrailingStop({
                stockCode,
                trailingStopType,
                orderQuantity,
                stopPercent,
                basePrice,
                leverageRatio: leverageRatio === 'SPOT' ? null : leverageRatio,
            })
            setResultModal({ success: true, message: '트레일링 스탑 주문이 등록되었습니다.' })
        } catch (e: any) {
            setResultModal({
                success: false,
                message: `❌ 트레일링 스탑 주문 실패: ${e.response?.data?.message ?? e.message}`,
            })
        } finally {
            setSubmitting(false)
            setConfirmOpen(false)
        }
    }

    const handleCancel = async (trailingStopId: number, targetStockCode: string) => {
        setCancelingId(trailingStopId)
        try {
            await cancelTrailingStop({ trailingStopId, stockCode: targetStockCode })
            // 성공하면 '/user/sub/trailing-stop' 전체 목록이 곧 다시 push되어 자동 갱신됨
        } catch (e: any) {
            setResultModal({
                success: false,
                message: `취소 요청 실패: ${e.response?.data?.message ?? e.message}`,
            })
        } finally {
            setCancelingId(null)
        }
    }

    const sortedStops = [...trailingStops].sort(
        (a, b) => new Date(b.orderTime).getTime() - new Date(a.orderTime).getTime()
    )

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' }}>
                <span style={styles.label}>구분</span>
                {(['BUY', 'SELL'] as const).map(t => (
                    <button
                        key={t}
                        onClick={() => setTrailingStopType(t)}
                        style={{ ...styles.toggleButton, ...(trailingStopType === t ? styles.toggleActive : {}) }}
                    >
                        {t === 'BUY' ? '매수' : '매도'}
                    </button>
                ))}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' }}>
                <span style={styles.label}>기준가</span>
                <input
                    type="number"
                    value={basePrice}
                    onChange={e => setBasePrice(Number(e.target.value))}
                    style={styles.input}
                    className="no-spinner"
                />
                <span style={styles.unit}>원</span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' }}>
                <span style={styles.label}>추적 폭</span>
                <input
                    type="number"
                    step={0.1}
                    value={stopPercent}
                    onChange={e => setStopPercent(Number(e.target.value))}
                    style={styles.input}
                    className="no-spinner"
                />
                <span style={styles.unit}>%</span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' }}>
                <span style={styles.label}>수량</span>
                <input
                    type="number"
                    value={orderQuantity}
                    onChange={e => setOrderQuantity(Math.max(1, Number(e.target.value)))}
                    style={styles.input}
                    className="no-spinner"
                />
                <span style={styles.unit}>주</span>
            </div>

            <p style={{ fontSize: '11px', color: '#888', textAlign: 'center', margin: 0 }}>
                {trailingStopType === 'SELL'
                    ? `기준가 이후 고점 대비 ${stopPercent}% 하락 시 매도`
                    : `기준가 이후 저점 대비 ${stopPercent}% 상승 시 매수`}
            </p>

            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '4px' }}>
                <button onClick={() => setConfirmOpen(true)} style={styles.submitButton} disabled={submitting}>
                    트레일링 스탑 등록
                </button>
            </div>

            <div style={{ borderTop: '1px solid #333', margin: '8px 0' }} />

            <h4 style={{ color: '#AAA', margin: '0 0 6px 0' }}>내 트레일링 스탑</h4>
            <div style={{ flex: 1, overflowY: 'auto', maxHeight: '220px' }}>
                {sortedStops.length === 0 ? (
                    <div style={{ color: '#666', fontSize: '13px', textAlign: 'center' }}>등록된 트레일링 스탑 없음</div>
                ) : (
                    sortedStops.map(t => (
                        <div key={t.trailingStopId} style={styles.orderCard}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                                <span>{stockNameMap[t.stockCode] ?? t.stockCode}</span>
                                <span style={{ color: t.trailingStopType === 'BUY' ? '#FF6347' : '#4F9DFF' }}>
                                    {t.trailingStopType === 'BUY' ? '매수' : '매도'}
                                </span>
                            </div>
                            <div style={{ fontSize: '12px', color: '#AAA', marginTop: '4px' }}>
                                추적 {t.stopPercent}% · {t.orderQuantity}주
                                {t.leverageRatio && t.leverageRatio !== 'SPOT' && (
                                    <> · {t.leverageRatio.replace('X', '').replace('_', '.')}배</>
                                )}
                            </div>
                            {t.triggerPrice != null && (
                                <div style={{ fontSize: '12px', color: '#888', marginTop: '2px' }}>
                                    현재 감시가: {t.triggerPrice.toLocaleString()}원
                                </div>
                            )}
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '6px' }}>
                                <button
                                    onClick={() => handleCancel(t.trailingStopId, t.stockCode)}
                                    disabled={cancelingId === t.trailingStopId}
                                    style={styles.cancelButton}
                                >
                                    {cancelingId === t.trailingStopId ? '취소중...' : '취소'}
                                </button>
                            </div>
                        </div>
                    ))
                )}
            </div>

            {confirmOpen && (
                <Modal show={true} onClose={() => setConfirmOpen(false)}>
                    <div style={{ textAlign: 'center' }}>
                        <h3>{stockName} 트레일링 스탑</h3>
                        <p style={{ fontSize: '13px', lineHeight: 1.6, margin: '12px 0' }}>
                            기준가 {basePrice.toLocaleString()}원, 추적 {stopPercent}%,
                            {trailingStopType === 'BUY' ? ' 매수' : ' 매도'} {orderQuantity}주
                            {leverageRatio !== 'SPOT' && <><br />레버리지: {leverageRatio.replace('X', '').replace('_', '.')}배</>}
                        </p>
                        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
                            <button style={styles.submitButton} onClick={handleSubmit} disabled={submitting}>확인</button>
                            <button style={styles.cancelModalButton} onClick={() => setConfirmOpen(false)}>취소</button>
                        </div>
                    </div>
                </Modal>
            )}

            {resultModal && (
                <Modal show={true} onClose={() => setResultModal(null)}>
                    <div style={{ textAlign: 'center' }}>
                        <p>{resultModal.message}</p>
                        <button style={styles.submitButton} onClick={() => setResultModal(null)}>확인</button>
                    </div>
                </Modal>
            )}
        </div>
    )
}

const styles = {
    label: { color: '#AAA', minWidth: '52px', fontSize: '13px', textAlign: 'center' as const },
    unit: { fontSize: '12px', color: '#AAA', marginLeft: '2px' },
    input: { padding: '6px', fontSize: '13px', borderRadius: '4px', border: '1px solid #333', backgroundColor: '#222', color: '#FFF', width: '90px', textAlign: 'center' as const },
    toggleButton: { padding: '4px 8px', fontSize: '12px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    toggleActive: { backgroundColor: '#4F9DFF', borderColor: '#4F9DFF' },
    submitButton: { padding: '8px 16px', fontSize: '14px', borderRadius: '6px', border: 'none', backgroundColor: '#4F9DFF', color: '#FFF', cursor: 'pointer', fontWeight: 600 },
    cancelModalButton: { padding: '8px 16px', fontSize: '14px', borderRadius: '6px', border: 'none', backgroundColor: '#333', color: '#FFF', cursor: 'pointer' },
    orderCard: { backgroundColor: '#222', border: '1px solid #333', borderRadius: '8px', padding: '10px 12px', marginBottom: '8px' },
    cancelButton: { padding: '3px 10px', fontSize: '11px', borderRadius: '4px', border: '1px solid #552222', backgroundColor: '#331414', color: '#FF8A80', cursor: 'pointer' },
} as const
