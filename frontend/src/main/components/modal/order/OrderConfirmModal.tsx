import { useState } from 'react'
import ModalV2 from '../../../../components/ModalV2'
import { placeOrder } from '../../../../api/order'
import { tokenStorage } from '../../../../utils/token'
import type { LeverageRatio, OrderType } from '../../../../types/order'

interface Props {
    open: boolean
    onClose: () => void
    onConfirm: () => void
    side: 'buy' | 'sell'
    orderType: 'market' | 'limit' | 'conditional'
    stockName: string
    stockCode: string
    quantity: number
    price: number
    watchPrice?: number
    estimatedAmount: number
    isCredit: boolean
    leverage: number
    profitAmount?: number
    profitRate?: number
}

function toLeverageRatio(leverage: number): LeverageRatio | null {
    switch (leverage) {
        case 1.5: return 'X1_5'
        case 2:   return 'X2'
        case 2.5: return 'X2_5'
        default:  return null // 1배(현금)
    }
}

export default function OrderConfirmModal({
                                              open, onClose, onConfirm,
                                              side, orderType, stockName, stockCode,
                                              quantity, price, estimatedAmount, isCredit, leverage,
                                              profitAmount, profitRate,
                                          }: Props) {
    const [loading, setLoading] = useState(false)
    const [errorMsg, setErrorMsg] = useState<string | null>(null)

    async function handleConfirmClick() {
        if (orderType === 'conditional') {
            setErrorMsg('조건부 주문은 아직 지원되지 않습니다. 고급 주문을 이용해주세요.')
            return
        }

        setLoading(true)
        setErrorMsg(null)

        try {
            await placeOrder({
                stockCode,
                orderType: (side === 'buy' ? 'BUY' : 'SELL') as OrderType,
                orderPrice: price,
                orderQuantity: quantity,
                leverageRatio: isCredit ? toLeverageRatio(leverage) : null,
            })
            onConfirm()
            onClose()
        } catch (e: any) {
            const status = e.response?.status
            if (status === 401 || status === 403) {
                // 세션 만료 — MainLayout의 로그아웃 모달로 위임 (TradePanel과 동일 패턴)
                tokenStorage.clear()
                onClose()
                return
            }
            setErrorMsg(e.response?.data?.message ?? e.message ?? '주문 처리 중 오류가 발생했습니다.')
        } finally {
            setLoading(false)
        }
    }

    return (
        <ModalV2 open={open} title="주문 확인" onClose={onClose}>
            <div style={{ padding: '4px 4px 0', fontSize: 13, lineHeight: 1.8 }}>
                <p>{stockName} ({stockCode})</p>
                <p>{side === 'buy' ? '매수' : '매도'} · {orderType === 'market' ? '시장가' : orderType === 'limit' ? '지정가' : '조건부'}</p>
                <p>{quantity.toLocaleString()}주 · {price.toLocaleString()}원</p>
                {isCredit && <p>신용 {leverage}배</p>}
                <p>예상금액: {estimatedAmount.toLocaleString()}원</p>
                {profitAmount !== undefined && profitRate !== undefined && (
                    <p>예상손익: {profitAmount.toLocaleString()}원 ({profitRate.toFixed(2)}%)</p>
                )}
                {errorMsg && <p style={{ color: '#f04a4a' }}>{errorMsg}</p>}
            </div>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
                <button onClick={onClose} disabled={loading}>취소</button>
                <button onClick={handleConfirmClick} disabled={loading}>
                    {loading ? '처리 중...' : '확인'}
                </button>
            </div>
        </ModalV2>
    )
}
