import {type ReactNode, useEffect, useRef, useState} from 'react'
import {stockNameMap} from '../../constants/stocks'
import type {
    OrderResultResponse,
    OrderResponseMessage,
    OrderResponse
} from '../../types/order'
import type { CancelResultResponse } from '../../types/cancel'
import type { TradeResponse } from '../../types/trade'
import type { AccountResponse } from '../../types/account'
import './TradePanel.css'
import type {
    AutoOrderResultResponse,
    AutoOrderResponseMessage,
} from '../../types/autoOrder'
import type {AutoCancelResultResponse} from "../../types/autoCancel.ts";
import api from '../../lib/api' // axios instance
import Modal from '../Modal.tsx'
import {tokenStorage} from "../../utils/token.ts";
import axios from 'axios'

interface Props {
    stockCode: string
    stockName: string
    isPriceReady: boolean
    curPrice?: number
    orderResult: OrderResultResponse | null
    cancelResult: CancelResultResponse | null
    tradeResult: TradeResponse | null
    orders: OrderResponseMessage[]
    autoOrders: AutoOrderResponseMessage[]
    autoOrderResult: AutoOrderResultResponse | null
    accountInfo: AccountResponse | null
    autoCancelResult: AutoCancelResultResponse | null
}

type TabType = 'ORDER' | 'AUTO' | 'ACCOUNT'

type ToastPayload = {
    responseType: 'SUCCESS' | 'ERROR'
    message: ReactNode
}

