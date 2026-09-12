import { useState, useEffect } from 'react'
import { FiEye, FiEyeOff } from 'react-icons/fi'
import './LoginPage.css'
import LoginBackground from './LoginBackground'
import { tokenStorage } from '../utils/token'
import { login } from '../api/auth'
import { useMarketData } from '../main/context/MarketDataContext'

interface Props {
    onLoginSuccess: () => void
    onNavigateToSignup: () => void
}

type LoginState = 'idle' | 'loading' | 'success' | 'exiting' | 'failure'

function formatBaseTime(iso: string): string {
    try {
        const d = new Date(iso)
        const yyyy = d.getFullYear()
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const dd = String(d.getDate()).padStart(2, '0')

        const weekdays = ['일', '월', '화', '수', '목', '금', '토']
        const weekday = weekdays[d.getDay()]

        return `${yyyy}.${mm}.${dd}(${weekday}) 기준`
    } catch {
        return iso
    }
}

// Header.tsx와 동일한 로직으로 시간에 따른 시장 상태 판단
type MarketDot = 'green' | 'yellow' | 'gray'

function getMarketStatus(date: Date): { label: string; dot: MarketDot } {
    const day = date.getDay() // 0=일, 6=토
    const minutes = date.getHours() * 60 + date.getMinutes()

    if (day === 0 || day === 6) {
        return { label: '장마감', dot: 'gray' }
    } else if (minutes >= 530 && minutes < 540) {
        // 08:50 ~ 09:00
        return { label: '동시호가', dot: 'yellow' }
    } else if (minutes >= 540 && minutes < 920) {
        // 09:00 ~ 15:20
        return { label: '개장', dot: 'green' }
    } else if (minutes >= 920 && minutes < 930) {
        // 15:20 ~ 15:30
        return { label: '동시호가', dot: 'yellow' }
    } else {
        return { label: '장마감', dot: 'gray' }
    }
}

