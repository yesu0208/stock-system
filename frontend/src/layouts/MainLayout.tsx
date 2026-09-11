import type { ReactNode } from 'react'
import { tokenStorage } from '../utils/token'
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { logout } from '../api/auth'
import { disconnectStomp } from '../api/stompClient'
import styles from './MainLayout.module.css'

interface Props {
    children: ReactNode
    onLoggedOut: () => void
}

type LogoutReason = 'manual' | 'expired' | null

export default function MainLayout({ children, onLoggedOut }: Props) {
    const [logoutReason, setLogoutReason] = useState<LogoutReason>(null)

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

    // 다른 탭 로그아웃 or refresh 실패 감지
    useEffect(() => {
        const unsubscribe = tokenStorage.subscribe(token => {
            if (!token) {
                setLogoutReason(prev => prev ?? 'expired')
            }
        })
        return () => unsubscribe()
    }, [])

    // [5단계 변경] navigate('/login', { replace: true }) → onLoggedOut()
    const handleModalClose = () => {
        setLogoutReason(null)
        setTimeout(() => onLoggedOut(), 0)
    }

    return (
        <div className={styles.container}>
            <header className={styles.header}>
                <h1 className={styles.title}>모의투자 서비스</h1>
                <button className={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
            </header>

            <main className={styles.main}>{children}</main>

            <footer className={styles.footer}>
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
        </div>
    )
}
