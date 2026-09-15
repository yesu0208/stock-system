import { useEffect, useState } from 'react'
import OrderBook from '../components/orderbook/OrderBook'
import TradePanel from '../components/trade/TradePanel'
import AccountInfoPanel from '../components/information/AccountInfoPanel.tsx'
import type { StockSummaryTickMessage } from '../types/stockSummary'
import type { OtocoResponseMessage, OtocoResultResponse, OtocoCancelResultResponse } from '../types/otoco'
import type { TrailingStopResponseMessage, TrailingStopResultResponse, TrailingStopCancelResultResponse } from '../types/trailingStop'
import StockSummaryPanel from '../components/information/StockSummaryPanel.tsx'
import StockInfoPanel from '../components/information/StockInfoPanel'
import TradingChart from '../main/components/TradingChart'
import { useStock } from '../main/context/StockContext'
import { useUser } from '../main/context/UserContext'
import { useStockRealtime } from '../main/context/StockRealtimeContext'
import { motion } from 'framer-motion'
import styles from './TradePage.module.css'
import TopBar from '../main/components/TopBar'
import TradeBtnPanel from '../main/components/TradeBtnPanel'

import { useRealtime } from '../main/context/RealtimeContext'
import { useAccount } from '../main/context/AccountContext'

import type {
    OrderResultResponse,
    OrderResponseMessage,
} from '../types/order'

import type {
    AutoOrderResultResponse,
    AutoOrderResponseMessage,
} from '../types/autoOrder'

import type { AutoCancelResultResponse } from '../types/autoCancel'
import type { CancelResultResponse } from '../types/cancel'
import type { TradeResponse } from '../types/trade'
import { STOCKS } from '../constants/stocks'

