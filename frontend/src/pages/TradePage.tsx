import { useEffect, useState } from 'react'
import OrderBook from '../components/orderbook/OrderBook'
import TradePanel from '../components/trade/TradePanel'
import AccountInfoPanel from '../components/information/AccountInfoPanel.tsx'
import type { StockSummaryTickMessage } from '../types/stockSummary'
import StockSummaryPanel from '../components/information/StockSummaryPanel.tsx'
import { motion } from 'framer-motion'
import styles from './TradePage.module.css'

import type { TradePriceTickMessage } from '../types/tradePriceTickMessage'
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

    /*
     * - userInfo는 UserContext 도입 이전까지 임시로 이 화면에서 직접
     *   조회하던 값이었는데, 이번 단계에서는 손대지 않고 그대로 둠.
     *   (아래 useEffect의 fetchUser 참고 — 원래 로직 그대로).
     * - accountInfo 로컬 state + '/user/sub/account' 구독은 제거하고
     *   AccountContext(useAccount)로 대체
     * - 나머지 구독(주문/취소/체결/자동주문/시세요약)은 아직 전용
     *   Context가 없으므로 이 화면에 남겨두되, 더 이상
     *   client.activate()/onConnect를 직접 부르지 않고
     *   RealtimeContext.subscribeDestination()을 통해서만 구독
     *   (STOMP 연결의 유일한 소유자는 이제 RealtimeProvider뿐)
     */
    const { subscribeDestination } = useRealtime()
    const { account: accountInfo } = useAccount()

    const [userInfo, setUserInfo] = useState<import('../types/user.ts').UserDto | null>(null)

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const instance = (await import('../api/axios')).default
                const res = await instance.get('/users/user')
                setUserInfo(res.data)
            } catch (err) {
                console.error('유저 조회 실패:', err)
            }
        }

        fetchUser()
    }, [])

    useEffect(() => {
        const unsubStock = subscribeDestination(`/sub/stock/${selectedStock}`, (data: any) => {
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
        })

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

        return () => {
            unsubStock()
            unsubOrderResult()
            unsubCancelResult()
            unsubTradeResult()
            unsubOrderList()
            unsubAutoOrderResult()
            unsubAutoOrderList()
            unsubSummary()
            unsubAutoCancelResult()
        }
    }, [selectedStock, subscribeDestination])

    const stockName =
        STOCKS.find(s => s.code === selectedStock)?.name ?? selectedStock

    const isOrderBookReady = isBidAskReady && isTradeReady

    return (
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
                <div className={styles.stockSelector}>
                    <label className={styles.label}>종목 선택:</label>

                    <select
                        className={styles.select}
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

            <motion.div
                className={styles.tradePanelWrapper}
                initial={{opacity: 0, y: 20}}
                animate={{opacity: 1, y: 0}}
                transition={{duration: 0.5, delay: 0.2}}
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
    )
}
