import { useState, useEffect } from 'react'
import AuthLayout from '../layouts/AuthLayout'
import { signUp, checkUsernameAPI, checkNicknameAPI } from '../api/auth'
import Modal from '../components/Modal'
import { motion, AnimatePresence } from 'framer-motion'
import axios from 'axios'
import styles from './SignupPage.module.css'

interface Props {
    onNavigateToLogin: () => void
}

interface ConditionProps {
    met: boolean
    text: string
}

function Condition({ met, text }: ConditionProps) {
    return (
        <li className={met ? styles.conditionMet : styles.conditionUnmet}>
            {met ? '✔' : '✖'} {text}
        </li>
    )
}

export default function SignupPage({ onNavigateToLogin }: Props) {
    const [username, setUsername] = useState('')
    const [nickname, setNickname] = useState('')
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
    // [5단계 변경] navigate('/login') → onNavigateToLogin()
    useEffect(() => {
        if (fadeOut) {
            const timer = setTimeout(() => {
                onNavigateToLogin()
            }, 300)
            return () => clearTimeout(timer)
        }
    }, [fadeOut, onNavigateToLogin])

    const [usernameLength, setUsernameLength] = useState(false)
    const [usernameChars, setUsernameChars] = useState(false)
    const [usernameExists, setUsernameExists] = useState(false)

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
    const allowedNicknameRegex = /^[a-z0-9가-힣]+$/

    const handleUsernameChange = (value: string) => {
        const filtered = value.replace(/[^a-z0-9]/g, '')
        setUsername(filtered)
        setUsernameLength(filtered.length >= 4 && filtered.length <= 20)
        setUsernameChars(allowedUsernameRegex.test(filtered))
    }

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
    const nicknameValid = nicknameLength && nicknameChars
    const passwordValid = passLength && passLower && passNumber && passSpecial

    useEffect(() => {
        if (!username || !usernameValid) {
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

    const handleSignup = async () => {
        setError('')

        if (!username || !nickname || !password || !passwordConfirm)
            return setError('아이디, 닉네임, 비밀번호를 입력하세요.')

        if (!usernameValid)
            return setError('아이디 조건을 확인하세요.')

        if (usernameExists)
            return setError('이미 존재하는 아이디입니다.')

        if (!nicknameValid)
            return setError('닉네임 조건을 확인하세요.')

        if (nicknameExists)
            return setError('이미 존재하는 닉네임입니다.')

        if (!passwordValid)
            return setError('비밀번호 조건을 확인하세요.')

        if (!passwordsMatch)
            return setError('비밀번호가 일치하지 않습니다.')

        try {
            await signUp({ username, nickname, password })
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
                        className={styles.card}
                    >
                        <h2 className={styles.title}>회원가입</h2>

                        <input
                            className={styles.input}
                            placeholder="아이디"
                            value={username}
                            onChange={(e) => handleUsernameChange(e.target.value)}
                        />
                        <ul className={styles.conditionList}>
                            <Condition met={usernameLength} text="4~20자" />
                            <Condition met={usernameChars} text="영어 소문자·숫자만 사용" />
                            <Condition
                                met={username.length > 0 && !usernameExists}
                                text={username.length === 0 ? '아이디 입력' : '중복되지 않은 아이디'}
                            />
                        </ul>

                        <input
                            className={styles.input}
                            placeholder="닉네임"
                            value={nickname}
                            onChange={(e) => handleNicknameChange(e.target.value)}
                        />
                        <ul className={styles.conditionList}>
                            <Condition met={nicknameLength} text="2~10자" />
                            <Condition met={nicknameChars} text="영어 소문자·한글·숫자만 사용" />
                            <Condition
                                met={nickname.length > 0 && !nicknameExists}
                                text={nickname.length === 0 ? '닉네임 입력' : '중복되지 않은 닉네임'}
                            />
                        </ul>

                        <input
                            className={styles.input}
                            type="password"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) => handlePasswordChange(e.target.value)}
                        />
                        <ul className={styles.conditionList}>
                            <Condition met={passLength} text="8자 이상" />
                            <Condition met={passLower} text="소문자 포함" />
                            <Condition met={passNumber} text="숫자 포함" />
                            <Condition met={passSpecial} text="특수문자 포함 (!@#$%^&*)" />
                        </ul>

                        <input
                            className={styles.input}
                            type="password"
                            placeholder="비밀번호 확인"
                            value={passwordConfirm}
                            onChange={(e) => handlePasswordConfirmChange(e.target.value)}
                        />
                        <ul className={styles.conditionList}>
                            <Condition
                                met={passwordConfirm.length > 0 && passwordsMatch}
                                text={passwordConfirm.length === 0 ? '비밀번호 확인 입력' : '비밀번호 일치'}
                            />
                        </ul>

                        {error && <p className={styles.error}>{error}</p>}

                        <button
                            className={styles.button}
                            onClick={handleSignup}
                            disabled={
                                !usernameValid || usernameExists ||
                                !nicknameValid || nicknameExists ||
                                !passwordValid || !passwordsMatch
                            }
                        >
                            회원가입
                        </button>

                        <p className={styles.loginText}>
                            이미 계정이 있으신가요?{' '}
                            <button className={styles.loginLink} onClick={onNavigateToLogin}>로그인</button>
                        </p>

                        <Modal show={showModal} onClose={closeModal}>
                            <motion.div
                                initial={{ opacity: 0, scale: 0.9 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0, scale: 0.9 }}
                                transition={{ duration: 0.25 }}
                                className={styles.modalContent}
                            >
                                <h3>회원가입 성공!</h3>
                                <p>로그인 화면으로 이동합니다.</p>
                                <button className={styles.modalButton} onClick={closeModal}>확인</button>
                            </motion.div>
                        </Modal>
                    </motion.div>
                )}
            </AnimatePresence>
        </AuthLayout>
    )
}
