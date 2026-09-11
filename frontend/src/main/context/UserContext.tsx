import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import instance from '../../api/axios'
import type { UserDto } from '../../types/user'

/**
 * UserContext
 * 기존의 TradePage.tsx가 마운트 시 직접 instance.get('/users/user')를
 * 호출해 userInfo를 로컬 state로 관리하던 것을 전역 Context로 승격
 * 백엔드 UserController.getUser()가 이제 랭크(RankResponse)까지 함께
 * 내려주므로(UserDto.withRank), 유저 정보와 랭크를 한 번에 가져옴.
 *
 * 로그인 상태에서만 의미가 있으므로 App.tsx의 isLoggedIn === true
 * 분기 안에서 마운트
 */

interface UserContextValue {
    user: UserDto | null
    loading: boolean
    refresh: () => void
}

const UserContext = createContext<UserContextValue | null>(null)

export function UserProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<UserDto | null>(null)
    const [loading, setLoading] = useState(true)

    const fetchUser = () => {
        setLoading(true)
        instance.get<UserDto>('/users/user')
            .then(res => setUser(res.data))
            .catch(err => console.error('유저 조회 실패:', err))
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        fetchUser()
    }, [])

    return (
        <UserContext.Provider value={{ user, loading, refresh: fetchUser }}>
            {children}
        </UserContext.Provider>
    )
}

export function useUser() {
    const ctx = useContext(UserContext)
    if (!ctx) throw new Error('useUser는 UserProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
