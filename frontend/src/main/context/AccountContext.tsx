import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { StompSubscription } from '@stomp/stompjs'
import { getStockClient } from '../../api/stompClient'
import { tokenStorage } from '../../utils/token'
import type { AccountResponse } from '../../types/account'

interface AccountContextValue {
    account: AccountResponse | null
    connected: boolean
}

const AccountContext = createContext<AccountContextValue | null>(null)

export function AccountProvider({ children }: { children: ReactNode }) {
    const [account, setAccount] = useState<AccountResponse | null>(null)
    const [connected, setConnected] = useState(false)

    useEffect(() => {
        if (!tokenStorage.get()) return

        const client = getStockClient()
        let sub: StompSubscription | undefined

        client.onConnect = () => {
            setConnected(true)
            sub = client.subscribe('/user/sub/account', (message) => {
                const data: AccountResponse = JSON.parse(message.body)
                setAccount(data)
            })
        }
        client.onWebSocketClose = () => setConnected(false)

        client.activate()

        return () => {
            sub?.unsubscribe()
            client.deactivate()
        }
    }, [])

    return (
        <AccountContext.Provider value={{ account, connected }}>
            {children}
        </AccountContext.Provider>
    )
}

export function useAccount() {
    const ctx = useContext(AccountContext)
    if (!ctx) throw new Error('useAccount는 AccountProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