export default function TradePage() {
    // 종목 선택은 TopBar와 동일한 전역 StockContext를 단일 소스로 사용
    const { selectedStock } = useStock()

    const [stockSummaries, setStockSummaries] =
        useState<StockSummaryTickMessage[]>([])

    // 주문 / 취소 / 체결 결과
    const [orderResult, setOrderResult] =
        useState<OrderResultResponse | null>(null)

    const [cancelResult, setCancelResult] =
        useState<CancelResultResponse | null>(null)

    const [tradeResult, setTradeResult] =
        useState<TradeResponse | null>(null)

    const [orders, setOrders] = useState<OrderResponseMessage[]>([])

    const [autoOrderResult, setAutoOrderResult] =
        useState<AutoOrderResultResponse | null>(null)

    const [autoCancelResult, setAutoCancelResult] = useState<AutoCancelResultResponse | null>(null);

    const [autoOrders, setAutoOrders] =
        useState<AutoOrderResponseMessage[]>([])

    const [otocoOrders, setOtocoOrders] = useState<OtocoResponseMessage[]>([])
    const [otocoResult, setOtocoResult] = useState<OtocoResultResponse | null>(null)
    const [otocoCancelResult, setOtocoCancelResult] = useState<OtocoCancelResultResponse | null>(null)

    const [trailingStops, setTrailingStops] = useState<TrailingStopResponseMessage[]>([])
    const [trailingStopResult, setTrailingStopResult] = useState<TrailingStopResultResponse | null>(null)
    const [trailingStopCancelResult, setTrailingStopCancelResult] = useState<TrailingStopCancelResultResponse | null>(null)

    const { subscribeDestination } = useRealtime()
    const { account: accountInfo } = useAccount()
    const { user: userInfo } = useUser()

    // OrderBook의 실시간 호가/체결가는 StockRealtimeContext가 selectedStock 기준으로 자체 구독하므로
    // 여기서는 TradePanel에 넘길 현재가/연결상태만 가져온다.
    const { priceTick, connected: isPriceConnected } = useStockRealtime()

    useEffect(() => {
        const unsubOrderResult = subscribeDestination('/user/sub/order/result', (data: OrderResultResponse) => {
            setOrderResult(data)
        })

        const unsubCancelResult = subscribeDestination('/user/sub/cancel', (data: CancelResultResponse) => {
            setCancelResult(data)
        })

        const unsubTradeResult = subscribeDestination('/user/sub/trade', (data: TradeResponse) => {
            setTradeResult(data)
        })

        const unsubOrderList = subscribeDestination('/user/sub/order', (data: OrderResponseMessage[]) => {
            setOrders(data)
        })

        const unsubAutoOrderResult = subscribeDestination('/user/sub/auto/order/result', (data: AutoOrderResultResponse) => {
            setAutoOrderResult(data)
        })

        const unsubAutoOrderList = subscribeDestination('/user/sub/auto/order', (data: AutoOrderResponseMessage[]) => {
            setAutoOrders(data)
        })

        const unsubSummary = subscribeDestination('/sub/stock/summary', (data: StockSummaryTickMessage[]) => {
            setStockSummaries(data)
        })

        const unsubAutoCancelResult = subscribeDestination('/user/sub/auto/cancel', (data: AutoCancelResultResponse) => {
            setAutoCancelResult(data)
        })

        const unsubOtocoResult = subscribeDestination('/user/sub/otoco/result', (data: OtocoResultResponse) => {
            setOtocoResult(data)
        })

        const unsubOtocoList = subscribeDestination('/user/sub/otoco', (data: OtocoResponseMessage[]) => {
            setOtocoOrders(data)
        })

        const unsubOtocoCancelResult = subscribeDestination('/user/sub/otoco/cancel', (data: OtocoCancelResultResponse) => {
            setOtocoCancelResult(data)
        })

        const unsubTrailingResult = subscribeDestination('/user/sub/trailing-stop/result', (data: TrailingStopResultResponse) => {
            setTrailingStopResult(data)
        })

        const unsubTrailingList = subscribeDestination('/user/sub/trailing-stop', (data: TrailingStopResponseMessage[]) => {
            setTrailingStops(data)
        })

        const unsubTrailingUpdate = subscribeDestination('/user/sub/trailing-stop/update', (data: TrailingStopResponseMessage) => {
            setTrailingStops(prev =>
                prev.map(t => (t.trailingStopId === data.trailingStopId ? data : t))
            )
        })

        const unsubTrailingCancelResult = subscribeDestination('/user/sub/trailing-stop/cancel', (data: TrailingStopCancelResultResponse) => {
            setTrailingStopCancelResult(data)
        })

        return () => {
            unsubOrderResult()
            unsubCancelResult()
            unsubTradeResult()
            unsubOrderList()
            unsubAutoOrderResult()
            unsubAutoOrderList()
            unsubSummary()
            unsubAutoCancelResult()
            unsubAutoCancelResult()
            unsubOtocoResult()
            unsubOtocoList()
            unsubOtocoCancelResult()
            unsubTrailingResult()
            unsubTrailingList()
            unsubTrailingUpdate()
            unsubTrailingCancelResult()
        }
    }, [subscribeDestination])

    const stockCode = selectedStock.code
    const stockName = selectedStock.name

    return (
        <>
            <TopBar /> {/* 임시 — 화면 확인용 */}

            <motion.div
                className={styles.tradeContainer}
                initial={{opacity: 0}}
                animate={{opacity: 1}}
                transition={{duration: 0.6, ease: 'easeInOut'}}
            >
                <motion.div
                    className={styles.orderBookContainer}
                    initial={{opacity: 0, y: 20}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 0.5, delay: 0.1}}
                >
                    <OrderBook />
                    <TradeBtnPanel />
                </motion.div>

                <motion.div
                    style={{ display: 'flex', flexDirection: 'column', gap: '12px', width: '480px' }}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.15 }}
                >
                    <StockInfoPanel />
                    <div style={{ height: '420px' }}>
                        <TradingChart />
                    </div>
                </motion.div>

                <motion.div
                    style={{ flex: 1, minWidth: 0 }}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.2 }}
                >
                    <TradePanel
                        stockCode={stockCode}
                        stockName={stockName}
                        isPriceReady={isPriceConnected && !!priceTick}
                        curPrice={priceTick?.curPrice}
                        orderResult={orderResult}
                        cancelResult={cancelResult}
                        tradeResult={tradeResult}
                        orders={orders}
                        autoOrders={autoOrders}
                        autoOrderResult={autoOrderResult}
                        autoCancelResult={autoCancelResult}
                        accountInfo={accountInfo}
                        otocoOrders={otocoOrders}
                        otocoResult={otocoResult}
                        otocoCancelResult={otocoCancelResult}
                        trailingStops={trailingStops}
                        trailingStopResult={trailingStopResult}
                        trailingStopCancelResult={trailingStopCancelResult}
                    />
                </motion.div>

                <motion.div
                    className={styles.infoColumn}
                    initial={{opacity: 0, y: 20}}
                    animate={{opacity: 1, y: 0}}
                    transition={{duration: 0.5, delay: 0.3}}
                >
                    <AccountInfoPanel
                        account={accountInfo}
                        user={userInfo}
                    />

                    <StockSummaryPanel
                        summaries={stockSummaries.map(s => ({
                            ...s,
                            stockName: STOCKS.find(st => st.code === s.stockCode)?.name ?? s.stockCode
                        }))}
                    />
                </motion.div>
            </motion.div>
        </>
    )
}