export default function LoginPage({ onLoginSuccess, onNavigateToSignup }: Props) {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [state, setState] = useState<LoginState>('idle')

    const [showPw, setShowPw] = useState(false)

    const [usernameInvalid, setUsernameInvalid] = useState(false)
    const [passwordInvalid, setPasswordInvalid] = useState(false)

    const { marketMain } = useMarketData()

    // KOSPI/KOSDAQ을 동시에 보여주지 않고 5초마다 번갈아 표시
    const [marketTab, setMarketTab] = useState<'KOSPI' | 'KOSDAQ'>('KOSPI')

    useEffect(() => {
        const timer = window.setInterval(() => {
            setMarketTab((prev) => (prev === 'KOSPI' ? 'KOSDAQ' : 'KOSPI'))
        }, 5000)
        return () => window.clearInterval(timer)
    }, [])

    // 시간에 따른 시장 상태(휴장/동시호가/개장) 갱신용 현재 시각
    const [now, setNow] = useState(new Date())

    useEffect(() => {
        const timer = window.setInterval(() => setNow(new Date()), 1000)
        return () => window.clearInterval(timer)
    }, [])

    // 마운트 시 fade-in 트리거
    const [isEntering, setIsEntering] = useState(true)

    useEffect(() => {
        const timer = window.setTimeout(() => setIsEntering(false), 20)
        return () => window.clearTimeout(timer)
    }, [])

    // 기존 토큰이 있으면 자동 로그인
    useEffect(() => {
        const token = tokenStorage.get()
        if (token) {
            onLoginSuccess()
        }
    }, [onLoginSuccess])

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        if (state !== 'idle') return

        const isUsernameEmpty = !username.trim()
        const isPasswordEmpty = !password.trim()

        setUsernameInvalid(isUsernameEmpty)
        setPasswordInvalid(isPasswordEmpty)

        if (isUsernameEmpty || isPasswordEmpty) {
            setError('아이디와 비밀번호를 입력해주세요.')
            return
        }

        setError('')
        setState('loading')

        try {
            await login({ username, password })

            setState('success')
            window.setTimeout(() => {
                setState('exiting')
                window.setTimeout(onLoginSuccess, 500)
            }, 1000)
        } catch (err) {
            console.error('Login failed:', err)
            setPasswordInvalid(true)
            setError('아이디 또는 비밀번호가 올바르지 않습니다.')
            setState('failure')

            window.setTimeout(() => {
                setPassword('')
                setPasswordInvalid(false)
                setState('idle')
            }, 1800)
        }
    }

    const statusText =
        state === 'loading' ? '로그인 중'
            : state === 'success' || state === 'exiting' ? '로그인 성공'
                : state === 'failure' ? '로그인 실패'
                    : ''

    // marketTab에 해당하는 지수만 뽑아서 렌더링에 사용
    const activeIndex = marketMain
        ? (marketTab === 'KOSPI' ? marketMain.kospi : marketMain.kosdaq)
        : null

    const activeIndexColor = activeIndex
        ? activeIndex.direction === 'UP' ? '#f87171'
            : activeIndex.direction === 'DOWN' ? '#60a5fa'
                : '#94a3b8'
        : '#94a3b8'

    // 시간 기반 시장 상태
    const marketStatus = getMarketStatus(now)

    return (
        <div className={`login-wrapper${state === 'exiting' ? ' is-exiting' : ''}${isEntering ? ' is-entering' : ''}`}>
            <LoginBackground />

            <div className="login-brand">
                <h2 className="login-brand__title">
                    <span>모의투자</span> 시스템
                </h2>
                <p className="login-brand__subtitle">
                    실시간 시세를 확인하고, 주주들과 소통하며
                    <br />
                    모의투자를 시작해보세요!
                </p>

                {/* features 리스트보다 위로 이동 */}
                <div className="login-brand__market">
                    {activeIndex ? (
                        <>
                            <div className="login-brand__market-row">
                                <span className={`login-brand__market-dot login-brand__market-dot--${marketStatus.dot}`} />
                                <span className={`login-brand__market-label login-brand__market-label--${marketStatus.dot}`}>
                                    {marketStatus.label}
                                </span>
                                <span className="login-brand__market-divider" />
                                <span className="login-brand__market-time">{formatBaseTime(activeIndex.baseTime)}</span>
                            </div>

                            {/* KOSPI/KOSDAQ 동시 표시 → marketTab 하나만 표시 */}
                            <div className="login-brand__market-row" key={marketTab}>
                                <span className="login-brand__market-index">
                                    <span className="login-brand__market-index-name">{marketTab}</span>{' '}
                                    <span
                                        className="login-brand__market-index-value"
                                        style={{ color: activeIndexColor }}
                                    >
                                        {activeIndex.currentIndex} ({activeIndex.changeRate}%)
                                    </span>
                                </span>
                            </div>
                        </>
                    ) : (
                        <div className="login-brand__market-row">
                            <span className="login-brand__market-dot login-brand__market-dot--gray" />
                            <span className="login-brand__market-label login-brand__market-label--gray">
                                시세 연결 중...
                            </span>
                        </div>
                    )}
                </div>

                <ul className="login-brand__features">
                    <li className="login-brand__feature">
                        <span className="login-brand__feature-text">
                            <strong>종목 정보</strong> - 실시간 시세 정보와 최신 뉴스, 공시를 한눈에 확인할 수 있습니다.
                        </span>
                    </li>
                    <li className="login-brand__feature">
                        <span className="login-brand__feature-text">
                            <strong>고급 주문</strong> - 자동주문, OTOCO, 트레일링 스탑으로 정교한 매매 전략을 세울 수 있습니다.
                        </span>
                    </li>
                    <li className="login-brand__feature">
                        <span className="login-brand__feature-text">
                            <strong>커뮤니티</strong> - 종목 토론방, 종목톡에서 다른 투자자들과 실시간으로 소통할 수 있습니다.
                        </span>
                    </li>
                    <li className="login-brand__feature">
                        <span className="login-brand__feature-text">
                            <strong>랭크 시스템</strong> - 랭킹으로 나의 투자 실력을 확인할 수 있습니다.
                        </span>
                    </li>
                </ul>
            </div>

            <form className="login-box" data-state={state} onSubmit={handleSubmit}>
                <h1 className="login-title">로그인</h1>

                <div className="login-fields" data-state={state}>
                    <div className="login-fields__inner">
                        <div className="login-field">
                            <label htmlFor="login-id">아이디</label>
                            <input
                                id="login-id"
                                type="text"
                                value={username}
                                onChange={(e) => {
                                    setUsername(e.target.value)
                                    if (usernameInvalid) setUsernameInvalid(false)
                                }}
                                placeholder="아이디를 입력하세요"
                                autoComplete="username"
                                disabled={state !== 'idle'}
                                className={usernameInvalid ? 'is-invalid' : ''}
                            />
                        </div>

                        <div className="login-field">
                            <label htmlFor="login-pw">비밀번호</label>
                            <div className="login-field__input-wrap">
                                <input
                                    id="login-pw"
                                    type={showPw ? 'text' : 'password'}
                                    value={password}
                                    onChange={(e) => {
                                        setPassword(e.target.value)
                                        if (passwordInvalid) setPasswordInvalid(false)
                                    }}
                                    placeholder="비밀번호를 입력하세요"
                                    autoComplete="current-password"
                                    disabled={state !== 'idle'}
                                    className={passwordInvalid ? 'is-invalid' : ''}
                                />
                                <button
                                    type="button"
                                    className="login-field__toggle-pw"
                                    onClick={() => setShowPw((prev) => !prev)}
                                    disabled={state !== 'idle'}
                                    tabIndex={-1}
                                    aria-label={showPw ? '비밀번호 숨기기' : '비밀번호 표시'}
                                >
                                    {showPw ? <FiEyeOff size={16} /> : <FiEye size={16} />}
                                </button>
                            </div>
                        </div>

                        <div className="login-error-wrap" data-visible={error ? 'true' : 'false'}>
                            <div className="login-error-wrap__inner">
                                {error && <p className="login-error">{error}</p>}
                            </div>
                        </div>
                    </div>
                </div>

                <p className="login-signup-text" data-state={state}>
                    계정이 없으신가요?{' '}
                    <button
                        type="button"
                        className="login-signup-link"
                        onClick={onNavigateToSignup}
                        disabled={state !== 'idle'}
                    >
                        회원가입
                    </button>
                </p>

                <div className="login-submit-area">
                    <button
                        type="submit"
                        className="login-submit"
                        data-state={state}
                        disabled={state !== 'idle'}
                    >
                        <span className="login-submit__label">로그인</span>

                        <span className="login-submit__spinner" aria-hidden="true">
                            <svg viewBox="0 0 44 44">
                                <circle className="login-submit__spinner-track" cx="22" cy="22" r="18" />
                                <circle className="login-submit__spinner-arc" cx="22" cy="22" r="18" />
                            </svg>
                        </span>

                        <span className="login-submit__check" aria-hidden="true">
                            <svg viewBox="0 0 44 44">
                                <path className="login-submit__check-mark" d="M13 22.5L19 28.5L31 15.5" />
                            </svg>
                        </span>

                        <span className="login-submit__cross" aria-hidden="true">
                            <svg viewBox="0 0 44 44">
                                <path className="login-submit__cross-line login-submit__cross-line--1" d="M15 15L29 29" />
                                <path className="login-submit__cross-line login-submit__cross-line--2" d="M29 15L15 29" />
                            </svg>
                        </span>
                    </button>

                    <p className="login-status" data-state={state}>
                        {statusText}
                    </p>
                </div>
            </form>
        </div>
    )
}
