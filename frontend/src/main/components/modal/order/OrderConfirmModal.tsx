import { useState } from 'react'
import ModalV2 from '../../../../components/ModalV2'
import { placeOrder } from '../../../../api/order'
import { placeAutoOrder } from '../../../../api/autoOrder'
import { tokenStorage } from '../../../../utils/token'
import './OrderConfirmModal.css'
import { useMsg } from "../../../context/MsgContext";
import type { LeverageRatio, OrderType as ApiOrderType } from '../../../../types/order'
import Tooltip from '../../../../tooltip/Tooltip'
import { calculateFee } from '../../../../utils/fee'

type OrderType = 'market' | 'limit' | 'conditional'
type Side = 'buy' | 'sell'

interface Props {
    open: boolean
    onClose: () => void
    onConfirm: () => void
    side: Side
    orderType: OrderType
    stockName: string
    stockCode: string
    quantity: number
    price: number
    watchPrice?: number
    estimatedAmount: number
    estimatedAmountRaw: number
    isCredit: boolean
    leverage: number
    profitAmount?: number
    profitRate?: number
}

const ORDER_TYPE_LABEL: Record<OrderType, string> = {
    market: '시장가',
    limit: '지정가',
    conditional: '조건부',
}

function toLeverageRatio(leverage: number): LeverageRatio | null {
    switch (leverage) {
        case 1.5: return 'X1_5'
        case 2:   return 'X2'
        case 2.5: return 'X2_5'
        default:  return null
    }
}

function LeverageTag({ leverage }: { leverage: number }) {
    const isCash = leverage === 1

    const levClass =
        isCash            ? 'cash' :
            leverage === 1.5 ? 'lev-2' :
                leverage === 2   ? 'lev-3' :
                    leverage === 2.5 ? 'lev-5' :
                        'lev-other'

    return (
        <span className={`leverage-tag ${levClass}`}>
            {isCash ? '현금' : `${leverage}x`}
        </span>
    )
}

