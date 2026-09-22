import { useEffect, useRef, useState } from 'react'
import Modal from '../../components/Modal'
import { placeOtocoOrder, cancelOtocoOrder } from '../../api/otoco'
import { stockNameMap } from '../data/stocks'
import type {
    OtocoEntryDirection,
    OtocoExitMode,
    OtocoResponseMessage,
    OtocoResultResponse,
    OtocoCancelResultResponse,
} from '../../types/otoco'

/**
 * OtocoPanel
 * TradePanel.tsx의 4번째 탭으로 추가되며, 실시간 구독은 TradePage.tsx에서
 * 받아 props로 내려받음
 *
 * 취소 성공 시에는 서버가 '/user/sub/otoco' 전체 목록도 같이 다시
 * push해주므로 별도 안내 모달 없이 목록만 자동 갱신되고, 실패했을 때만 결과 모달로 안내
 */

interface Props {
    stockCode: string
    stockName: string
    curPrice?: number
    leverageRatio: 'SPOT' | 'X1_5' | 'X2' | 'X2_5'
    otocoOrders: OtocoResponseMessage[]
    otocoResult: OtocoResultResponse | null
    otocoCancelResult: OtocoCancelResultResponse | null
}

const STATUS_LABEL: Record<string, string> = {
    WAITING_ENTRY: '진입 대기',
    ENTRY_ORDER_PLACED: '진입 주문중',
    WAITING_EXIT: '청산 대기',
    COMPLETED: '완료',
    CANCELED: '취소됨',
}

const CANCELABLE_STATUSES = ['WAITING_ENTRY', 'ENTRY_ORDER_PLACED', 'WAITING_EXIT']

