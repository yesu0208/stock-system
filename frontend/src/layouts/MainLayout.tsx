import type { ReactNode } from 'react'
import { tokenStorage } from '../utils/token'
import { useEffect, useState } from 'react'
import { logout } from '../api/auth'
import { disconnectStomp } from '../api/stompClient'
import Header from '../main/components/Header'
import LogoutResultModal from '../main/components/LogoutResultModal'
import { useMarketData } from '../main/context/MarketDataContext'
import IndexModal from '../main/components/IndexModal'
import HotStocksModal from '../main/components/HotStocksModal'
import SectorModal from '../main/components/SectorModal'
import InvestorModal from '../main/components/InvestorModal'
import styles from './MainLayout.module.css'

interface Props {
    children: ReactNode
    onLoggedOut: () => void
}

type LogoutReason = 'manual' | 'expired' | null

export default function MainLayout({ children, onLoggedOut }: Props) {
    const [logoutReason, setLogoutReason] = useState<LogoutReason>(null)
    const [showIndexModal, setShowIndexModal] = useState(false)
    const [showHotStocksModal, setShowHotStocksModal] = useState(false)
    const [showSectorModal, setShowSectorModal] = useState(false)
    const [showInvestorModal, setShowInvestorModal] = useState(false)
    const { marketMain } = useMarketData()

    const handleLogout = async () => {
        try {
            await logout()
        } catch (error) {
            console.warn('서버 로그아웃 실패:', error)
        }

        setLogoutReason('manual')
        tokenStorage.clear()
        disconnectStomp()
    }

    useEffect(() => {
        const unsubscribe = tokenStorage.subscribe(token => {
            if (!token) {
                setLogoutReason(prev => prev ?? 'expired')
            }
        })
        return () => unsubscribe()
    }, [])

    const handleModalClose = () => {
        setLogoutReason(null)
        setTimeout(() => onLoggedOut(), 0)
    }

    return (
        <div className={styles.container}>
            <Header onLogout={handleLogout} />

            <main className={styles.main}>{children}</main>

            <footer className={styles.footer}>
                <div className={styles.footerWidgets}>
                    {marketMain && (
                        <span className={styles.tickerText}>
                            KOSPI {marketMain.kospi.currentIndex} ({marketMain.kospi.changeRate}) ·{' '}
                            KOSDAQ {marketMain.kosdaq.currentIndex} ({marketMain.kosdaq.changeRate})
                        </span>
                    )}
                    <div style={{ display: 'flex', gap: '6px' }}>
                        <button className={styles.widgetButton} onClick={() => setShowIndexModal(true)}>지수</button>
                        <button className={styles.widgetButton} onClick={() => setShowHotStocksModal(true)}>인기종목</button>
                        <button className={styles.widgetButton} onClick={() => setShowSectorModal(true)}>업종</button>
                        <button className={styles.widgetButton} onClick={() => setShowInvestorModal(true)}>투자자동향</button>
                    </div>
                </div>
                <p>© 2026 Arile. All rights reserved.</p>
            </footer>

            <LogoutResultModal reason={logoutReason} onConfirm={handleModalClose} />

            <IndexModal show={showIndexModal} onClose={() => setShowIndexModal(false)} />
            <HotStocksModal show={showHotStocksModal} onClose={() => setShowHotStocksModal(false)} />
            <SectorModal show={showSectorModal} onClose={() => setShowSectorModal(false)} />
            <InvestorModal show={showInvestorModal} onClose={() => setShowInvestorModal(false)} />
        </div>
    )
}
