import type { ReactNode } from 'react'
import { tokenStorage } from '../utils/token'
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { logout } from '../api/auth'
import { disconnectStomp } from '../api/stompClient'
import NoticeModal from '../main/components/NoticeModal'
import MyInfoModal from '../main/components/MyInfoModal'
import HelpModal from '../main/components/HelpModal'
import ManagedModal from '../main/components/ManagedModal'
import { useUser } from '../main/context/UserContext'
import RankBadge from '../main/components/RankBadge'
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
    const [showNoticeModal, setShowNoticeModal] = useState(false)
    const [showMyInfoModal, setShowMyInfoModal] = useState(false)
    const [showHelpModal, setShowHelpModal] = useState(false)
    const [showManagedModal, setShowManagedModal] = useState(false)
    const [showIndexModal, setShowIndexModal] = useState(false)
    const [showHotStocksModal, setShowHotStocksModal] = useState(false)
    const [showSectorModal, setShowSectorModal] = useState(false)
    const [showInvestorModal, setShowInvestorModal] = useState(false)
    const { user } = useUser()
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
            <header className={styles.header}>
                <h1 className={styles.title}>모의투자 서비스</h1>
                <button className={styles.userBadge} onClick={() => setShowMyInfoModal(true)}>
                    <span>{user?.nickname ?? '...'}</span>
                    <RankBadge rank={user?.rank ?? null} size={20} />
                </button>
                <div className={styles.headerActions}>
                    <button className={styles.noticeButton} onClick={() => setShowNoticeModal(true)}>공지사항</button>
                    <button className={styles.noticeButton} onClick={() => setShowHelpModal(true)}>도움말</button>
                    {user?.role === 'ADMIN' && (
                        <button className={styles.noticeButton} onClick={() => setShowManagedModal(true)}>관리자</button>
                    )}
                    <button className={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
                </div>
            </header>

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

            {logoutReason === 'manual' && (
                <Modal show={true} onClose={handleModalClose}>
                    <div className={styles.modalContent}>
                        <h3>로그아웃되었습니다.</h3>
                        <p>정상적으로 로그아웃 처리되었습니다.</p>
                        <button className={styles.modalButton} onClick={handleModalClose}>확인</button>
                    </div>
                </Modal>
            )}

            {logoutReason === 'expired' && (
                <Modal show={true} onClose={handleModalClose}>
                    <div className={styles.modalContent}>
                        <h3>세션이 만료되었습니다.</h3>
                        <p>다시 로그인해주세요.</p>
                        <button className={styles.modalButton} onClick={handleModalClose}>로그인 페이지로 이동</button>
                    </div>
                </Modal>
            )}

            <NoticeModal show={showNoticeModal} onClose={() => setShowNoticeModal(false)} />
            <MyInfoModal show={showMyInfoModal} onClose={() => setShowMyInfoModal(false)} />
            <HelpModal show={showHelpModal} onClose={() => setShowHelpModal(false)} />
            {user?.role === 'ADMIN' && (
                <ManagedModal show={showManagedModal} onClose={() => setShowManagedModal(false)} />
            )}
            <IndexModal show={showIndexModal} onClose={() => setShowIndexModal(false)} />
            <HotStocksModal show={showHotStocksModal} onClose={() => setShowHotStocksModal(false)} />
            <SectorModal show={showSectorModal} onClose={() => setShowSectorModal(false)} />
            <InvestorModal show={showInvestorModal} onClose={() => setShowInvestorModal(false)} />
        </div>
    )
}