export default function OrderConfirmModal({
                                              open, onClose, onConfirm,
                                              side, orderType, stockName, stockCode,
                                              quantity, price, watchPrice, estimatedAmount, estimatedAmountRaw, isCredit, leverage,
                                              profitAmount, profitRate,
                                          }: Props) {
    const [loading, setLoading] = useState(false)

    const { error } = useMsg();

    const isBuy = side === 'buy'
    const sideLabel = isBuy ? '매수' : '매도'
    const creditLabel = isCredit ? '신용' : '현금'

    const buyFee = isBuy ? calculateFee(estimatedAmountRaw) : 0
    const requiredAfter = estimatedAmount + buyFee
    const requiredRaw = estimatedAmountRaw + buyFee

    async function handleConfirmClick() {
        setLoading(true)

        try {
            if (orderType === 'conditional') {
                if (watchPrice == null) {
                    error('감시가가 올바르지 않습니다.')
                    setLoading(false)
                    return
                }

                await placeAutoOrder({
                    stockCode,
                    autoOrderType: side === 'buy' ? 'BUY' : 'SELL',
                    triggerPrice: watchPrice,
                    orderPrice: price,
                    orderQuantity: quantity,
                    leverageRatio: isCredit ? toLeverageRatio(leverage) : null,
                })
            } else {
                await placeOrder({
                    stockCode,
                    orderType: (side === 'buy' ? 'BUY' : 'SELL') as ApiOrderType,
                    orderPrice: price,
                    orderQuantity: quantity,
                    leverageRatio: isCredit ? toLeverageRatio(leverage) : null,
                    orderExecutionType: orderType === 'market' ? 'MARKET' : 'LIMIT',
                })
            }

            onConfirm()
            onClose()
        } catch (e: any) {
            const status = e.response?.status
            if (status === 401 || status === 403) {
                tokenStorage.clear()
                onClose()
                return
            }
            const errMessage = e.response?.data?.message ?? e.message ?? '주문 처리 중 오류가 발생했습니다.'
            error(errMessage)
        } finally {
            setLoading(false)
        }
    }

    return (
        <ModalV2
            open={open}
            title="주문 확인"
            onClose={onClose}
        >
            <div className="ocm">

                <div className="ocm__header">
                    <span className={`ocm__side-badge ocm__side-badge--${side}`}>
                        {creditLabel} {sideLabel}
                    </span>

                    <div className="ocm__stock-info">
                        <span className="ocm__stock-name">{stockName}</span>
                        <span className="ocm__stock-code">{stockCode}</span>
                    </div>

                    <div className="ocm__tag-row">
                        <LeverageTag leverage={isCredit ? leverage : 1} />
                        <span className="ocm__tag-sep">/</span>
                        <span className="ocm__tag">
                            {ORDER_TYPE_LABEL[orderType]}
                        </span>
                    </div>
                </div>

                <div className="ocm__rows">
                    {orderType === 'conditional' ? (
                        <>
                            <div className="ocm__row">
                                <span className="ocm__row-label">수량</span>
                                <span className="ocm__row-value">{quantity.toLocaleString()} 주</span>
                            </div>

                            <div className="ocm__row">
                                <span className="ocm__row-label">감시가</span>
                                <span className="ocm__row-value ocm__row-value--trigger">
                                    {isBuy ? '≥' : '≤'} {watchPrice?.toLocaleString()} 원
                                </span>
                            </div>

                            <div className="ocm__row">
                                <span className="ocm__row-label">주문가</span>
                                <span className="ocm__row-value">{price.toLocaleString()} 원</span>
                            </div>

                            <div className="ocm__divider" />

                            {isBuy ? (
                                <>
                                    <div className="ocm__row ocm__row--stacked">
                                        <span className="ocm__row-label">
                                            주문금액
                                            <LeverageTag leverage={isCredit ? leverage : 1} />
                                        </span>
                                        <span className="ocm__row-value ocm__row-value--amount buy">
                                            {isCredit ? (
                                                <>
                                                    <span>{estimatedAmount.toLocaleString()} 원</span>
                                                    <span className="ocm__row-value--before">
                                                        {estimatedAmountRaw.toLocaleString()} 원
                                                    </span>
                                                </>
                                            ) : (
                                                `${estimatedAmount.toLocaleString()} 원`
                                            )}
                                        </span>
                                    </div>

                                    <div className="ocm__row ocm__row--stacked">
                                        <span className="ocm__row-label">
                                            필요금액
                                            <LeverageTag leverage={isCredit ? leverage : 1} />
                                            <Tooltip
                                                text={`매수 수수료(0.015%) ${buyFee.toLocaleString()}원 포함`}
                                                placement="top"
                                            >
                                                <span className="ocm__fee-tag">수수료</span>
                                            </Tooltip>
                                        </span>
                                        <span className="ocm__row-value ocm__row-value--amount buy">
                                            {isCredit ? (
                                                <>
                                                    <span>{requiredAfter.toLocaleString()} 원</span>
                                                    <span className="ocm__row-value--before">
                                                        {requiredRaw.toLocaleString()} 원
                                                    </span>
                                                </>
                                            ) : (
                                                `${requiredAfter.toLocaleString()} 원`
                                            )}
                                        </span>
                                    </div>
                                </>
                            ) : (
                                <div className="ocm__row">
                                    <span className="ocm__row-label">예상 금액</span>
                                    <span className={`ocm__row-value ocm__row-value--amount ${side}`}>
                                        {estimatedAmount.toLocaleString()} 원
                                    </span>
                                </div>
                            )}

                            {side === 'sell' && profitAmount !== undefined && profitRate !== undefined && (
                                <div className="ocm__row">
                                    <span className="ocm__row-label">예상 손익</span>
                                    <span className={`ocm__row-value ${
                                        profitAmount > 0 ? 'ocm__row-value--profit' :
                                            profitAmount < 0 ? 'ocm__row-value--loss' : ''
                                    }`}>
                                        {profitAmount > 0 ? '+' : profitAmount < 0 ? '−' : ''}
                                        {Math.abs(profitAmount).toLocaleString()}원&nbsp;
                                        ({profitRate > 0 ? '+' : profitRate < 0 ? '−' : ''}
                                        {Math.abs(profitRate).toFixed(2)}%)
                                    </span>
                                </div>
                            )}
                        </>
                    ) : (
                        <>
                            <div className="ocm__row">
                                <span className="ocm__row-label">수량</span>
                                <span className="ocm__row-value">{quantity.toLocaleString()} 주</span>
                            </div>

                            {orderType === 'market' ? (
                                <div className="ocm__row">
                                    <span className="ocm__row-label">예상 가격</span>
                                    <span className="ocm__row-value">
                                        {price > 0 ? `${price.toLocaleString()} 원` : '—'}
                                    </span>
                                </div>
                            ) : (
                                <div className="ocm__row">
                                    <span className="ocm__row-label">주문가</span>
                                    <span className="ocm__row-value">{price.toLocaleString()} 원</span>
                                </div>
                            )}

                            <div className="ocm__divider" />

                            {isBuy ? (
                                <>
                                    <div className="ocm__row ocm__row--stacked">
                                        <span className="ocm__row-label">
                                            주문금액
                                            <LeverageTag leverage={isCredit ? leverage : 1} />
                                        </span>
                                        <span className="ocm__row-value ocm__row-value--amount buy">
                                            {isCredit ? (
                                                <>
                                                    <span>{estimatedAmount.toLocaleString()} 원</span>
                                                    <span className="ocm__row-value--before">
                                                        {estimatedAmountRaw.toLocaleString()} 원
                                                    </span>
                                                </>
                                            ) : (
                                                `${estimatedAmount.toLocaleString()} 원`
                                            )}
                                        </span>
                                    </div>

                                    <div className="ocm__row ocm__row--stacked">
                                        <span className="ocm__row-label">
                                            필요금액
                                            <LeverageTag leverage={isCredit ? leverage : 1} />
                                            <Tooltip
                                                text={`매수 수수료(0.015%) ${buyFee.toLocaleString()}원 포함`}
                                                placement="top"
                                            >
                                                <span className="ocm__fee-tag">수수료</span>
                                            </Tooltip>
                                        </span>
                                        <span className="ocm__row-value ocm__row-value--amount buy">
                                            {isCredit ? (
                                                <>
                                                    <span>{requiredAfter.toLocaleString()} 원</span>
                                                    <span className="ocm__row-value--before">
                                                        {requiredRaw.toLocaleString()} 원
                                                    </span>
                                                </>
                                            ) : (
                                                `${requiredAfter.toLocaleString()} 원`
                                            )}
                                        </span>
                                    </div>
                                </>
                            ) : (
                                <div className="ocm__row">
                                    <span className="ocm__row-label">예상 금액</span>
                                    <span className={`ocm__row-value ocm__row-value--amount ${side}`}>
                                        {estimatedAmount.toLocaleString()} 원
                                    </span>
                                </div>
                            )}

                            {side === 'sell' && profitAmount !== undefined && profitRate !== undefined && (
                                <div className="ocm__row">
                                    <span className="ocm__row-label">예상 손익</span>
                                    <span className={`ocm__row-value ${
                                        profitAmount > 0 ? 'ocm__row-value--profit' :
                                            profitAmount < 0 ? 'ocm__row-value--loss' : ''
                                    }`}>
                                        {profitAmount > 0 ? '+' : profitAmount < 0 ? '−' : ''}
                                        {Math.abs(profitAmount).toLocaleString()}원&nbsp;
                                        ({profitRate > 0 ? '+' : profitRate < 0 ? '−' : ''}
                                        {Math.abs(profitRate).toFixed(2)}%)
                                    </span>
                                </div>
                            )}
                        </>
                    )}
                </div>

                {orderType === 'market' && (
                    <p className="ocm__market-notice">
                        <strong>⚠ 주의사항</strong>
                        <br />
                        시장가 주문은 현재 호가창 기준으로 체결됩니다.
                        <br />
                        실제 체결가는 표시된 예상 가격과 다를 수 있습니다.
                    </p>
                )}

                {orderType === 'conditional' && watchPrice != null && (
                    <p className="ocm__market-notice">
                        <strong>
                            <span style={{ color: 'var(--color-text-primary)' }}>
                                종목 가격이&nbsp;
                            </span>
                            <span className={isBuy ? 'ocm__notice--buy' : 'ocm__notice--sell'}>
                                {watchPrice?.toLocaleString()}원 {isBuy ? '이상' : '이하'}
                            </span>
                            <span style={{ color: 'var(--color-text-primary)' }}>
                                &nbsp;도달 시&nbsp;
                            </span>
                            <br />
                            <span className={isBuy ? 'ocm__notice--buy' : 'ocm__notice--sell'}>
                                {price.toLocaleString()}원으로 {sideLabel}
                            </span>
                            <span style={{ color: 'var(--color-text-primary)' }}>
                                &nbsp;합니다.
                            </span>
                        </strong>
                        <br />
                        <span className="ocm__notice--sub">
                            주문가격과 감시가격의 차이가 클 경우,
                            <br />체결이 늦어질 수 있습니다.
                        </span>
                    </p>
                )}

                <div className="ocm__footer">
                    <button
                        className={`ocm__btn ocm__btn--confirm-${side}`}
                        onClick={handleConfirmClick}
                        disabled={loading}
                    >
                        {loading ? '처리 중...' : `${sideLabel} 확정`}
                    </button>
                    <button className="ocm__btn ocm__btn--cancel" onClick={onClose} disabled={loading}>
                        취소
                    </button>
                </div>

            </div>
        </ModalV2>
    )
}
