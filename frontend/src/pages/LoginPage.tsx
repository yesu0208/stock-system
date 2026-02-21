import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthLayout from '../layouts/AuthLayout'
import { tokenStorage } from '../utils/token'
import { login } from '../api/auth'
import { getStockClient, getOrderClient } from '../api/stompClient'
import Modal from '../components/Modal'
import { motion, AnimatePresence } from 'framer-motion'

export default function LoginPage() {
    const navigate = useNavigate()
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [errorModal, setErrorModal] = useState(false)

    // STOMP 연결 함수
    const connectStomp = () => {
        const stockClient = getStockClient()
        stockClient.onConnect = () => {
            console.log('STOCK connected')
            stockClient.subscribe('/topic/stock-updates', (msg) => {
                console.log('Stock update:', JSON.parse(msg.body))
            })
        }
        stockClient.activate()

        const orderClient = getOrderClient()
        orderClient.onConnect = () => {
            console.log('ORDER connected')
            orderClient.subscribe('/user/queue/orders', (msg) => {
                console.log('My order update:', JSON.parse(msg.body))
            })
        }
        orderClient.activate()
    }

    // 자동 로그인: 기존 토큰 있으면 바로 TradePage
    useEffect(() => {
        const token = tokenStorage.get()
        if (token) {
            connectStomp()
            navigate('/trade', { replace: true })
        }
    }, [])

    const handleLogin = async () => {
        if (!username || !password) return setErrorModal(true)
        try {
            await login({ username, password })
            connectStomp()
            navigate('/trade', { replace: true })
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
                style={styles.card}
            >
                <h2 style={styles.welcome}>환영합니다!</h2>
                <input style={styles.input} placeholder="아이디" value={username} onChange={e => setUsername(e.target.value)} />
                <input style={styles.input} type="password" placeholder="비밀번호" value={password} onChange={e => setPassword(e.target.value)} />
                <button style={styles.button} onClick={handleLogin}>로그인</button>
                <p style={styles.signupText}>
                    계정이 없으신가요? <Link to="/signup" style={styles.signupLink}>회원가입</Link>
                </p>

                <AnimatePresence>
                    {errorModal && (
                        <Modal show={true} onClose={() => setErrorModal(false)}>
                            <motion.div initial={{ scale: 0.8, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.8, opacity: 0 }} transition={{ duration: 0.2 }} style={{ textAlign: 'center' }}>
                                <h3>로그인 실패</h3>
                                <p style={{ fontSize: '14px', color: '#FFFFFF' }}>아이디 또는 비밀번호가 올바르지 않습니다.</p>
                                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '16px' }}>
                                    <button style={modalStyles.button} onClick={() => setErrorModal(false)}>확인</button>
                                </div>
                            </motion.div>
                        </Modal>
                    )}
                </AnimatePresence>
            </motion.div>
        </AuthLayout>
    )
}

const modalStyles = { button: { padding: '10px 20px', backgroundColor: '#4F9DFF', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer' } }
const styles = {
    card: { width: '360px', padding: '40px', borderRadius: '12px', backgroundColor: '#1E1E1E', display: 'flex', flexDirection: 'column' as const, gap: '16px' },
    welcome: { color: '#4F9DFF', textAlign: 'center' as const, marginBottom: '20px' },
    input: { padding: '12px', fontSize: '14px', borderRadius: '6px', border: '1px solid #333', backgroundColor: '#2A2A2A', color: '#FFF' },
    button: { padding: '12px', fontSize: '15px', borderRadius: '6px', border: 'none', cursor: 'pointer', backgroundColor: '#4F9DFF', color: '#FFF', fontWeight: 500 },
    signupText: { textAlign: 'center' as const, fontSize: '13px', color: '#AAA' },
    signupLink: { color: '#4F9DFF', textDecoration: 'none' },
}
