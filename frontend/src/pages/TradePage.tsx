import { useEffect, useState } from 'react'
import OrderBook from '../components/orderbook/OrderBook'
import AccountInfoPanel from '../components/information/AccountInfoPanel.tsx'
import type { StockSummaryTickMessage } from '../types/stockSummary'
import StockSummaryPanel from '../components/information/StockSummaryPanel.tsx'
import StockInfoPanel from '../components/information/StockInfoPanel'
import TradingChart from '../main/components/TradingChart'
import { useUser } from '../main/context/UserContext'
import { motion } from 'framer-motion'
import styles from './TradePage.module.css'
import TopBar from '../main/components/TopBar'
import TradeBtnPanel from '../main/components/TradeBtnPanel'
import OrderPanel from '../main/components/OrderPanel'

import { useRealtime } from '../main/context/RealtimeContext'
import { useAccount } from '../main/context/AccountContext'

import { STOCKS } from '../constants/stocks'

export default function TradePage() {

    const [stockSummaries, setStockSummaries] =
        useState<StockSummaryTickMessage[]>([])

    const { subscribeDestination } = useRealtime()
    const { account: accountInfo } = useAccount()
    const { user: userInfo } = useUser()

    useEffect(() => {
        const unsubSummary = subscribeDestination('/sub/stock/summary', (data: StockSummaryTickMessage[]) => {
            setStockSummaries(data)
        })

        return () => {
            unsubSummary()
        }
    }, [subscribeDestination])

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
                    <OrderPanel />
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