export default function OtocoPanel({
                                       stockCode,
                                       stockName,
                                       curPrice,
                                       leverageRatio,
                                       otocoOrders,
                                       otocoResult,
                                       otocoCancelResult,
                                   }: Props) {
    const [entryDirection, setEntryDirection] = useState<OtocoEntryDirection>('ABOVE')
    const [orderQuantity, setOrderQuantity] = useState(1)
    const [entryTriggerPrice, setEntryTriggerPrice] = useState(curPrice ?? 0)

    const [tpMode, setTpMode] = useState<OtocoExitMode>('PRICE')
    const [tpPrice, setTpPrice] = useState(0)
    const [tpPct, setTpPct] = useState(5)

    const [slMode, setSlMode] = useState<OtocoExitMode>('PRICE')
    const [slPrice, setSlPrice] = useState(0)
    const [slPct, setSlPct] = useState(5)

    const [confirmOpen, setConfirmOpen] = useState(false)
    const [resultModal, setResultModal] = useState<{ success: boolean; message: string } | null>(null)
    const [submitting, setSubmitting] = useState(false)
    const [cancelingId, setCancelingId] = useState<number | null>(null)

    const initializedRef = useRef(false)

    useEffect(() => {
        if (!initializedRef.current && curPrice !== undefined) {
            setEntryTriggerPrice(curPrice)
            setTpPrice(Math.round(curPrice * 1.05))
            setSlPrice(Math.round(curPrice * 0.95))
            initializedRef.current = true
        }
    }, [curPrice])

    useEffect(() => {
        initializedRef.current = false
    }, [stockCode])

    useEffect(() => {
        if (!otocoResult) return
        if (otocoResult.responseType === 'ERROR') {
            setResultModal({ success: false, message: otocoResult.errorMessage ?? '알 수 없는 오류' })
        }
    }, [otocoResult])

    useEffect(() => {
        if (!otocoCancelResult) return
        if (otocoCancelResult.responseType === 'ERROR') {
            setResultModal({ success: false, message: `취소 실패: ${otocoCancelResult.errorMessage ?? '알 수 없는 오류'}` })
        }
    }, [otocoCancelResult])

    const handleSubmit = async () => {
        setSubmitting(true)
        try {
            await placeOtocoOrder({
                stockCode,
                entryDirection,
                orderQuantity,
                entryTriggerPrice,
                tpMode,
                tpPrice: tpMode === 'PRICE' ? tpPrice : null,
                tpPct: tpMode === 'PCT' ? tpPct : null,
                slMode,
                slPrice: slMode === 'PRICE' ? slPrice : null,
                slPct: slMode === 'PCT' ? slPct : null,
                leverageRatio: leverageRatio === 'SPOT' ? null : leverageRatio,
            })
            setResultModal({ success: true, message: 'OTOCO 주문이 등록되었습니다.' })
        } catch (e: any) {
            setResultModal({
                success: false,
                message: `❌ OTOCO 주문 실패: ${e.response?.data?.message ?? e.message}`,
            })
        } finally {
            setSubmitting(false)
            setConfirmOpen(false)
        }
    }

    const handleCancel = async (otocoId: number, targetStockCode: string) => {
        setCancelingId(otocoId)
        try {
            await cancelOtocoOrder({ otocoId, stockCode: targetStockCode })
            // 성공하면 '/user/sub/otoco' 전체 목록이 곧 다시 push되어 자동 갱신됨
        } catch (e: any) {
            setResultModal({
                success: false,
                message: `취소 요청 실패: ${e.response?.data?.message ?? e.message}`,
            })
        } finally {
            setCancelingId(null)
        }
    }

    const sortedOrders = [...otocoOrders].sort(
        (a, b) => new Date(b.orderTime).getTime() - new Date(a.orderTime).getTime()
    )

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' }}>
                <span style={styles.label}>진입 조건</span>
                {(['ABOVE', 'BELOW'] as const).map(d => (
                    <button
                        key={d}
                        onClick={() => setEntryDirection(d)}
                        style={{ ...styles.toggleButton, ...(entryDirection === d ? styles.toggleActive : {}) }}
                    >
                        {d === 'ABOVE' ? '이상' : '이하'}
                    </button>
                ))}
                <input
                    type="number"
                    value={entryTriggerPrice}
                    onChange={e => setEntryTriggerPrice(Number(e.target.value))}
                    style={styles.input}
                    className="no-spinner"
                />
                <span style={styles.unit}>원 도달 시 진입</span>
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

            <div style={styles.exitRow}>
                <span style={styles.label}>익절(TP)</span>
                {(['PRICE', 'PCT'] as const).map(m => (
                    <button key={m} onClick={() => setTpMode(m)} style={{ ...styles.toggleButton, ...(tpMode === m ? styles.toggleActive : {}) }}>
                        {m === 'PRICE' ? '가격' : '%'}
                    </button>
                ))}
                {tpMode === 'PRICE' ? (
                    <input type="number" value={tpPrice} onChange={e => setTpPrice(Number(e.target.value))} style={styles.input} className="no-spinner" />
                ) : (
                    <input type="number" value={tpPct} onChange={e => setTpPct(Number(e.target.value))} style={styles.input} className="no-spinner" />
                )}
                <span style={styles.unit}>{tpMode === 'PRICE' ? '원' : '%'}</span>
            </div>

            <div style={styles.exitRow}>
                <span style={styles.label}>손절(SL)</span>
                {(['PRICE', 'PCT'] as const).map(m => (
                    <button key={m} onClick={() => setSlMode(m)} style={{ ...styles.toggleButton, ...(slMode === m ? styles.toggleActive : {}) }}>
                        {m === 'PRICE' ? '가격' : '%'}
                    </button>
                ))}
                {slMode === 'PRICE' ? (
                    <input type="number" value={slPrice} onChange={e => setSlPrice(Number(e.target.value))} style={styles.input} className="no-spinner" />
                ) : (
                    <input type="number" value={slPct} onChange={e => setSlPct(Number(e.target.value))} style={styles.input} className="no-spinner" />
                )}
                <span style={styles.unit}>{slMode === 'PRICE' ? '원' : '%'}</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'center', marginTop: '4px' }}>
                <button onClick={() => setConfirmOpen(true)} style={styles.submitButton} disabled={submitting}>
                    OTOCO 주문 등록
                </button>
            </div>

            <div style={{ borderTop: '1px solid #333', margin: '8px 0' }} />

            <h4 style={{ color: '#AAA', margin: '0 0 6px 0' }}>내 OTOCO 주문</h4>
            <div style={{ flex: 1, overflowY: 'auto', maxHeight: '220px' }}>
                {sortedOrders.length === 0 ? (
                    <div style={{ color: '#666', fontSize: '13px', textAlign: 'center' }}>등록된 OTOCO 주문 없음</div>
                ) : (
                    sortedOrders.map(o => (
                        <div key={o.otocoId} style={styles.orderCard}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                <span style={{ color: '#AAA' }}>{STATUS_LABEL[o.otocoStatus] ?? o.otocoStatus}</span>
                            </div>
                            <div style={{ fontSize: '12px', color: '#AAA', marginTop: '4px' }}>
                                진입 {o.entryDirection === 'ABOVE' ? '≥' : '≤'} {o.entryTriggerPrice.toLocaleString()}원 · {o.orderQuantity}주
                                {o.leverageRatio && o.leverageRatio !== 'SPOT' && (
                                    <> · {o.leverageRatio.replace('X', '').replace('_', '.')}배</>
                                )}
                            </div>
                            {(o.tpTriggerPrice || o.slTriggerPrice) && (
                                <div style={{ fontSize: '12px', color: '#888', marginTop: '2px' }}>
                                    {o.tpTriggerPrice && <>TP {o.tpTriggerPrice.toLocaleString()}원 </>}
                                    {o.slTriggerPrice && <>SL {o.slTriggerPrice.toLocaleString()}원</>}
                                </div>
                            )}
                            {CANCELABLE_STATUSES.includes(o.otocoStatus) && (
                                <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '6px' }}>
                                    <button
                                        onClick={() => handleCancel(o.otocoId, o.stockCode)}
                                        disabled={cancelingId === o.otocoId}
                                        style={styles.cancelButton}
                                    >
                                        {cancelingId === o.otocoId ? '취소중...' : '취소'}
                                    </button>
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>

            {confirmOpen && (
                <Modal show={true} onClose={() => setConfirmOpen(false)}>
                    <div style={{ textAlign: 'center' }}>
                        <h3>{stockName} OTOCO 주문</h3>
                        <p style={{ fontSize: '13px', lineHeight: 1.6, margin: '12px 0' }}>
                            {entryTriggerPrice.toLocaleString()}원 {entryDirection === 'ABOVE' ? '이상' : '이하'} 도달 시 {orderQuantity}주 진입
                            <br />
                            TP: {tpMode === 'PRICE' ? `${tpPrice.toLocaleString()}원` : `${tpPct}%`} ·
                            SL: {slMode === 'PRICE' ? `${slPrice.toLocaleString()}원` : `${slPct}%`}
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
    exitRow: { display: 'flex', alignItems: 'center', gap: '4px', justifyContent: 'center' },
    toggleButton: { padding: '4px 8px', fontSize: '12px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    toggleActive: { backgroundColor: '#4F9DFF', borderColor: '#4F9DFF' },
    submitButton: { padding: '8px 16px', fontSize: '14px', borderRadius: '6px', border: 'none', backgroundColor: '#4F9DFF', color: '#FFF', cursor: 'pointer', fontWeight: 600 },
    cancelModalButton: { padding: '8px 16px', fontSize: '14px', borderRadius: '6px', border: 'none', backgroundColor: '#333', color: '#FFF', cursor: 'pointer' },
    orderCard: { backgroundColor: '#222', border: '1px solid #333', borderRadius: '8px', padding: '10px 12px', marginBottom: '8px' },
    cancelButton: { padding: '3px 10px', fontSize: '11px', borderRadius: '4px', border: '1px solid #552222', backgroundColor: '#331414', color: '#FF8A80', cursor: 'pointer' },
} as const
