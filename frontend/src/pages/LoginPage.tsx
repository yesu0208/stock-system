import { useState, useEffect } from 'react'
import AuthLayout from '../layouts/AuthLayout'
import { tokenStorage } from '../utils/token'
import { login } from '../api/auth'
import Modal from '../components/Modal'
import { motion, AnimatePresence } from 'framer-motion'
import styles from './LoginPage.module.css'

interface Props {
    onLoginSuccess: () => void
    onNavigateToSignup: () => void
}

export default function LoginPage({ onLoginSuccess, onNavigateToSignup }: Props) {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [errorModal, setErrorModal] = useState(false)

    // 자동 로그인: 기존 토큰 있으면 바로 메인 화면
    // 여기 있던 connectStomp() 호출 제거.
    // 원래 /topic/stock-updates, /user/queue/orders 같은 실제로는
    // 어디서도 쓰이지 않는 디버그용 토픽을 구독하던 죽은 코드였고,
    // 지금은 App.tsx에서 로그인 상태가 되면 RealtimeProvider가
    // STOMP 연결을 알아서 소유함.
    useEffect(() => {
        const token = tokenStorage.get()
        if (token) {
            onLoginSuccess()
        }
    }, [])

    const handleLogin = async () => {
        if (!username || !password) return setErrorModal(true)
        try {
            await login({ username, password })
            onLoginSuccess()
        } catch (error) {
            console.error('Login failed:', error)
            setErrorModal(true)
        }
    }

    return (
        <AuthLayout>
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3 }}
                className={styles.card}
            >
                <h2 className={styles.welcome}>환영합니다!</h2>
                <input className={styles.input} placeholder="아이디" value={username} onChange={e => setUsername(e.target.value)} />
                <input className={styles.input} type="password" placeholder="비밀번호" value={password} onChange={e => setPassword(e.target.value)} />
                <button className={styles.button} onClick={handleLogin}>로그인</button>
                <p className={styles.signupText}>
                    계정이 없으신가요?{' '}
                    <button className={styles.signupLink} onClick={onNavigateToSignup}>회원가입</button>
                </p>

                <AnimatePresence>
                    {errorModal && (
                        <Modal show={true} onClose={() => setErrorModal(false)}>
                            <motion.div
                                initial={{ scale: 0.8, opacity: 0 }}
                                animate={{ scale: 1, opacity: 1 }}
                                exit={{ scale: 0.8, opacity: 0 }}
                                transition={{ duration: 0.2 }}
                                className={styles.modalContent}
                            >
                                <h3>로그인 실패</h3>
                                <p className={styles.modalMessage}>아이디 또는 비밀번호가 올바르지 않습니다.</p>
                                <div className={styles.modalActions}>
                                    <button className={styles.modalButton} onClick={() => setErrorModal(false)}>확인</button>
                                </div>
                            </motion.div>
                        </Modal>
                    )}
                </AnimatePresence>
            </motion.div>
        </AuthLayout>
    )
}
