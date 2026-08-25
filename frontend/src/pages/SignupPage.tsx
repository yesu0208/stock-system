import { useState, useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import AuthLayout from '../layouts/AuthLayout'
import { signUp, checkUsernameAPI, checkNicknameAPI } from '../api/auth' // [수정] checkNicknameAPI 추가
import Modal from '../components/Modal'
import { motion, AnimatePresence } from 'framer-motion'
import axios from 'axios'

interface ConditionProps {
    met: boolean
    text: string
}

function Condition({ met, text }: ConditionProps) {
    return (
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
}

export default function SignupPage() {
    const navigate = useNavigate()

    const [username, setUsername] = useState('')
    const [nickname, setNickname] = useState('') // [신규] 닉네임 상태
    const [password, setPassword] = useState('')
    const [passwordConfirm, setPasswordConfirm] = useState('')
    const [error, setError] = useState('')

    const [showModal, setShowModal] = useState(false)
    const [fadeOut, setFadeOut] = useState(false)

    const openModal = () => setShowModal(true)

    const closeModal = () => {
        setShowModal(false)
        setFadeOut(true)
    }

    // 페이지 fade-out 후 이동
    useEffect(() => {
        if (fadeOut) {
            const timer = setTimeout(() => {
                navigate('/login')
            }, 300)
            return () => clearTimeout(timer)
        }
    }, [fadeOut, navigate])

    // 유효성 상태
    const [usernameLength, setUsernameLength] = useState(false)
    const [usernameChars, setUsernameChars] = useState(false)
    const [usernameExists, setUsernameExists] = useState(false)

    // [신규] 닉네임 유효성 상태
    const [nicknameLength, setNicknameLength] = useState(false)
    const [nicknameChars, setNicknameChars] = useState(false)
    const [nicknameExists, setNicknameExists] = useState(false)

    const [passLength, setPassLength] = useState(false)
    const [passLower, setPassLower] = useState(false)
    const [passNumber, setPassNumber] = useState(false)
    const [passSpecial, setPassSpecial] = useState(false)
    const [passwordsMatch, setPasswordsMatch] = useState(false)

    const lowerRegex = /[a-z]/
    const numberRegex = /[0-9]/
    const specialRegex = /[!@#$%^&*]/
    const allowedUsernameRegex = /^[a-z0-9]+$/
    const allowedNicknameRegex = /^[a-z0-9가-힣]+$/ // [신규] 닉네임 허용 문자: 영소문자, 한글, 숫자

    const handleUsernameChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9]/g, '')
        setUsername(filtered)
        setUsernameLength(filtered.length >= 4 && filtered.length <= 20)
        setUsernameChars(allowedUsernameRegex.test(filtered))
    }

    // [신규] 닉네임 입력 처리 (영소문자, 한글, 숫자만 허용)
    const handleNicknameChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9가-힣]/g, '')
        setNickname(filtered)
        setNicknameLength(filtered.length >= 2 && filtered.length <= 10)
        setNicknameChars(allowedNicknameRegex.test(filtered))
    }

    const handlePasswordChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9!@#$%^&*]/g, '')
        setPassword(filtered)
        setPassLength(filtered.length >= 8)
        setPassLower(lowerRegex.test(filtered))
        setPassNumber(numberRegex.test(filtered))
        setPassSpecial(specialRegex.test(filtered))
        setPasswordsMatch(filtered === passwordConfirm)
    }

    const handlePasswordConfirmChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9!@#$%^&*]/g, '')
        setPasswordConfirm(filtered)
        setPasswordsMatch(password === filtered)
    }

    const usernameValid = usernameLength && usernameChars
    const nicknameValid = nicknameLength && nicknameChars // [신규]
    const passwordValid = passLength && passLower && passNumber && passSpecial

    // 아이디 중복 체크
    useEffect(() => {
        if (!username || !usernameValid) {
            // 상태를 안전하게 초기화
            setTimeout(() => setUsernameExists(false), 0)
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

    // [신규] 닉네임 중복 체크
    useEffect(() => {
        if (!nickname || !nicknameValid) {
            setTimeout(() => setNicknameExists(false), 0)
            return
        }

        const handler = setTimeout(async () => {
            try {
                const res = await checkNicknameAPI(nickname)
                setNicknameExists(res.exists)
            } catch {
                setNicknameExists(false)
            }
        }, 500)

        return () => clearTimeout(handler)
    }, [nickname, nicknameValid])

    // 회원가입
    const handleSignup = async () => {
        setError('')

        if (!username || !nickname || !password || !passwordConfirm) // [수정] nickname 체크 추가
            return setError('아이디, 닉네임, 비밀번호를 입력하세요.')

        if (!usernameValid)
            return setError('아이디 조건을 확인하세요.')

        if (usernameExists)
            return setError('이미 존재하는 아이디입니다.')

        if (!nicknameValid) // [신규]
            return setError('닉네임 조건을 확인하세요.')

        if (nicknameExists) // [신규]
            return setError('이미 존재하는 닉네임입니다.')

        if (!passwordValid)
            return setError('비밀번호 조건을 확인하세요.')

        if (!passwordsMatch)
            return setError('비밀번호가 일치하지 않습니다.')

        try {
            await signUp({ username, nickname, password }) // [수정] nickname 전달
            openModal()
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                setError(
                    err.response?.data?.message ??
                    '회원가입 실패: 서버 오류가 발생했습니다.'
                )
            } else {
                setError('회원가입 실패: 알 수 없는 오류가 발생했습니다.')
            }
        }
    }

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

                        {/* 아이디 */}
                        <input
                            style={styles.input}
                            placeholder="아이디"
                            value={username}
                            onChange={(e) =>
                                handleUsernameChange(e.target.value)
                            }
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            <Condition met={usernameLength} text="4~20자" />
                            <Condition
                                met={usernameChars}
                                text="영어 소문자·숫자만 사용"
                            />
                            <Condition
                                met={username.length > 0 && !usernameExists}
                                text={
                                    username.length === 0
                                        ? '아이디 입력'
                                        : '중복되지 않은 아이디'
                                }
                            />
                        </ul>

                        {/* [신규] 닉네임 */}
                        <input
                            style={styles.input}
                            placeholder="닉네임"
                            value={nickname}
                            onChange={(e) =>
                                handleNicknameChange(e.target.value)
                            }
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            <Condition met={nicknameLength} text="2~10자" />
                            <Condition
                                met={nicknameChars}
                                text="영어 소문자·한글·숫자만 사용"
                            />
                            <Condition
                                met={nickname.length > 0 && !nicknameExists}
                                text={
                                    nickname.length === 0
                                        ? '닉네임 입력'
                                        : '중복되지 않은 닉네임'
                                }
                            />
                        </ul>

                        {/* 비밀번호 */}
                        <input
                            style={styles.input}
                            type="password"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) =>
                                handlePasswordChange(e.target.value)
                            }
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            <Condition met={passLength} text="8자 이상" />
                            <Condition met={passLower} text="소문자 포함" />
                            <Condition met={passNumber} text="숫자 포함" />
                            <Condition
                                met={passSpecial}
                                text="특수문자 포함 (!@#$%^&*)"
                            />
                        </ul>

                        {/* 비밀번호 확인 */}
                        <input
                            style={styles.input}
                            type="password"
                            placeholder="비밀번호 확인"
                            value={passwordConfirm}
                            onChange={(e) =>
                                handlePasswordConfirmChange(e.target.value)
                            }
                        />
                        <ul style={{ listStyle: 'none', paddingLeft: 0, margin: '4px 0' }}>
                            <Condition
                                met={passwordConfirm.length > 0 && passwordsMatch}
                                text={
                                    passwordConfirm.length === 0
                                        ? '비밀번호 확인 입력'
                                        : '비밀번호 일치'
                                }
                            />
                        </ul>

                        {error && <p style={styles.error}>{error}</p>}

                        <button
                            style={{
                                ...styles.button,
                                opacity:
                                    usernameValid &&
                                    !usernameExists &&
                                    nicknameValid && // [수정]
                                    !nicknameExists && // [수정]
                                    passwordValid &&
                                    passwordsMatch
                                        ? 1
                                        : 0.6,
                                cursor:
                                    usernameValid &&
                                    !usernameExists &&
                                    nicknameValid && // [수정]
                                    !nicknameExists && // [수정]
                                    passwordValid &&
                                    passwordsMatch
                                        ? 'pointer'
                                        : 'not-allowed',
                            }}
                            onClick={handleSignup}
                            disabled={
                                !usernameValid ||
                                usernameExists ||
                                !nicknameValid || // [수정]
                                nicknameExists || // [수정]
                                !passwordValid ||
                                !passwordsMatch
                            }
                        >
                            회원가입
                        </button>

                        <p style={styles.loginText}>
                            이미 계정이 있으신가요?{' '}
                            <Link to="/login" style={styles.loginLink}>
                                로그인
                            </Link>
                        </p>

                        {/* 성공 모달 */}
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
                                <button
                                    style={modalStyles.button}
                                    onClick={closeModal}
                                >
                                    확인
                                </button>
                            </motion.div>
                        </Modal>
                    </motion.div>
                )}
            </AnimatePresence>
        </AuthLayout>
    )
}

const modalStyles = {
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
        padding: '14px 40px',
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
        marginTop: '2px',
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
        marginTop: '' +
            '6px',
    },
    loginLink: {
        color: '#4F9DFF',
        textDecoration: 'none',
    },
}