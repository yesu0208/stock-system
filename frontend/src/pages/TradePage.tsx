import { useEffect, useState } from 'react'
import MainLayout from '../layouts/MainLayout'
import OrderBook from '../components/orderbook/OrderBook'
import TradePanel from '../components/trade/TradePanel'
import AccountInfoPanel from '../components/information/AccountInfoPanel.tsx'
import type { StockSummaryTickMessage } from '../types/stockSummary'
import StockSummaryPanel from '../components/information/StockSummaryPanel.tsx'
import { motion } from 'framer-motion'
import instance from '../api/axios'
import type { StompSubscription } from '@stomp/stompjs'
import { tokenStorage } from '../utils/token'

import type { TradePriceTickMessage } from '../types/tradePriceTickMessage'
import { getStockClient } from '../api/stompClient'

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
import type { AccountResponse } from '../types/account'
import { STOCKS } from '../constants/stocks'
import type {UserDto} from "../types/user.ts";

interface PriceLevel {
    price: number
    quantity: number
}

export default function TradePage() {
    const [selectedStock, setSelectedStock] = useState(STOCKS[0].code)

    const [asks, setAsks] = useState<PriceLevel[]>([])
    const [bids, setBids] = useState<PriceLevel[]>([])

    const [tradeTicks, setTradeTicks] = useState<TradePriceTickMessage[]>([])
    const [tradePrice, setTradePrice] =
        useState<TradePriceTickMessage | null>(null)

    const [prevClosePrice, setPrevClosePrice] = useState(0)

    const [isBidAskReady, setIsBidAskReady] = useState(false)
    const [isTradeReady, setIsTradeReady] = useState(false)

    const [userInfo, setUserInfo] = useState<UserDto | null>(null)

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

    // 계좌 정보
    const [accountInfo, setAccountInfo] =
        useState<AccountResponse | null>(null)

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const res = await instance.get<UserDto>('/users/user')
                setUserInfo(res.data)
            } catch (err) {
                console.error('유저 조회 실패:', err)
            }
        }

        fetchUser()
    }, [])

    useEffect(() => {
        const token = tokenStorage.get()
        if (!token) return // 로그인 안된 상태면 STOMP 연결 금지

        const client = getStockClient()

        let stockSub: StompSubscription | undefined
        let orderResultSub: StompSubscription | undefined
        let orderListSub: StompSubscription | undefined
        let cancelResultSub: StompSubscription | undefined
        let tradeResultSub: StompSubscription | undefined
        let autoOrderResultSub: StompSubscription | undefined
        let autoOrderListSub: StompSubscription | undefined
        let accountSub: StompSubscription | undefined
        let summarySub: StompSubscription | undefined
        let autoCancelResultSub: StompSubscription | undefined

        client.onConnect = () => {

            // 시세 / 호가
            stockSub = client.subscribe(
                `/sub/stock/${selectedStock}`,
                (msg) => {
                    const data = JSON.parse(msg.body)

                    if (data.tickMessageType === 'BIDASKPRICE') {
                        setAsks(data.asks ?? [])
                        setBids(data.bids ?? [])
                        setIsBidAskReady(true)
                    }

                    if (data.tickMessageType === 'TRADEPRICE') {
                        const tick = data as TradePriceTickMessage
                        setPrevClosePrice(tick.prevClosePrice ?? 0)
                        setTradePrice(tick)
                        setTradeTicks(prev => [...prev, tick])
                        setIsTradeReady(true)
                    }
                }
            )

            // 주문 결과
            orderResultSub = client.subscribe(
                '/user/sub/order/result',
                (msg) => {
                    const data: OrderResultResponse = JSON.parse(msg.body)
                    setOrderResult(data)
                }
            )

            // 주문 취소
            cancelResultSub = client.subscribe(
                '/user/sub/cancel',
                (msg) => {
                    const data: CancelResultResponse = JSON.parse(msg.body)
                    setCancelResult(data)
                }
            )

            // 체결 결과
            tradeResultSub = client.subscribe(
                '/user/sub/trade',
                (msg) => {
                    const data: TradeResponse = JSON.parse(msg.body)
                    setTradeResult(data)
                }
            )

            // 주문 목록
            orderListSub = client.subscribe(
                '/user/sub/order',
                (msg) => {
                    const data: OrderResponseMessage[] = JSON.parse(msg.body)
                    setOrders(data)
                }
            )

            // 자동주문 결과
            autoOrderResultSub = client.subscribe(
                '/user/sub/auto/order/result',
                (msg) => {
                    const data: AutoOrderResultResponse = JSON.parse(msg.body)
                    setAutoOrderResult(data)
                }
            )

            // 자동주문 목록
            autoOrderListSub = client.subscribe(
                '/user/sub/auto/order',
                (msg) => {
                    const data: AutoOrderResponseMessage[] = JSON.parse(msg.body)
                    setAutoOrders(data)
                }
            )

            // 계좌 정보
            accountSub = client.subscribe(
                '/user/sub/account',
                (msg) => {
                    const data: AccountResponse = JSON.parse(msg.body)
                    setAccountInfo(data)
                }
            )

            summarySub = client.subscribe(
                '/sub/stock/summary',
                (msg) => {
                    const data: StockSummaryTickMessage[] =
                        JSON.parse(msg.body)

                    setStockSummaries(data)
                }
            )

            autoCancelResultSub = client.subscribe(
                '/user/sub/auto/cancel',
                (msg) => {
                    const data: AutoCancelResultResponse = JSON.parse(msg.body)
                    setAutoCancelResult(data)
                }
            )
        }

        client.activate()

        return () => {
            stockSub?.unsubscribe()
            orderResultSub?.unsubscribe()
            cancelResultSub?.unsubscribe()
            tradeResultSub?.unsubscribe()
            orderListSub?.unsubscribe()
            autoOrderResultSub?.unsubscribe()
            autoOrderListSub?.unsubscribe()
            accountSub?.unsubscribe()
            summarySub?.unsubscribe()
            autoCancelResultSub?.unsubscribe()
            client.deactivate()
        }
    }, [selectedStock])

    const stockName =
        STOCKS.find(s => s.code === selectedStock)?.name ?? selectedStock

    const isOrderBookReady = isBidAskReady && isTradeReady

    return (
        <MainLayout>
            <motion.div
                style={{ ...styles.tradeContainer }}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.6, ease: 'easeInOut' }}
            >
                {/* OrderBook */}
                <motion.div
                    style={styles.orderBookContainer}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.1 }}
                >
                    <div style={styles.stockSelector}>
                        <label style={styles.label}>종목 선택:</label>

                        <select
                            style={styles.select}
                            value={selectedStock}
                            onChange={(e) => {
                                setSelectedStock(e.target.value)
                                setAsks([])
                                setBids([])
                                setTradeTicks([])
                                setTradePrice(null)
                                setPrevClosePrice(0)
                                setIsBidAskReady(false)
                                setIsTradeReady(false)
                            }}
                        >
                            {STOCKS.map(stock => (
                                <option key={stock.code} value={stock.code}>
                                    {stock.name} ({stock.code})
                                </option>
                            ))}
                        </select>
                    </div>

                    <OrderBook
                        stockName={stockName}
                        asks={asks}
                        bids={bids}
                        tradeTicks={tradeTicks}
                        prevClosePrice={prevClosePrice}
                        isReady={isOrderBookReady}
                    />
                </motion.div>

                {/* TradePanel: flex: 1 적용 */}
                <motion.div
                    style={{ flex: 1, minWidth: 0 }}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.2 }}
                >
                    <TradePanel
                        stockCode={selectedStock}
                        stockName={stockName}
                        isPriceReady={isBidAskReady}
                        curPrice={tradePrice?.curPrice}
                        orderResult={orderResult}
                        cancelResult={cancelResult}
                        tradeResult={tradeResult}
                        orders={orders}
                        autoOrders={autoOrders}
                        autoOrderResult={autoOrderResult}
                        autoCancelResult={autoCancelResult}
                        accountInfo={accountInfo}
                    />
                </motion.div>

                {/* 계좌 + 요약 패널 */}
                <motion.div
                    style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, delay: 0.3 }}
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
        </MainLayout>
    )
}

const styles = {
    tradeContainer: {
        display: 'flex',
        gap: '20px',
        width: '100%',
        minHeight: '100vh',
        flexWrap: 'wrap',
    },
    orderBookContainer: {
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        width: '350px',
        height: '912px',
    },
    stockSelector: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px',
    },
    label: {
        fontSize: '14px',
        color: '#AAA',
    },
    select: {
        padding: '6px 12px',
        borderRadius: '6px',
        border: '1px solid #333',
        backgroundColor: '#1E1E1E',
        color: '#FFF',
    },
} as const
