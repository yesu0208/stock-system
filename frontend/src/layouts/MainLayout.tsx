import type { ReactNode } from 'react'
import { tokenStorage } from '../utils/token'
import { useNavigate } from 'react-router-dom'
import { useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { logout } from '../api/auth'

interface Props { children: ReactNode }

type LogoutReason = 'manual' | 'expired' | null

export default function MainLayout({ children }: Props) {
    const navigate = useNavigate()
    const [logoutReason, setLogoutReason] = useState<LogoutReason>(null)

    const handleLogout = async () => {
        try {
            await logout()
        } catch (error) {
            console.warn('서버 로그아웃 실패:', error)
        }

        setLogoutReason('manual')
        tokenStorage.clear()
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

    const handleModalClose = () => {
        setLogoutReason(null)
        navigate('/login', { replace: true })
    }

    return (
        <div style={styles.container}>
            <header style={styles.header}>
                <h1 style={styles.title}>모의투자 서비스</h1>
                <button style={styles.logoutButton} onClick={handleLogout}>로그아웃</button>
            </header>

            <main style={styles.main}>{children}</main>

            <footer style={styles.footer}>
                <p>© 2026 Arile. All rights reserved.</p>
            </footer>

            {logoutReason === 'manual' && (
                <Modal show={true} onClose={handleModalClose}>
                    <div style={modalStyle}>
                        <h3>로그아웃되었습니다.</h3>
                        <p>정상적으로 로그아웃 처리되었습니다.</p>
                        <button style={buttonStyle} onClick={handleModalClose}>확인</button>
                    </div>
                </Modal>
            )}

            {logoutReason === 'expired' && (
                <Modal show={true} onClose={handleModalClose}>
                    <div style={modalStyle}>
                        <h3>세션이 만료되었습니다.</h3>
                        <p>다시 로그인해주세요.</p>
                        <button style={buttonStyle} onClick={handleModalClose}>로그인 페이지로 이동</button>
                    </div>
                </Modal>
            )}
        </div>
    )
}

const modalStyle = {
    textAlign: 'center' as const,
    color: '#FFF'
}
const buttonStyle = {
    marginTop: '16px',
    padding: '10px 20px',
    borderRadius: '6px',
    border: 'none',
    backgroundColor: '#4F9DFF',
    color: '#FFF',
    cursor: 'pointer'
}
const styles = {
    container: {
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column' as const,
        backgroundColor: '#121212',
        color: '#FFF'
    },
    header: {
        padding: '20px',
        textAlign: 'center' as const,
        borderBottom: '1px solid #333',
        position: 'relative' as const
    },
    title: {
        margin: 0,
        fontSize: '24px'
    },
    logoutButton: {
        position: 'absolute' as const,
        right: '20px',
        top: '50%',
        transform: 'translateY(-50%)',
        padding: '6px 14px',
        fontSize: '14px',
        borderRadius: '6px',
        border: 'none',
        cursor: 'pointer',
        backgroundColor: '#333333',
        color: '#FFF',
        fontWeight: 500
    },
    main: {
        flex: 1,
        display: 'flex',
        padding: '20px',
        gap: '20px'
    },
    footer: {
        padding: '12px',
        textAlign: 'center' as const,
        borderTop: '1px solid #333',
        fontSize: '12px',
        color: '#AAAAAA'
    },
}
