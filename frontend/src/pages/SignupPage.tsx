import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthLayout from '../layouts/AuthLayout'
import { signUp, checkUsernameAPI } from '../api/auth'
import Modal from '../components/Modal.tsx'
import { motion, AnimatePresence } from 'framer-motion'

export default function SignupPage() {
    const navigate = useNavigate()
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [passwordConfirm, setPasswordConfirm] = useState('')
    const [error, setError] = useState('')

    const [showModal, setShowModal] = useState(false)
    const [fadeOut, setFadeOut] = useState(false) // 페이지 fade-out 상태

    const openModal = () => setShowModal(true)

    const closeModal = () => {
        setShowModal(false)      // 모달 fade-out
        setFadeOut(true)         // 페이지 fade-out 시작
    }

    // 페이지 fade-out 후 이동
    useEffect(() => {
        if (fadeOut) {
            const timer = setTimeout(() => {
                navigate('/login')
            }, 300) // 페이지 transition duration과 동일
            return () => clearTimeout(timer)
        }
    }, [fadeOut])

    // --- 기존 유효성 체크 코드 ---
    const [usernameLength, setUsernameLength] = useState(false)
    const [usernameChars, setUsernameChars] = useState(false)
    const [usernameExists, setUsernameExists] = useState(false)
    const [passLength, setPassLength] = useState(false)
    const [passLower, setPassLower] = useState(false)
    const [passNumber, setPassNumber] = useState(false)
    const [passSpecial, setPassSpecial] = useState(false)
    const [passwordsMatch, setPasswordsMatch] = useState(false)

    const lowerRegex = /[a-z]/
    const numberRegex = /[0-9]/
    const specialRegex = /[!@#$%^&*]/
    const allowedUsernameRegex = /^[a-z0-9_]+$/

    const handleUsernameChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9_]/g, '')
        setUsername(filtered)
        setUsernameLength(filtered.length >= 4 && filtered.length <= 20)
        setUsernameChars(allowedUsernameRegex.test(filtered))
    }

    const handlePasswordChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9!@#$%^&*]/g, '')
        setPassword(filtered)
        setPassLength(filtered.length >= 8)
        setPassLower(lowerRegex.test(filtered))
        setPassNumber(numberRegex.test(filtered))
        setPassSpecial(specialRegex.test(filtered))
        setPasswordsMatch(filtered === passwordConfirm) // 비밀번호 확인 체크
    }

    const handlePasswordConfirmChange = (value: string) => {
        setPasswordConfirm(value)
        setPasswordsMatch(password === value)
    }

    const usernameValid = usernameLength && usernameChars
    const passwordValid = passLength && passLower && passNumber && passSpecial

    useEffect(() => {
        if (!username || !usernameValid) {
            setUsernameExists(false)
            return
        }
        const handler = setTimeout(async () => {
            try {
                const res = await checkUsernameAPI(username)
                setUsernameExists(res.exists)
            } catch {
                setUsernameExists(false)
            }
        }, 500)
        return () => clearTimeout(handler)
    }, [username, usernameValid])

    const handleSignup = async () => {
        setError('')
        if (!username || !password || !passwordConfirm)
            return setError('아이디와 비밀번호를 입력하세요.')
        if (!usernameValid) return setError('아이디 조건을 확인하세요.')
        if (usernameExists) return setError('이미 존재하는 아이디입니다.')
        if (!passwordValid) return setError('비밀번호 조건을 확인하세요.')
        if (!passwordsMatch) return setError('비밀번호가 일치하지 않습니다.')

        try {
            await signUp({ username, password })
            openModal() // 모달 띄우기
        } catch (e: any) {
            setError(e.response?.data?.message ?? '회원가입 실패: 알 수 없는 오류가 발생했습니다.')
        }
    }

    const Condition = ({ met, text }: { met: boolean; text: string }) => (
        <li
            style={{
                color: met ? '#39A54A' : '#FF6347',
                fontSize: 12,
                margin: '2px 0',
                fontWeight: 'bold',
            }}
        >
            {met ? '✔' : '✖'} {text}
        </li>
    )

    return (
        <AuthLayout>
            <AnimatePresence>
                {!fadeOut && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.3 }}
                        style={styles.card}
                    >
                        <h2 style={styles.title}>회원가입</h2>

                        {/* 아이디 입력 */}
                        <input
                            style={styles.input}
                            placeholder="아이디"
                            value={username}
                            onChange={(e) => handleUsernameChange(e.target.value)}
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            <Condition met={usernameLength} text="4~20자" />
                            <Condition met={usernameChars} text="소문자·숫자·_만 사용" />
                            {username.length > 0 && <Condition met={!usernameExists} text="중복되지 않은 아이디" />}
                        </ul>

                        {/* 비밀번호 입력 */}
                        <input
                            style={styles.input}
                            type="password"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) => handlePasswordChange(e.target.value)}
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            <Condition met={passLength} text="8자 이상" />
                            <Condition met={passLower} text="소문자 포함" />
                            <Condition met={passNumber} text="숫자 포함" />
                            <Condition met={passSpecial} text="특수문자 포함 (!@#$%^&*)" />
                        </ul>

                        {/* 비밀번호 확인 */}
                        <input
                            style={styles.input}
                            type="password"
                            placeholder="비밀번호 확인"
                            value={passwordConfirm}
                            onChange={(e) => handlePasswordConfirmChange(e.target.value)}
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            {passwordConfirm.length > 0 && (
                                <Condition met={passwordsMatch} text="비밀번호 일치" />
                            )}
                        </ul>

                        {error && <p style={styles.error}>{error}</p>}

                        <button
                            style={{
                                ...styles.button,
                                opacity:
                                    usernameValid &&
                                    !usernameExists &&
                                    passwordValid &&
                                    passwordsMatch
                                        ? 1
                                        : 0.6,
                                cursor:
                                    usernameValid &&
                                    !usernameExists &&
                                    passwordValid &&
                                    passwordsMatch
                                        ? 'pointer'
                                        : 'not-allowed',
                            }}
                            onClick={handleSignup}
                            disabled={!usernameValid || usernameExists || !passwordValid || !passwordsMatch}
                        >
                            회원가입
                        </button>

                        <p style={styles.loginText}>
                            이미 계정이 있으신가요?{' '}
                            <Link to="/login" style={styles.loginLink}>
                                로그인
                            </Link>
                        </p>

                        {/* 회원가입 성공 모달 */}
                        <Modal show={showModal} onClose={closeModal}>
                            <motion.div
                                initial={{ opacity: 0, scale: 0.9 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.9 }}
                                transition={{ duration: 0.25 }}
                                style={{ textAlign: 'center' }}
                            >
                                <h3>회원가입 성공!</h3>
                                <p>로그인 화면으로 이동합니다.</p>
                                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '16px' }}>
                                    <button style={modalStyles.button} onClick={closeModal}>
                                        확인
                                    </button>
                                </div>
                            </motion.div>
                        </Modal>
                    </motion.div>
                )}
            </AnimatePresence>
        </AuthLayout>
    )
}

