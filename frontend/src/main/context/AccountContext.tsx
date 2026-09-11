import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import type { AccountResponse } from '../../types/account'

interface AccountContextValue {
    account: AccountResponse | null
}

const AccountContext = createContext<AccountContextValue | null>(null)

export function AccountProvider({ children }: { children: ReactNode }) {
    const [account, setAccount] = useState<AccountResponse | null>(null)
    const { subscribeDestination } = useRealtime()

    useEffect(() => {
        return subscribeDestination('/user/sub/account', (data: AccountResponse) => {
            setAccount(data)
        })
    }, [subscribeDestination])

    return (
        <AccountContext.Provider value={{ account }}>
            {children}
        </AccountContext.Provider>
    )
}

export function useAccount() {
    const ctx = useContext(AccountContext)
    if (!ctx) throw new Error('useAccount는 AccountProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
