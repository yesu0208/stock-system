import OrderBook from '../components/orderbook/OrderBook'
import StockInfoPanel from '../components/information/StockInfoPanel'
import TradingChart from '../main/components/TradingChart'
import { motion } from 'framer-motion'
import styles from './TradePage.module.css'
import TopBar from '../main/components/TopBar'
import TradeBtnPanel from '../main/components/TradeBtnPanel'
import OrderPanel from '../main/components/OrderPanel'

export default function TradePage() {

    return (

        <div className={styles.page}>
            <TopBar />

            <motion.div
                className={styles.tradeContent}
                initial={{opacity: 0}}
                animate={{opacity: 1}}
                transition={{duration: 0.6, ease: 'easeInOut'}}
            >
                <div className={styles.tradeGrid}>

                    <div className={styles.panelOrderbook}>
                        <OrderBook />
                    </div>

                    <div className={styles.panelCenter}>
                        <div className={styles.panelChart}>
                            <TradingChart />
                        </div>
                        <div className={styles.panelStockinfo}>
                            <StockInfoPanel />
                        </div>
                    </div>

                    <div className={styles.panelRight}>
                        <div className={styles.panelOrder}>
                            <OrderPanel />
                        </div>
                        <div className={styles.panelTradeBtn}>
                            <TradeBtnPanel />
                        </div>
                    </div>

                </div>
            </motion.div>
        </div>
    )
}