const modalStyles = {
    overlay: {
        position: 'fixed' as const,
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        backgroundColor: 'rgba(0,0,0,0.6)',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        zIndex: 1000,
    },
    modal: {
        backgroundColor: '#1E1E1E',
        padding: '30px',
        borderRadius: '12px',
        textAlign: 'center' as const,
        color: '#FFFFFF',
        minWidth: '300px',
    },
    button: {
        marginTop: '20px',
        padding: '10px 20px',
        backgroundColor: '#4F9DFF',
        color: '#FFFFFF',
        border: 'none',
        borderRadius: '6px',
        cursor: 'pointer',
    },
}

const styles = {
    card: {
        width: '360px',
        padding: '40px',
        borderRadius: '12px',
        backgroundColor: '#1E1E1E',
        boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
        display: 'flex',
        flexDirection: 'column' as const,
        gap: '8px',
    },
    title: {
        color: '#4F9DFF',
        textAlign: 'center' as const,
        marginBottom: '20px',
    },
    input: {
        padding: '12px',
        fontSize: '14px',
        borderRadius: '6px',
        border: '1px solid #333',
        backgroundColor: '#2A2A2A',
        color: '#FFFFFF',
    },
    button: {
        padding: '12px',
        fontSize: '15px',
        borderRadius: '6px',
        border: 'none',
        fontWeight: 500,
        backgroundColor: '#4F9DFF',
        color: '#FFFFFF',
        marginTop: '12px',
    },
    error: {
        color: '#FF6347',
        fontSize: '13px',
        textAlign: 'center' as const,
        margin: '4px 0',
    },
    loginText: {
        textAlign: 'center' as const,
        fontSize: '13px',
        color: '#AAAAAA',
        marginTop: '12px',
    },
    loginLink: {
        color: '#4F9DFF',
        textDecoration: 'none',
    },
}
