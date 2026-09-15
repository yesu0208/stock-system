import { useState } from 'react'
import ModalV2 from '../../../../components/ModalV2.tsx'
import { cancelOrder } from '../../../../api/orderHistory.ts'
import { cancelAutoOrder } from '../../../../api/autoOrderHistory.ts'
import { tokenStorage } from '../../../../utils/token.ts'

interface Props {
    open: boolean
    onClose: () => void
    onConfirm: () => void
    count: number
    selectedOrders: { rawId: number; stockCode: string; isAuto: boolean; orderId: string }[] // [신규]
}

export default function CancelConfirmModal({ open, onClose, onConfirm, count, selectedOrders }: Props) {
    const [loading, setLoading] = useState(false)
    const [errorMsg, setErrorMsg] = useState<string | null>(null)

    async function handleConfirmClick() {
        setLoading(true)
        setErrorMsg(null)
        try {
            await Promise.all(
                selectedOrders.map((o) =>
                    o.isAuto ? cancelAutoOrder(o.rawId, o.stockCode) : cancelOrder(o.rawId, o.stockCode)
                )
            )
            onConfirm()
            onClose()
        } catch (e: any) {
            const status = e.response?.status
            if (status === 401 || status === 403) {
                tokenStorage.clear()
                onClose()
                return
            }
            setErrorMsg(e.response?.data?.message ?? e.message ?? '취소 처리 중 오류가 발생했습니다.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <ModalV2 open={open} title="주문 취소" onClose={onClose}>
            <div style={{ padding: '4px', fontSize: 13, textAlign: 'center' }}>
                <p>선택한 {count}건의 주문을 취소하시겠습니까?</p>
                {errorMsg && <p style={{ color: '#f04a4a' }}>{errorMsg}</p>}
            </div>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
                <button onClick={onClose} disabled={loading}>아니오</button>
                <button onClick={handleConfirmClick} disabled={loading}>
                    {loading ? '처리 중...' : '확인'}
                </button>
            </div>
        </ModalV2>
    )
}
