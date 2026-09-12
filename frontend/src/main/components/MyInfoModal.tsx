import { useState } from 'react'
import Modal from '../../components/Modal'
import { changePassword, changeNickname } from '../../api/userProfile'
import { useUser } from '../context/UserContext'
import { useMsg } from '../context/MsgContext'

interface Props {
    show: boolean
    onClose: () => void
}

const nicknameRegex = /^[a-z0-9가-힣]+$/
const passwordRegex = /^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*]).+$/

export default function MyInfoModal({ show, onClose }: Props) {
    const { user, refresh } = useUser()
    const { success, error } = useMsg()

    const [nickname, setNickname] = useState(user?.nickname ?? '')
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [newPasswordConfirm, setNewPasswordConfirm] = useState('')

    if (!show) return null

    const nicknameValid = nickname.length >= 2 && nickname.length <= 10 && nicknameRegex.test(nickname)
    const passwordValid = newPassword.length >= 8 && passwordRegex.test(newPassword)

    const handleChangeNickname = async () => {
        if (!nicknameValid) {
            error('닉네임은 2~10자의 영문 소문자·한글·숫자만 가능합니다.')
            return
        }
        try {
            await changeNickname({ nickname })
            refresh()
            success('닉네임이 변경되었습니다.')
        } catch (e: any) {
            error(`닉네임 변경 실패: ${e.response?.data?.message ?? e.message}`)
        }
    }

    const handleChangePassword = async () => {
        if (!currentPassword) {
            error('현재 비밀번호를 입력하세요.')
            return
        }
        if (!passwordValid) {
            error('새 비밀번호는 8자 이상, 소문자·숫자·특수문자(!@#$%^&*)를 모두 포함해야 합니다.')
            return
        }
        if (newPassword !== newPasswordConfirm) {
            error('새 비밀번호가 일치하지 않습니다.')
            return
        }
        try {
            await changePassword({ currentPassword, newPassword })
            setCurrentPassword('')
            setNewPassword('')
            setNewPasswordConfirm('')
            success('비밀번호가 변경되었습니다.')
        } catch (e: any) {
            error(`비밀번호 변경 실패: ${e.response?.data?.message ?? e.message}`)
        }
    }

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '320px' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '16px' }}>내 정보</h3>

                <div style={{ marginBottom: '8px', fontSize: '12px', color: '#888' }}>아이디: {user?.username}</div>

                <div style={{ borderTop: '1px solid #333', paddingTop: '12px', marginBottom: '16px' }}>
                    <h4 style={{ fontSize: '13px', color: '#AAA', margin: '0 0 8px 0' }}>닉네임 변경</h4>
                    <div style={{ display: 'flex', gap: '6px' }}>
                        <input
                            value={nickname}
                            onChange={e => setNickname(e.target.value.replace(/[^a-z0-9가-힣]/g, ''))}
                            style={{ ...styles.input, flex: 1 }}
                        />
                        <button onClick={handleChangeNickname} style={styles.actionButton}>변경</button>
                    </div>
                    <div style={{ fontSize: '11px', color: nicknameValid ? '#39A54A' : '#FF6347', marginTop: '4px' }}>
                        {nicknameValid ? '✔ 사용 가능한 형식' : '2~10자, 영문 소문자·한글·숫자만'}
                    </div>
                </div>

                <div style={{ borderTop: '1px solid #333', paddingTop: '12px' }}>
                    <h4 style={{ fontSize: '13px', color: '#AAA', margin: '0 0 8px 0' }}>비밀번호 변경</h4>
                    <input
                        type="password"
                        value={currentPassword}
                        onChange={e => setCurrentPassword(e.target.value)}
                        placeholder="현재 비밀번호"
                        style={{ ...styles.input, marginBottom: '6px' }}
                    />
                    <input
                        type="password"
                        value={newPassword}
                        onChange={e => setNewPassword(e.target.value)}
                        placeholder="새 비밀번호"
                        style={{ ...styles.input, marginBottom: '6px' }}
                    />
                    <input
                        type="password"
                        value={newPasswordConfirm}
                        onChange={e => setNewPasswordConfirm(e.target.value)}
                        placeholder="새 비밀번호 확인"
                        style={{ ...styles.input, marginBottom: '6px' }}
                    />
                    <div style={{ fontSize: '11px', color: '#888', marginBottom: '8px' }}>
                        8자 이상, 소문자·숫자·특수문자(!@#$%^&*) 모두 포함
                    </div>
                    <button onClick={handleChangePassword} style={{ ...styles.actionButton, width: '100%' }}>비밀번호 변경</button>
                </div>

                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    input: { padding: '8px', fontSize: '13px', borderRadius: '6px', border: '1px solid #333', backgroundColor: '#222', color: '#FFF', width: '100%', boxSizing: 'border-box' as const },
    actionButton: { padding: '8px 12px', fontSize: '13px', borderRadius: '6px', border: 'none', backgroundColor: '#4F9DFF', color: '#FFF', cursor: 'pointer', fontWeight: 600 },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '16px', width: '100%' },
} as const