export default function TradePanel({
                                       stockCode,
                                       stockName,
                                       isPriceReady,
                                       curPrice,
                                       orderResult,
                                       cancelResult,
                                       tradeResult,
                                       orders,
                                       autoOrderResult,
                                       autoCancelResult,
                                       accountInfo,
                                   }: Props) {
    const [orderQuantity, setOrderQuantity] = useState<number>(1)

    const [orderPrice, setOrderPrice] = useState<number>(0)
    const initializedRef = useRef(false)
    const triggerInitializedRef = useRef(false)

    const [toast, setToast] = useState<ToastPayload | null>(null)
    const [now, setNow] = useState(new Date())

    const [activeTab, setActiveTab] = useState<TabType>('ORDER')
    const [loading, setLoading] = useState(false);

    const orderAmount = orderPrice * orderQuantity;


    // HTTP 주문 모달 상태
    const [httpResponseModal, setHttpResponseModal] = useState<{
        success: boolean
        message: React.ReactNode
    } | null>(null)

    // 상태 추가
    const [confirmCancelModal, setConfirmCancelModal] = useState<{
        orderId: number;
        stockCode: string;
    } | null>(null);

    // 모달 상태
    const [confirmOrderModal, setConfirmOrderModal] = useState<{
        type: 'BUY' | 'SELL'
        stockCode: string
        stockName: string
        orderPrice: number
        orderQuantity: number
    } | null>(null)

    const toastTimerRef = useRef<number | null>(null)

    useEffect(() => {
        initializedRef.current = false
    }, [stockCode])

    useEffect(() => {
        initializedRef.current = false
        triggerInitializedRef.current = false
    }, [stockCode])

    useEffect(() => {
        if (!initializedRef.current && curPrice !== undefined) {
            setOrderPrice(curPrice)
            initializedRef.current = true
        }
    }, [curPrice])

    /* ===== 주문 결과 토스트 ===== */
    useEffect(() => {
        if (!orderResult) return

        // 이전 toast 타이머 무조건 제거
        if (toastTimerRef.current) {
            clearTimeout(toastTimerRef.current)
            toastTimerRef.current = null
        }

        setToast({
            responseType: orderResult.responseType,
            message:
                orderResult.responseType === 'SUCCESS' ? (
                    <div>
                        <span style={{ color: '#AAA' }}>[주문] </span>
                        <span>
                    {stockNameMap[orderResult.stockCode] ?? orderResult.stockCode}
                </span>{' '}
                        <span>
                    {orderResult.orderPrice?.toLocaleString()}원{' '}
                </span>
                        <span>
                    {orderResult.orderQuantity}주{' '}
                </span>
                        <span
                            style={{
                                color:
                                    orderResult.orderType === 'BUY'
                                        ? '#FF6347' // 매수 빨강
                                        : '#4F9DFF', // 매도 파랑
                                fontWeight: 600,
                            }}
                        >
                    {orderResult.orderType === 'BUY' ? '매수' : '매도'}
                </span>
                    </div>
                ) : (
                    `❌ [주문 실패] ${orderResult.errorMessage}`
                ),
        })


        toastTimerRef.current = window.setTimeout(() => {
            setToast(null)
            toastTimerRef.current = null
        }, 5000)
    }, [orderResult])

    /* ===== 취소 결과 토스트 ===== */
    useEffect(() => {
        if (!cancelResult) return

        // 이전 toast 타이머 무조건 제거
        if (toastTimerRef.current) {
            clearTimeout(toastTimerRef.current)
            toastTimerRef.current = null
        }

        setToast({
            responseType: cancelResult.responseType,
            message:
                cancelResult.responseType === 'SUCCESS' ? (
                    <div>
                        <span style={{ color: '#AAA' }}>[주문 취소] </span>
                        <span>
                    {stockNameMap[cancelResult.stockCode] ??
                        cancelResult.stockCode}
                </span>{' '}
                        <span>
                    {cancelResult.orderPrice?.toLocaleString()}원{' '}
                </span>
                        <span>
                    {cancelResult.orderQuantity}주{' '}
                </span>
                        <span
                            style={{
                                color:
                                    cancelResult.orderType === 'BUY'
                                        ? '#FF6347' // 매수 빨강
                                        : '#4F9DFF', // 매도 파랑
                                fontWeight: 600,
                            }}
                        >
                    {cancelResult.orderType === 'BUY' ? '매수 취소' : '매도 취소'}
                </span>

                    </div>
                ) : (
                    `❌ ${cancelResult.errorMessage}`
                ),
        })

        toastTimerRef.current = window.setTimeout(() => {
            setToast(null)
            toastTimerRef.current = null
        }, 5000)
    }, [cancelResult])

    /* ===== 자동주문 취소 결과 토스트 ===== */

    useEffect(() => {
        if (!autoCancelResult) return

        // 이전 toast 타이머 무조건 제거
        if (toastTimerRef.current) {
            clearTimeout(toastTimerRef.current)
            toastTimerRef.current = null
        }

        setToast({
            responseType: autoCancelResult.responseType,
            message:
                autoCancelResult.responseType === 'SUCCESS' ? (
                    <div>
                        <span style={{ color: '#AAA' }}>[자동주문 취소] </span>
                        <span>
                    {stockNameMap[autoCancelResult.stockCode] ??
                        autoCancelResult.stockCode}
                </span>{' '}
                        <span>
                    {autoCancelResult.orderPrice?.toLocaleString()}원{' '}
                </span>
                        <span>(감시가:{' '}
                            {autoCancelResult.triggerPrice?.toLocaleString()}원){' '}
                </span>
                        <span>
                    {autoCancelResult.orderQuantity}주{' '}
                </span>
                        <span
                            style={{
                                color:
                                    autoCancelResult.autoOrderType === 'BUY'
                                        ? '#FF6347' // 매수 빨강
                                        : '#4F9DFF', // 매도 파랑
                                fontWeight: 600,
                            }}
                        >
                    {autoCancelResult.autoOrderType === 'BUY' ? '자동 매수 취소' : '자동 매도 취소'}
                </span>

                    </div>
                ) : (
                    `❌ ${autoCancelResult.errorMessage}`
                ),
        })

        toastTimerRef.current = window.setTimeout(() => {
            setToast(null)
            toastTimerRef.current = null
        }, 5000)
    }, [autoCancelResult])

    /* ===== 체결 결과 토스트 ===== */
    useEffect(() => {
        if (!tradeResult) return

        // 이전 toast 타이머 무조건 제거
        if (toastTimerRef.current) {
            clearTimeout(toastTimerRef.current)
            toastTimerRef.current = null
        }

        setToast({
            responseType: 'SUCCESS',
            message: (
                <div>
                    <span style={{ color: '#AAA' }}>[체결] </span>

                    <span>
                {stockNameMap[tradeResult.stockCode] ??
                    tradeResult.stockCode}
            </span>{' '}

                    <span>
                {tradeResult.tradePrice.toLocaleString()}원{' '}
            </span>

                    <span>
                {tradeResult.tradeQuantity}주{' '}
            </span>

                    <span
                        style={{
                            color:
                                tradeResult.tradeType === 'BUY'
                                    ? '#FF6347' // 매수 빨강
                                    : '#4F9DFF', // 매도 파랑
                            fontWeight: 600,
                        }}
                    >
                {tradeResult.tradeType === 'BUY' ? '매수' : '매도'}
            </span>
                </div>
            ),
        })


        toastTimerRef.current = window.setTimeout(() => {
            setToast(null)
            toastTimerRef.current = null
        }, 5000)
    }, [tradeResult])

    /* ===== 자동주문 결과 토스트 ===== */
    useEffect(() => {
        if (!autoOrderResult) return

        // 이전 toast 타이머 무조건 제거
        if (toastTimerRef.current) {
            clearTimeout(toastTimerRef.current)
            toastTimerRef.current = null
        }

        setToast({
            responseType: autoOrderResult.responseType,
            message:
                autoOrderResult.responseType === 'SUCCESS' ? (
                    <div>
                        <span style={{ color: '#AAA' }}>[자동주문] </span>

                        <span>
                    {stockNameMap[autoOrderResult.stockCode] ??
                        autoOrderResult.stockCode}
                </span>{' '}

                        <span>
                    감시가 {autoOrderResult.triggerPrice.toLocaleString()}원 ·{' '}
                </span>

                        <span>
                    주문가 {autoOrderResult.orderPrice.toLocaleString()}원{' '}
                </span>

                        <span>
                    {autoOrderResult.orderQuantity}주{' '}
                </span>

                        <span
                            style={{
                                color:
                                    autoOrderResult.autoOrderType === 'BUY'
                                        ? '#FF6347' // 자동 매수
                                        : '#4F9DFF', // 자동 매도
                                fontWeight: 600,
                                marginRight: '6px',
                            }}
                        >
                    {autoOrderResult.autoOrderType === 'BUY'
                        ? '자동 매수'
                        : '자동 매도'}
                </span>
                    </div>
                ) : (
                    `❌ [자동주문 실패] ${autoOrderResult.errorMessage}`
                ),
        })


        toastTimerRef.current = window.setTimeout(() => {
            setToast(null)
            toastTimerRef.current = null
        }, 5000)
    }, [autoOrderResult])

    useEffect(() => {
        const timer = setInterval(() => setNow(new Date()), 1000)
        return () => clearInterval(timer)
    }, [])

    const getStep = (price: number) => {
        if (price < 2000) return 1
        if (price < 5000) return 5
        if (price < 20000) return 10
        if (price < 50000) return 50
        if (price < 200000) return 100
        if (price < 500000) return 500
        return 1000
    }

    const adjustPrice = (price: number) => {
        const step = getStep(price)
        return Math.max(step, Math.round(price / step) * step)
    }

    const openCancelModal = (orderId: number, stockCode: string) => {
        setConfirmCancelModal({ orderId, stockCode});
    }

    const handleConfirmCancel = async () => {
        if (!confirmCancelModal) return

        try {
            await api.post('/cancels', {
                orderId: confirmCancelModal.orderId,
                stockCode: confirmCancelModal.stockCode,
            })

            setHttpResponseModal({
                success: true,
                message: '미체결 주문이 취소되었습니다.',
            })
        } catch (e: unknown) {
            if (axios.isAxiosError(e)) {
                const status = e.response?.status

                if (status === 401 || status === 403) {
                    // 세션 만료 처리 → MainLayout에서 모달 표시
                    tokenStorage.clear()
                } else {
                    const errMessage =
                        e.response?.data?.message ??
                        e.response?.data ??
                        e.message

                    setHttpResponseModal({
                        success: false,
                        message: `❌ [주문 취소 실패] ${errMessage}`,
                    })
                }
            } else {
                setHttpResponseModal({
                    success: false,
                    message: '❌ 알 수 없는 오류가 발생했습니다.',
                })
            }
        } finally {
            setConfirmCancelModal(null)
        }
    }


    const sortedOrders = [...orders].sort(
        (a, b) =>
            new Date(b.orderTime).getTime() -
            new Date(a.orderTime).getTime()
    )

    const [quantityWarning, setQuantityWarning] = useState<string | null>(null);

    const maxSellQuantity = accountInfo?.stocks?.[stockCode]?.availableQuantity ?? 0;
    const maxBuyQuantity = curPrice && accountInfo
        ? Math.floor(accountInfo.availableCash / orderPrice)
        : 0;

    const warningTimeout = useRef<number | null>(null);

    const handleOrderClick = (type: 'BUY' | 'SELL') => {
        if (type === 'BUY' && orderQuantity > maxBuyQuantity) {
            setQuantityWarning(`❌ 주문 수량이 매수 가능 수량(${maxBuyQuantity}주)을 초과했습니다.`);

            if (warningTimeout.current) clearTimeout(warningTimeout.current);
            warningTimeout.current = setTimeout(() => setQuantityWarning(null), 2000);
            return;
        }

        if (type === 'SELL' && orderQuantity > maxSellQuantity) {
            setQuantityWarning(`❌ 주문 수량이 매도 가능 수량(${maxSellQuantity}주)을 초과했습니다.`);

            if (warningTimeout.current) clearTimeout(warningTimeout.current);
            warningTimeout.current = setTimeout(() => setQuantityWarning(null), 2000);
            return;
        }

        // 수량 정상 → 주문
        setQuantityWarning(null);
        onClickOrderButton(type);
    };

    // 자동주문 버튼 클릭 시 → 모달 띄우기
    const onClickOrderButton = (type: 'BUY' | 'SELL') => {
        setConfirmOrderModal({
            type,
            stockCode,
            stockName,
            orderPrice: adjustPrice(orderPrice),
            orderQuantity,
        })
    }

    const handleConfirmOrder = async () => {
        if (!confirmOrderModal) return

        setLoading(true)

        try {
            const res = await api.post<OrderResponse>('/orders', {
                stockCode: confirmOrderModal.stockCode,
                orderType: confirmOrderModal.type,
                orderPrice: confirmOrderModal.orderPrice,
                orderQuantity: confirmOrderModal.orderQuantity,
            })

            const data = res.data

            setHttpResponseModal({
                success: true,
                message: (
                    <div style={{ textAlign: 'center' }}>
                        <h3 style={{ marginBottom: '12px' }}>
                            {confirmOrderModal.stockName} ({data.stockCode})
                        </h3>

                        <div style={styles.divider} />

                        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px', marginBottom: '12px' }}>
                            <tbody>
                            <tr>
                                <td style={styles.modalLabel}>주문구분</td>
                                <td>{data.orderType === 'BUY' ? '매수' : '매도'}</td>
                            </tr>
                            <tr>
                                <td style={styles.modalLabel}>주문가격</td>
                                <td>{data.orderPrice.toLocaleString()}원</td>
                            </tr>
                            <tr>
                                <td style={styles.modalLabel}>주문수량</td>
                                <td>{data.orderQuantity}주</td>
                            </tr>
                            <tr>
                                <td style={styles.modalLabel}>주문금액</td>
                                <td>{(data.orderPrice * data.orderQuantity).toLocaleString()}원</td>
                            </tr>
                            </tbody>
                        </table>

                        <div style={styles.divider} />

                        <p style={{ fontSize: '12px', lineHeight: 1.5 }}>
                        <span
                            style={{
                                color: data.orderType === 'BUY' ? '#FF6347' : '#4F9DFF',
                                fontWeight: 'bold',
                            }}
                        >
                            {data.orderPrice.toLocaleString()}원으로 {data.orderType === 'BUY' ? '매수' : '매도'}
                        </span>{' '}
                            완료되었습니다.
                        </p>
                    </div>
                ),
            })

        } catch (e: unknown) {
            if (axios.isAxiosError(e)) {
                const status = e.response?.status

                if (status === 401 || status === 403) {
                    tokenStorage.clear()
                } else {
                    const errMessage =
                        e.response?.data?.message ??
                        e.response?.data ??
                        e.message

                    setHttpResponseModal({
                        success: false,
                        message: `❌ [주문 실패] ${errMessage}`,
                    })
                }
            } else {
                setHttpResponseModal({
                    success: false,
                    message: '❌ 알 수 없는 오류가 발생했습니다.',
                })
            }
        }
    }



// 모달에서 취소 버튼 클릭 → 모달 닫기
    const handleCancelOrderModal = () => setConfirmOrderModal(null)

    return (
        <div style={styles.wrapper}>

            {/*  토스트 / 시계 */}
                <Modal
                    show={!!confirmCancelModal}
                    onClose={() => setConfirmCancelModal(null)}
                >
                    <p style={{ marginBottom: '12px', lineHeight: 1.5, fontSize: '14px', textAlign: 'center' }}>
                        미체결 주문을 취소하시겠습니까?
                    </p>
                    <div style={{ display: 'flex', justifyContent: 'center', gap: '12px' }}>
                        <button style={styles.modalButton} onClick={handleConfirmCancel}>확인</button>
                        <button style={styles.modalButton} onClick={() => setConfirmCancelModal(null)}>취소</button>
                    </div>
                </Modal>
            )}

            {confirmOrderModal && (
                <Modal show={!!confirmOrderModal} onClose={handleCancelOrderModal}>
                    {confirmOrderModal && (
                        <div style={{ textAlign: 'center' }}> {/* 모달 전체 가운데 정렬 */}
                            <h3 style={{ marginBottom: '12px' }}>
                                {confirmOrderModal.stockName} ({confirmOrderModal.stockCode})
                            </h3>
                            <div style={styles.divider} />

                            <table style={{ width: '100%', marginBottom: '12px', borderCollapse: 'collapse', fontSize: '14px' }}>
                                <tbody>
                                <tr>
                                    <td style={{ ...styles.modalLabel, textAlign: 'center' }}>주문구분</td>
                                    <td style={{ textAlign: 'center' }}>{confirmOrderModal.type === 'BUY' ? '매수' : '매도'}</td>
                                </tr>
                                <tr>
                                    <td style={{ ...styles.modalLabel, textAlign: 'center' }}>주문가격</td>
                                    <td style={{ textAlign: 'center' }}>{confirmOrderModal.orderPrice.toLocaleString()}원</td>
                                </tr>
                                <tr>
                                    <td style={{ ...styles.modalLabel, textAlign: 'center' }}>주문수량</td>
                                    <td style={{ textAlign: 'center' }}>{confirmOrderModal.orderQuantity}주</td>
                                </tr>
                                <tr>
                                    <td style={{ ...styles.modalLabel, textAlign: 'center' }}>주문금액</td>
                                    <td style={{ textAlign: 'center' }}>{(confirmOrderModal.orderPrice * confirmOrderModal.orderQuantity).toLocaleString()}원</td>
                                </tr>
                                </tbody>
                            </table>

                            <div style={styles.divider} />

                            <p style={{ marginBottom: '12px', lineHeight: 1.5, fontSize: '12px' }}>
                                {confirmOrderModal.stockName}{' '}
                                <span style={{ color: confirmOrderModal.type === 'BUY' ? '#FF6347' : '#4F9DFF', fontWeight: 'bold' }}>
            {confirmOrderModal.orderPrice.toLocaleString()}원으로 {confirmOrderModal.type === 'BUY' ? '매수' : '매도'}
          </span>{' '}
                                합니다.
                            </p>

                            <div style={styles.modalButtonContainer}>
                                <button style={styles.modalButton} disabled={loading} onClick={handleConfirmOrder}>확인</button>
                                <button style={styles.modalButton} onClick={handleCancelOrderModal}>취소</button>
                            </div>
                        </div>
                    )}
                </Modal>
            )}



            {/* HTTP Response 모달 */}
            {httpResponseModal && (
                <Modal show={!!httpResponseModal} onClose={() => setHttpResponseModal(null)}>
                    {httpResponseModal && (
                        <div style={{ textAlign: 'center' }}> {/* 전체 가운데 정렬 */}
                            <p>{httpResponseModal.message}</p>
                            <div style={{ marginTop: '12px' }}> {/* 버튼 간격 확보 */}
                                <button style={styles.modalButton} onClick={() => setHttpResponseModal(null)}>
                                    확인
                                </button>
                            </div>
                        </div>
                    )}
                </Modal>
            )}
            {/* 토스트 / 시계 */}
            <div
                style={{
                    ...styles.toastContainer,
                    display: 'flex',
                    flexDirection: 'column', // 위에서 아래로 쌓기
                    alignItems: 'center',    // 가로 중앙 정렬
                }}
            >
                {/* 토스트 또는 시계 */}
                {toast ? (
                    <div
                        style={{
                            ...styles.toast,
                            backgroundColor: toast.responseType === 'SUCCESS' ? '#2A2A2A' : '#C62828',
                        }}
                    >
                        {toast.message}
                    </div>
                ) : (
                    <div
                        style={{
                            ...styles.toast,
                            backgroundColor: '#2A2A2A', // 시계 배경색
                            color: '#FFF',
                            padding: '10px 16px',
                            borderRadius: '8px',
                            fontWeight: 'bold',
                            textAlign: 'center',
                            minWidth: '180px',
                        }}
                    >
                        {/* 현재 시각 */}
                        <div>
                            {now.toLocaleDateString('ko-KR', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                weekday: 'short',
                            })}{' '}
                            {now.toLocaleTimeString('ko-KR', { hour12: false })}
                        </div>
                    </div>
                )}

                {/* 장 타이머 정보 (항상 표시, 15:40 이후 제외) */}
                {(() => {
                    const day = now.getDay(); // 0:일, 6:토
                    if (day === 0 || day === 6) return null; // 주말 제외

                    const getTargetTime = (h: number, m: number) => {
                        const t = new Date(now);
                        t.setHours(h, m, 0, 0);
                        return t;
                    };

                    const diffText = (target: Date) => {
                        const diff = target.getTime() - now.getTime();
                        if (diff <= 0) return '0분 0초';

                        const totalSec = Math.floor(diff / 1000);

                        const hour = Math.floor(totalSec / 3600);
                        const min = Math.floor((totalSec % 3600) / 60);
                        const sec = totalSec % 60;

                        if (hour > 0) {
                            return `${hour}시간 ${min}분 ${sec}초`;
                        }

                        return `${min}분 ${sec}초`;
                    };

                    const hour = now.getHours();
                    const minute = now.getMinutes();

                    let mainText = '';
                    let subText = '';

                    // 08:00 ~ 08:50
                    if ((hour === 8 && minute >= 0 && minute < 50)) {
                        mainText = `⏳ 장 시작(09:00)까지 ${diffText(getTargetTime(9, 0))} ⏳`;
                        subText = `동시호가(08:50)까지 ${diffText(getTargetTime(8, 50))}`;
                    }

                    // 08:50 ~ 09:00
                    else if (hour === 8 && minute >= 50) {
                        mainText = `⏳ 장 시작(09:00)까지 ${diffText(getTargetTime(9, 0))} ⏳`;
                        subText = `동시호가 진행중`;
                    }

                    // 09:00 ~ 15:20
                    else if (
                        (hour > 9 && hour < 15) ||
                        (hour === 9) ||
                        (hour === 15 && minute < 20)
                    ) {
                        mainText = `⏳ 장 종료(15:30)까지 ${diffText(getTargetTime(15, 30))} ⏳`;
                        subText = `동시호가(15:20)까지 ${diffText(getTargetTime(15, 20))}`;
                    }

                    // 15:20 ~ 15:30
                    else if (hour === 15 && minute >= 20 && minute < 30) {
                        mainText = `⏳ 장 종료(15:30)까지 ${diffText(getTargetTime(15, 30))} ⏳`;
                        subText = `동시호가 진행중`;
                    }

                    // 15:30 ~ 15:40
                    else if (hour === 15 && minute >= 30 && minute < 40) {
                        mainText = `⏳ ${diffText(getTargetTime(15, 40))} 후에 서버 주문/계좌 정리 ⏳`;
                    }

                    // 15:40 이후는 표시 안함
                    else {
                        return null;
                    }

                    return (
                        <div
                            style={{
                                marginTop: '6px',
                                padding: '6px 10px',
                                borderRadius: '6px',
                                backgroundColor: '#333',
                                color: '#FF6347',
                                fontSize: '0.8rem',
                                textAlign: 'center',
                                lineHeight: '1.4',
                            }}
                        >
                            <div>{mainText}</div>
                            {subText && <div style={{ opacity: 0.8 }}>{subText}</div>}
                        </div>
                    );
                })()}


                {/* 항상 보이는 시장 상태 바 */}
                <div
                    style={{
                        marginTop: '8px',        // 위쪽과 간격
                        padding: '4px 10px',     // 글씨 주변 여백
                        borderRadius: '6px',
                        backgroundColor: (() => {
                            const hour = now.getHours();
                            const minute = now.getMinutes();
                            if ((hour === 8 && minute >= 50) || (hour === 9 && minute < 0)) return '#4F9DFF'; // Opening
                            if ((hour > 9 && hour < 15) || (hour === 9 && minute >= 0) || (hour === 15 && minute < 20)) return '#2E7D32'; // Open
                            if (hour === 15 && minute >= 20 && minute < 30) return '#4F9DFF'; // Closing
                            return '#9E9E9E'; // Closed
                        })(),
                        color: '#FFF',
                        fontSize: '0.85rem',
                        fontWeight: 'bold',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {(() => {
                        const hour = now.getHours();
                        const minute = now.getMinutes();
                        if ((hour === 8 && minute >= 50) || (hour === 9 && minute < 0)) return 'Opening (8:50~9:00)';
                        if ((hour > 9 && hour < 15) || (hour === 9 && minute >= 0) || (hour === 15 && minute < 20)) return 'Open (9:00~15:20)';
                        if (hour === 15 && minute >= 20 && minute < 30) return 'Closing (15:20~15:30)';
                        return 'Closed';
                    })()}
                </div>
            </div>

            {/* ===== 탭 (왼쪽: 주문/자동주문, 오른쪽: 계좌/정보) ===== */}
            <div style={styles.tabContainer}>
                <div style={{ display: 'flex', gap: '12px' }}>
                    <button
                        onClick={() => setActiveTab('ORDER')}
                        style={{
                            ...styles.tab,
                            ...(activeTab === 'ORDER' ? styles.activeTab : {}),
                        }}
                    >
                        주문
                    </button>
                    <button
                        onClick={() => setActiveTab('AUTO')}
                        style={{
                            ...styles.tab,
                            ...(activeTab === 'AUTO' ? styles.activeTab : {}),
                        }}
                    >
                        자동주문
                    </button>
                </div>

                <div style={{ marginLeft: 'auto' }}>
                    <button
                        onClick={() => setActiveTab('ACCOUNT')}
                        style={{
                            ...styles.tab,
                            ...(activeTab === 'ACCOUNT' ? styles.activeTab : {}),
                        }}
                    >
                        계좌 정보
                    </button>
                </div>
            </div>

            {/* 종목 정보 (ORDER / AUTO 탭에서만 표시) */}
            {activeTab !== 'ACCOUNT' && (
                <div
                    style={{
                        ...styles.stockHeader,
                        display: 'flex',
                        justifyContent: 'center', // 수평 가운데
                        alignItems: 'center',     // 수직 가운데
                        gap: '4px',               // 종목명과 코드 사이 간격
                    }}
                >
                    <span style={styles.stockName}>{stockName}</span>
                    <span style={styles.stockCode}>({stockCode})</span>
                </div>
            )}

            {activeTab !== 'ACCOUNT' && (
                !isPriceReady || curPrice === undefined ? (
                    <div style={styles.loading}>주문 시스템 생성중...</div>
                ) : (
                    <>
                        {/* ===== 주문 탭 ===== */}
                        {activeTab === 'ORDER' && (
                            <>

                                {/* ===== 매도/매수 가능 수량 표시 ===== */}
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '12px' }}>
                                    {/* 매도 */}
                                    <span>
                <span style={{ color: '#AAA' }}>매도 가능 수량:</span>{' '}
                                        <span style={{ color: '#4F9DFF' }}>
                    {accountInfo?.stocks?.[stockCode]?.availableQuantity ?? 0}주
                </span>
                <br/>
                <span style={{ color: '#AAA' }}>매도 가능 금액:</span>{' '}
                                        <span style={{ color: '#4F9DFF' }}>
                    {Number(curPrice) > 0 && Number(accountInfo?.stocks?.[stockCode]?.availableQuantity ?? 0) > 0
                        ? (Number(accountInfo?.stocks[stockCode].availableQuantity) * Number(curPrice)).toLocaleString()
                        : '0'}원
                </span>
            </span>

                                    {/* 매수 */}
                                    <span style={{ textAlign: 'right' }}>
                <span style={{ color: '#AAA' }}>매수 가능 수량:</span>{' '}
                                        <span style={{ color: '#FF6347' }}>
                    {curPrice && accountInfo
                        ? Math.floor(accountInfo.availableCash / orderPrice)
                        : 0}주
                </span>
                <br/>
                <span style={{ color: '#AAA' }}>매수 가능 금액:</span>{' '}
                                        <span style={{ color: '#FF6347' }}>
                    {(accountInfo?.availableCash ?? 0).toLocaleString()}원
                </span>
            </span>
                                </div>
                                <div style={styles.divider} />

                                {/* 퍼센트 버튼 (매도 왼쪽, 매수 오른쪽) */}
                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', width: '100%' }}>
                                    {/* 매도 퍼센트 버튼 */}
                                    <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                                        <span style={{ color: '#AAA' }}>매도 비율:</span>
                                        {[10, 20, 50, 100].map(pct => (
                                            <button
                                                key={`sell-${pct}`}
                                                style={styles.percentButton}
                                                onClick={() => {
                                                    const available = accountInfo?.stocks?.[stockCode]?.availableQuantity ?? 0
                                                    setOrderQuantity(Math.max(1, Math.floor(available * pct / 100)))
                                                }}
                                            >
                                                {pct}%
                                            </button>
                                        ))}
                                    </div>

                                    {/* 매수 퍼센트 버튼 */}
                                    <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                                        <span style={{ color: '#AAA' }}>매수 비율:</span>
                                        {[10, 20, 50, 100].map(pct => (
                                            <button
                                                key={`buy-${pct}`}
                                                style={styles.percentButton}
                                                onClick={() => {
                                                    const maxBuy = curPrice && accountInfo
                                                        ? Math.floor(accountInfo.availableCash / orderPrice)
                                                        : 0
                                                    setOrderQuantity(Math.max(1, Math.floor(maxBuy * pct / 100)))
                                                }}
                                            >
                                                {pct}%
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                {/* 현재 주문 금액 */}
                                <div
                                    style={{
                                        fontSize: '14px',
                                        color: '#FFF',
                                        textAlign: 'center',
                                    }}
                                >
                                    현재 주문 금액: {orderAmount.toLocaleString()}원
                                </div>

                                {/* 수량 초과 경고 메시지 */}
                                <div style={{ color: '#FF5252', fontSize: '12px', marginTop: '4px', textAlign: 'center' }}>
                                    {quantityWarning}
                                </div>

                                {/* 주문 입력 */}
                                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                                    {/* 가격 입력 */}
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                        <span style={{ color: '#AAA', minWidth: '40px', textAlign: 'center' }}>주문가격</span>
                                        <button onClick={() => setOrderPrice(p => adjustPrice(p - getStep(p)))} style={styles.smallButton}>-</button>
                                        <input
                                            type="number"
                                            value={orderPrice}
                                            onChange={e => setOrderPrice(adjustPrice(Number(e.target.value)))}
                                            style={{ ...styles.input, textAlign: 'center', width: '80px' }}
                                            className="no-spinner"
                                        />
                                        <span style={{ fontSize: '12px', color: '#AAA', marginLeft: '4px' }}>원</span>
                                        <button onClick={() => setOrderPrice(p => adjustPrice(p + getStep(p)))} style={styles.smallButton}>+</button>
                                    </div>

                                    {/* 수량 입력 */}
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                        <span style={{ color: '#AAA', minWidth: '40px', textAlign: 'center' }}>수량</span>
                                        <button onClick={() => setOrderQuantity(q => Math.max(1, q - 1))} style={styles.smallButton}>-</button>
                                        <input
                                            type="number"
                                            value={orderQuantity}
                                            onChange={e => setOrderQuantity(Math.max(1, Number(e.target.value)))}
                                            style={{ ...styles.input, textAlign: 'center', width: '80px' }}
                                            className="no-spinner"
                                        />
                                        <span style={{ fontSize: '12px', color: '#AAA', marginLeft: '4px' }}>주</span>
                                        <button onClick={() => setOrderQuantity(q => q + 1)} style={styles.smallButton}>+</button>
                                    </div>

                                    {/* 매도/매수 버튼 */}
                                    <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '8px' }}>
                                        <button onClick={() => handleOrderClick('SELL')} style={styles.button}>매도</button>
                                        <button onClick={() => handleOrderClick('BUY')} style={styles.button}>매수</button>
                                    </div>
                                </div>

                                <div style={styles.divider} />

                                {/* 주문 목록 */}
                                <div className="ordersSection" style={styles.ordersSection}>
                                    <h4 style={{ color: '#AAA', marginBottom: '8px' }}>실시간 주문 목록</h4>

                                    {sortedOrders.length === 0 ? (
                                        <div style={styles.emptyCenter}>주문 내역 없음</div>
                                    ) : (
                                        sortedOrders.map(o => {
                                            const filled = o.orderQuantity - o.remainingQuantity
                                            const fillRate = Math.floor((filled / o.orderQuantity) * 100)
                                            const status =
                                                o.remainingQuantity === o.orderQuantity
                                                    ? '대기'
                                                    : o.remainingQuantity === 0
                                                        ? '체결 완료'
                                                        : '부분 체결'
                                            const time = new Date(o.orderTime)
                                            const timeText = `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}:${time.getSeconds().toString().padStart(2, '0')}`

                                            return (
                                                <div key={o.orderId} style={styles.orderCard}>
                                                    <div style={styles.orderHeader}>
                                <span style={{ justifySelf: 'start' }}>
                                    {stockNameMap[o.stockCode] ?? ''}
                                    <span style={{ color: '#777', marginLeft: '4px' }}>({o.stockCode})</span>
                                </span>
                                                        <span style={{ justifySelf: 'center', color: o.orderType === 'BUY' ? '#FF5252' : '#4F9DFF' }}>
                                    {o.orderType === 'BUY' ? '매수' : '매도'}
                                </span>
                                                        <span style={{ justifySelf: 'end', color: '#777' }}>{timeText}</span>
                                                    </div>

                                                    <div style={styles.orderRow}>
                                                        <span style={{ justifySelf: 'start' }}>주문가 <strong>{o.orderPrice.toLocaleString()}원</strong></span>
                                                        <span style={{ justifySelf: 'end' }}>주문 {o.orderQuantity}주</span>
                                                    </div>

                                                    <div style={styles.orderRowSmall}>
                                                        <span style={{ justifySelf: 'start' }}>체결 {filled}주</span>
                                                        <span style={{ justifySelf: 'center' }}>잔량 {o.remainingQuantity}주</span>
                                                        <span style={{ justifySelf: 'end' }}>{status}</span>
                                                    </div>

                                                    {o.remainingQuantity > 0 && (
                                                        <div style={styles.cancelButtonWrapper}>
                                                            <button
                                                                onClick={() => openCancelModal(o.orderId, o.stockCode)}
                                                                style={styles.cancelButton}
                                                            >
                                                                주문 취소
                                                            </button>
                                                        </div>
                                                    )}

                                                    <div style={styles.progressBar}>
                                                        <div
                                                            style={{
                                                                width: `${fillRate}%`,
                                                                height: '100%',
                                                                backgroundColor: o.orderType === 'BUY' ? '#FF5252' : '#4F9DFF',
                                                                transition: 'width 0.5s ease-in-out',
                                                            }}
                                                        />
                                                    </div>
                                                </div>
                                            )
                                        })
                                    )}
                                </div>
                            </>
                        )}
                    </>
                ))}
        </div>
    )
}


