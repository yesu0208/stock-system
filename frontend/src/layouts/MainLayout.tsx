import type { ReactNode } from 'react'
import { tokenStorage } from '../utils/token'
import { useEffect, useState } from 'react'
import { logout } from '../api/auth'
import { disconnectStomp } from '../api/stompClient'
import Header from '../main/components/Header'
import LogoutResultModal from '../main/components/LogoutResultModal'
import UserPanel from '../main/components/UserPanel'
import Footer from '../main/components/Footer'
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

            <main className={styles.main}>
                <UserPanel />
                <div className={styles.mainContent}>
                    {children}
                </div>
            </main>

            <Footer />

            <LogoutResultModal reason={logoutReason} onConfirm={handleModalClose} />
        </div>
    )
}
