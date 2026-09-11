import { createContext, useContext } from 'react'
import type { ReactNode } from 'react'
import { useToast } from './ToastContext'

interface MsgContextValue {
    success: (message: string) => void
    error: (message: string) => void
    info: (message: string) => void
}

const MsgContext = createContext<MsgContextValue | null>(null)

export function MsgProvider({ children }: { children: ReactNode }) {
    const { showToast } = useToast()

    const value: MsgContextValue = {
        success: (message) => showToast(message, 'success'),
        error:   (message) => showToast(message, 'error'),
        info:    (message) => showToast(message, 'info'),
    }

    return <MsgContext.Provider value={value}>{children}</MsgContext.Provider>
}

export function useMsg() {
    const ctx = useContext(MsgContext)
    if (!ctx) throw new Error('useMsg는 MsgProvider 내부에서만 사용할 수 있습니다 (ToastProvider 하위 필수).')
    return ctx
}