const styles = {
    wrapper: {
        padding: '16px',
        backgroundColor: '#1A1A1A',
        borderRadius: '8px',
        width: '100%',        // 부모 flex 기준으로 늘어나도록
        minWidth: 0,          // lex-shrink가 제대로 동작
        height: '913px',      // 필요하면 유지
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
    },
    toastContainer: {
        position: 'sticky' as const,
        top: 0,
        zIndex: 10,
        minHeight: '44px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
    },
    toast: {
        width: '100%',
        padding: '10px 14px',
        borderRadius: '6px',
        fontSize: '14px',
        fontWeight: 500,
        textAlign: 'center' as const,
        backgroundColor: '#2A2A2A', // 기본 배경
        color: '#DDD',
        boxShadow: '0 2px 6px rgba(0,0,0,0.4)',
    },
    clock: {
        color: '#777',
        fontSize: '13px',
    },
    loading: {
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#777',
    },
    tabContainer: {
        display: 'flex',
        gap: '12px',
        borderBottom: '1px solid #333',
    },
    tab: {
        padding: '6px 12px',
        background: 'none',
        border: 'none',
        color: '#888',
        cursor: 'pointer',
    },
    activeTab: {
        color: '#FFF',
        borderBottom: '2px solid #4CAF50',
        fontWeight: 600,
    },
    row: {
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    },
    inputGroup: {
        display: 'flex',
        alignItems: 'center',
        gap: '4px',
    },
    input: {
        width: '80px',
        textAlign: 'center' as const,
        padding: '4px',
        borderRadius: '4px',
        border: '1px solid #555',
        backgroundColor: '#222',
        color: '#FFF',
    },
    button: {
        backgroundColor: '#333',
        color: '#FFF',
        padding: '6px 12px',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
    },
    smallButton: {
        backgroundColor: '#444',
        color: '#FFF',
        padding: '4px 8px',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
    },
    stockHeader: {
        display: 'flex',
        alignItems: 'baseline',
        gap: '6px',
    },
    stockCode: {
        color: '#888',
        fontSize: '13px',
    },
    stockName: {
        color: '#FFF',
        fontSize: '16px',
        fontWeight: 600,
    },
    ordersSection: {
        flex: 1,
        overflowY: 'auto' as const,
    },
    orderCard: {
        backgroundColor: '#222',
        border: '1px solid #333',
        borderRadius: '8px',
        padding: '10px 12px',
        marginBottom: '8px',
        fontSize: '13px',
    },
    orderHeader: {
        display: 'grid',
        gridTemplateColumns: '1fr auto 1fr',
        alignItems: 'center',
        marginBottom: '6px',
        color: '#DDD',
        fontWeight: 600,
    },
    orderRow: {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        alignItems: 'center',
        color: '#DDD',
        marginBottom: '4px',
    },
    orderRowSmall: {
        display: 'grid',
        gridTemplateColumns: '1fr auto 1fr',
        alignItems: 'center',
        fontSize: '12px',
        color: '#AAA',
        marginBottom: '6px',
    },
    cancelButton: {
        padding: '2px 6px',
        fontSize: '11px',
        backgroundColor: '#552222',
        color: '#FF8A80',
        border: '1px solid #773333',
        borderRadius: '4px',
        cursor: 'pointer',
        marginBottom: '6px',
    },
    cancelButtonWrapper: {
        display: 'flex',
        justifyContent: 'center',
        marginTop: '6px',
    },
    progressBar: {
        height: '4px',
        backgroundColor: '#333',
        borderRadius: '2px',
        overflow: 'hidden',
    },
    loadingCenter: {
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#777',
        fontSize: '14px',
    },
    accountSummaryGrid: {
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
    },

    divider: {
        height: '1px',
        backgroundColor: '#333',
        margin: '8px 0',
    },
    emptyCenter: {
        flex: 1,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#666',
        fontSize: '13px',
    },
    // 모달 기본 오버레이
    modalOverlay: {
        position: 'fixed',
        top: 0, left: 0, right: 0, bottom: 0,
        backgroundColor: 'rgba(0,0,0,0.5)',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 9999,
    },

    // 모달 내용
    modalContent: {
        backgroundColor: '#1e1e1e',
        color: '#fff',
        padding: '20px',
        borderRadius: '8px',
        minWidth: '320px',
        maxWidth: '400px',
        textAlign: 'center', // 글씨 가운데 정렬
    },

    // 테이블 라벨 (왼쪽) -> 가운데 정렬
    modalLabel: {
        fontWeight: '600',
        color: '#ccc',
        textAlign: 'center',
        marginBottom: '4px',
        display: 'block',
    },

    // 모달 버튼
    modalButton: {
        padding: '6px 12px',
        backgroundColor: '#333',
        color: '#fff',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
        margin: '4px',
    },
    modalButtonContainer: {
        display: 'flex',
        justifyContent: 'center',
        gap: '12px', // 버튼 사이 간격
        marginTop: '12px',
    },
    percentButton: {
        padding: '2px 6px',
        fontSize: '12px',
        borderRadius: '4px',
        border: '1px solid #555',
        backgroundColor: '#222',
        color: '#FFF',
        cursor: 'pointer',
        transition: 'all 0.2s ease-in-out',
    },
} as const
