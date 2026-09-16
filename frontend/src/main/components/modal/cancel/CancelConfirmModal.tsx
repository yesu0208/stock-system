import { useState } from 'react'
import ModalV2 from '../../../../components/ModalV2'
import { cancelOrder } from '../../../../api/orderHistory'
import { cancelAutoOrder } from '../../../../api/autoOrderHistory'
import { tokenStorage } from '../../../../utils/token'
import './CancelConfirmModal.css'

interface Props {
    open: boolean
    onClose: () => void
    onConfirm: () => void
    count: number
    selectedOrders: { rawId: number; stockCode: string; isAuto: boolean; orderId: string }[]
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
        <ModalV2 open={open} title="주문 취소" onClose={onClose} extraClass="ccm-modal">
            <div className="ccm">
                <p className="ccm__message">
                    <span className="ccm__count">{count}건</span>의 주문을 취소하시겠습니까?
                </p>

                {errorMsg && <p className="ccm__error">{errorMsg}</p>}

                <div className="ccm__footer">
                    <button
                        className="ccm__btn ccm__btn--confirm"
                        onClick={handleConfirmClick}
                        disabled={loading}
                    >
                        {loading ? '처리 중...' : '취소 확정'}
                    </button>
                    <button
                        className="ccm__btn ccm__btn--close"
                        onClick={onClose}
                        disabled={loading}
                    >
                        닫기
                    </button>
                </div>
            </div>
        </ModalV2>
    )
}
