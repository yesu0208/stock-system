import { useState, useEffect, useRef } from 'react'
import { FiEye, FiEyeOff, FiCheck, FiX, FiLoader } from 'react-icons/fi'
import axios from 'axios'
import './SignupPage.css'
import LoginBackground from './LoginBackground'
import { signUp, checkUsernameAPI, checkNicknameAPI } from '../api/auth'

interface Props {
    onNavigateToLogin: () => void
}

type SignupState = 'idle' | 'loading' | 'success' | 'exiting' | 'failure'
type AvailabilityState = 'idle' | 'checking' | 'available' | 'taken'

function SignupRuleItem({
                            ok,
                            pending,
                            children,
                        }: {
    ok: boolean
    pending?: boolean
    children: React.ReactNode
}) {
    return (
        <li className="signup-rule" data-state={pending ? 'pending' : ok ? 'ok' : 'fail'}>
            <span className="signup-rule__icon" aria-hidden="true">
                {pending ? (
                    <FiLoader size={14} className="signup-rule__icon-spin" />
                ) : ok ? (
                    <FiCheck size={14} />
                ) : (
                    <FiX size={14} />
                )}
            </span>
            <span>{children}</span>
        </li>
    )
}

export default function SignupPage({ onNavigateToLogin }: Props) {
    const [id, setId] = useState('')
    const [nickname, setNickname] = useState('')
    const [pw, setPw] = useState('')
    const [pwConfirm, setPwConfirm] = useState('')
    const [error, setError] = useState('')
    const [state, setState] = useState<SignupState>('idle')

    const [showPw, setShowPw] = useState(false)
    const [showPwConfirm, setShowPwConfirm] = useState(false)

    const [idInvalid, setIdInvalid] = useState(false)
    const [nicknameInvalid, setNicknameInvalid] = useState(false)
    const [pwInvalid, setPwInvalid] = useState(false)
    const [pwConfirmInvalid, setPwConfirmInvalid] = useState(false)

    const [idAvailability, setIdAvailability] = useState<AvailabilityState>('idle')
    const [nicknameAvailability, setNicknameAvailability] = useState<AvailabilityState>('idle')

    const [isEntering, setIsEntering] = useState(true)

    useEffect(() => {
        const timer = window.setTimeout(() => setIsEntering(false), 20)
        return () => window.clearTimeout(timer)
    }, [])

    // 에러 메시지 높이를 실측해서 부드럽게 펼쳐지도록 처리
    const errorInnerRef = useRef<HTMLDivElement>(null)
    const [errorWrapHeight, setErrorWrapHeight] = useState(0)

    useEffect(() => {
        if (error && errorInnerRef.current) {
            setErrorWrapHeight(errorInnerRef.current.scrollHeight)
        } else {
            setErrorWrapHeight(0)
        }
    }, [error])

    const idLengthOk = id.length >= 4 && id.length <= 20
    const idFormatOk = /^[a-z0-9]+$/.test(id)

    const nicknameLengthOk = nickname.length >= 2 && nickname.length <= 10
    const nicknameFormatOk = /^[a-z0-9가-힣]+$/.test(nickname)

    const pwLengthOk = pw.length >= 8
    const pwLowerOk = /[a-z]/.test(pw)
    const pwNumberOk = /[0-9]/.test(pw)
    const pwSpecialOk = /[!@#$%^&*]/.test(pw)

    const pwConfirmMatchOk = pwConfirm.length > 0 && pw === pwConfirm

    // 아이디 중복 확인 — 형식이 유효할 때만 디바운스 후 실제 API 호출
    useEffect(() => {
        if (!idLengthOk || !idFormatOk) {
            setIdAvailability('idle')
            return
        }

        setIdAvailability('checking')

        const timer = window.setTimeout(async () => {
            try {
                const res = await checkUsernameAPI(id)
                setIdAvailability(res.exists ? 'taken' : 'available')
            } catch {
                setIdAvailability('idle')
            }
        }, 500)

        return () => window.clearTimeout(timer)
    }, [id, idLengthOk, idFormatOk])

    // 닉네임 중복 확인 — 동일한 방식
    useEffect(() => {
        if (!nicknameLengthOk || !nicknameFormatOk) {
            setNicknameAvailability('idle')
            return
        }

        setNicknameAvailability('checking')

        const timer = window.setTimeout(async () => {
            try {
                const res = await checkNicknameAPI(nickname)
                setNicknameAvailability(res.exists ? 'taken' : 'available')
            } catch {
                setNicknameAvailability('idle')
            }
        }, 500)

        return () => window.clearTimeout(timer)
    }, [nickname, nicknameLengthOk, nicknameFormatOk])

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        if (state !== 'idle') return

        const isIdEmpty = !id.trim()
        const isNicknameEmpty = !nickname.trim()
        const isPwEmpty = !pw.trim()
        const isPwConfirmEmpty = !pwConfirm.trim()
        const isPwMismatch = !isPwEmpty && !isPwConfirmEmpty && pw !== pwConfirm

        setIdInvalid(isIdEmpty)
        setNicknameInvalid(isNicknameEmpty)
        setPwInvalid(isPwEmpty)
        setPwConfirmInvalid(isPwConfirmEmpty || isPwMismatch)

        if (isIdEmpty || isNicknameEmpty || isPwEmpty || isPwConfirmEmpty) {
            setError('모든 항목을 입력해주세요.')
            return
        }

        if (isPwMismatch) {
            setError('비밀번호가 일치하지 않습니다.')
            return
        }

        if (!idLengthOk || !idFormatOk) {
            setIdInvalid(true)
            setError('아이디 형식을 확인해주세요. \n(4~20자, 영어 소문자/숫자)')
            return
        }

        if (!nicknameLengthOk || !nicknameFormatOk) {
            setNicknameInvalid(true)
            setError('닉네임 형식을 확인해주세요. \n(2~10자, 영어 소문자/한글/숫자)')
            return
        }

        if (!pwLengthOk || !pwLowerOk || !pwNumberOk || !pwSpecialOk) {
            setPwInvalid(true)
            setError('비밀번호 조건을 확인해주세요. \n(8자 이상, 소문자/숫자/특수문자 포함)')
            return
        }

        if (idAvailability !== 'available') {
            setIdInvalid(true)
            setError(
                idAvailability === 'taken'
                    ? '이미 사용 중인 아이디입니다.'
                    : '아이디 중복 확인이 필요합니다.'
            )
            return
        }

        if (nicknameAvailability !== 'available') {
            setNicknameInvalid(true)
            setError(
                nicknameAvailability === 'taken'
                    ? '이미 사용 중인 닉네임입니다.'
                    : '닉네임 중복 확인이 필요합니다.'
            )
            return
        }

        setError('')
        setState('loading')

        try {
            await signUp({ username: id, nickname, password: pw })

            setState('success')
            window.setTimeout(() => {
                setState('exiting')
                window.setTimeout(onNavigateToLogin, 500)
            }, 1200)
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                setError(err.response?.data?.message ?? '회원가입 실패: 서버 오류가 발생했습니다.')
            } else {
                setError('회원가입 실패: 알 수 없는 오류가 발생했습니다.')
            }
            setState('failure')

            window.setTimeout(() => {
                setState('idle')
            }, 1800)
        }
    }

    const statusText =
        state === 'loading' ? '가입 처리 중'
            : state === 'success' || state === 'exiting' ? '가입 완료'
                : state === 'failure' ? '가입 실패'
                    : ''

    return (
        <div className={`signup-wrapper${state === 'exiting' ? ' is-exiting' : ''}${isEntering ? ' is-entering' : ''}`}>
            <LoginBackground />

            <div className="signup-layout">
                <aside className="signup-rules-panel" data-state={state} aria-label="회원가입 입력 조건">
                    <div className="signup-rules-group">
                        <p className="signup-rules-group__label">아이디</p>
                        <ul className="signup-rules-list">
                            <SignupRuleItem ok={idLengthOk}>4~20자</SignupRuleItem>
                            <SignupRuleItem ok={idFormatOk}>영어 소문자, 숫자만 사용</SignupRuleItem>
                            <SignupRuleItem
                                ok={idAvailability === 'available'}
                                pending={idAvailability === 'checking'}
                            >
                                사용 가능한 아이디
                            </SignupRuleItem>
                        </ul>
                    </div>

                    <div className="signup-rules-group">
                        <p className="signup-rules-group__label">닉네임</p>
                        <ul className="signup-rules-list">
                            <SignupRuleItem ok={nicknameLengthOk}>2~10자</SignupRuleItem>
                            <SignupRuleItem ok={nicknameFormatOk}>영어 소문자, 한글, 숫자만 사용</SignupRuleItem>
                            <SignupRuleItem
                                ok={nicknameAvailability === 'available'}
                                pending={nicknameAvailability === 'checking'}
                            >
                                사용 가능한 닉네임
                            </SignupRuleItem>
                        </ul>
                    </div>

                    <div className="signup-rules-group">
                        <p className="signup-rules-group__label">비밀번호</p>
                        <ul className="signup-rules-list">
                            <SignupRuleItem ok={pwLengthOk}>8자 이상</SignupRuleItem>
                            <SignupRuleItem ok={pwLowerOk}>영어 소문자 포함</SignupRuleItem>
                            <SignupRuleItem ok={pwNumberOk}>숫자 포함</SignupRuleItem>
                            <SignupRuleItem ok={pwSpecialOk}>특수문자 포함 (!@#$%^&*)</SignupRuleItem>
                        </ul>
                    </div>

                    <div className="signup-rules-group">
                        <p className="signup-rules-group__label">비밀번호 확인</p>
                        <ul className="signup-rules-list">
                            <SignupRuleItem ok={pwConfirmMatchOk}>비밀번호와 일치</SignupRuleItem>
                        </ul>
                    </div>
                </aside>

                <form className="signup-box" data-state={state} onSubmit={handleSubmit}>
                    <h1 className="signup-title">회원가입</h1>

                    <div className="signup-fields" data-state={state}>
                        <div className="signup-fields__inner">
                            <div className="signup-field">
                                <label htmlFor="signup-id">아이디</label>
                                <input
                                    id="signup-id"
                                    type="text"
                                    value={id}
                                    onChange={(e) => {
                                        setId(e.target.value.replace(/[^a-z0-9]/g, ''))
                                        if (idInvalid) setIdInvalid(false)
                                    }}
                                    placeholder="아이디를 입력하세요"
                                    autoComplete="username"
                                    disabled={state !== 'idle'}
                                    className={idInvalid ? 'is-invalid' : ''}
                                />
                            </div>

                            <div className="signup-field">
                                <label htmlFor="signup-nickname">닉네임</label>
                                <input
                                    id="signup-nickname"
                                    type="text"
                                    value={nickname}
                                    onChange={(e) => {
                                        setNickname(e.target.value.replace(/[^a-z0-9가-힣]/g, ''))
                                        if (nicknameInvalid) setNicknameInvalid(false)
                                    }}
                                    placeholder="닉네임을 입력하세요"
                                    autoComplete="nickname"
                                    disabled={state !== 'idle'}
                                    className={nicknameInvalid ? 'is-invalid' : ''}
                                />
                            </div>

                            <div className="signup-field">
                                <label htmlFor="signup-pw">비밀번호</label>
                                <div className="signup-field__input-wrap">
                                    <input
                                        id="signup-pw"
                                        type={showPw ? 'text' : 'password'}
                                        value={pw}
                                        onChange={(e) => {
                                            setPw(e.target.value.replace(/[^a-z0-9!@#$%^&*]/g, ''))
                                            if (pwInvalid) setPwInvalid(false)
                                        }}
                                        placeholder="비밀번호를 입력하세요"
                                        autoComplete="new-password"
                                        disabled={state !== 'idle'}
                                        className={pwInvalid ? 'is-invalid' : ''}
                                    />
                                    <button
                                        type="button"
                                        className="signup-field__toggle-pw"
                                        onClick={() => setShowPw((prev) => !prev)}
                                        disabled={state !== 'idle'}
                                        tabIndex={-1}
                                        aria-label={showPw ? '비밀번호 숨기기' : '비밀번호 표시'}
                                    >
                                        {showPw ? <FiEyeOff size={16} /> : <FiEye size={16} />}
                                    </button>
                                </div>
                            </div>

                            <div className="signup-field">
                                <label htmlFor="signup-pw-confirm">비밀번호 확인</label>
                                <div className="signup-field__input-wrap">
                                    <input
                                        id="signup-pw-confirm"
                                        type={showPwConfirm ? 'text' : 'password'}
                                        value={pwConfirm}
                                        onChange={(e) => {
                                            setPwConfirm(e.target.value.replace(/[^a-z0-9!@#$%^&*]/g, ''))
                                            if (pwConfirmInvalid) setPwConfirmInvalid(false)
                                        }}
                                        placeholder="비밀번호를 다시 입력하세요"
                                        autoComplete="new-password"
                                        disabled={state !== 'idle'}
                                        className={pwConfirmInvalid ? 'is-invalid' : ''}
                                    />
                                    <button
                                        type="button"
                                        className="signup-field__toggle-pw"
                                        onClick={() => setShowPwConfirm((prev) => !prev)}
                                        disabled={state !== 'idle'}
                                        tabIndex={-1}
                                        aria-label={showPwConfirm ? '비밀번호 숨기기' : '비밀번호 표시'}
                                    >
                                        {showPwConfirm ? <FiEyeOff size={16} /> : <FiEye size={16} />}
                                    </button>
                                </div>
                            </div>

                            <div
                                className="signup-error-wrap"
                                data-visible={error ? 'true' : 'false'}
                                style={{ height: errorWrapHeight }}
                            >
                                <div className="signup-error-wrap__inner" ref={errorInnerRef}>
                                    {error && <p className="signup-error" key={error}>{error}</p>}
                                </div>
                            </div>
                        </div>
                    </div>

                    <p className="signup-back-text" data-state={state}>
                        이미 계정이 있으신가요?{' '}
                        <button
                            type="button"
                            className="signup-back-link"
                            onClick={onNavigateToLogin}
                            disabled={state !== 'idle'}
                        >
                            로그인
                        </button>
                    </p>

                    <div className="signup-submit-area">
                        <button
                            type="submit"
                            className="signup-submit"
                            data-state={state}
                            disabled={state !== 'idle'}
                        >
                            <span className="signup-submit__label">가입하기</span>

                            <span className="signup-submit__spinner" aria-hidden="true">
                                <svg viewBox="0 0 44 44">
                                    <circle className="signup-submit__spinner-track" cx="22" cy="22" r="18" />
                                    <circle className="signup-submit__spinner-arc" cx="22" cy="22" r="18" />
                                </svg>
                            </span>

                            <span className="signup-submit__check" aria-hidden="true">
                                <svg viewBox="0 0 44 44">
                                    <path className="signup-submit__check-mark" d="M13 22.5L19 28.5L31 15.5" />
                                </svg>
                            </span>

                            <span className="signup-submit__cross" aria-hidden="true">
                                <svg viewBox="0 0 44 44">
                                    <path className="signup-submit__cross-line signup-submit__cross-line--1" d="M15 15L29 29" />
                                    <path className="signup-submit__cross-line signup-submit__cross-line--2" d="M29 15L15 29" />
                                </svg>
                            </span>
                        </button>

                        <p className="signup-status" data-state={state}>
                            {statusText}
                        </p>
                    </div>
                </form>
            </div>
        </div>
    )
}
