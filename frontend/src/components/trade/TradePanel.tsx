import {type ReactNode, useEffect, useRef, useState} from 'react'
import {stockNameMap} from '../../constants/stocks'
import type {
    OrderResultResponse,
    OrderResponseMessage,
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
                                       orderResult,
                                       cancelResult,
                                       tradeResult,
                                       autoOrderResult,
                                       autoCancelResult,
                                   }: Props) {
    const initializedRef = useRef(false)
    const triggerInitializedRef = useRef(false)

    const [toast, setToast] = useState<ToastPayload | null>(null)
    const [now, setNow] = useState(new Date())

    const [activeTab, setActiveTab] = useState<TabType>('ORDER')
    const toastTimerRef = useRef<number | null>(null)

    useEffect(() => {
        initializedRef.current = false
    }, [stockCode])

    useEffect(() => {
        initializedRef.current = false
        triggerInitializedRef.current = false
    }, [stockCode])

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

    return (
        <div style={styles.wrapper}>

            {/*  토스트 / 시계 */}
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
} as const
