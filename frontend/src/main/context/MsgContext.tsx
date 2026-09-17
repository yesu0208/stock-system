import { createContext, useContext, useMemo } from 'react' // [수정] useMemo 추가
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

    const value: MsgContextValue = useMemo(() => ({
        success: (message: string) => showToast(message, 'success'),
        error:   (message: string) => showToast(message, 'error'),
        info:    (message: string) => showToast(message, 'info'),
    }), [showToast])

    return <MsgContext.Provider value={value}>{children}</MsgContext.Provider>
}

export function useMsg() {
    const ctx = useContext(MsgContext)
    if (!ctx) throw new Error('useMsg는 MsgProvider 내부에서만 사용할 수 있습니다 (ToastProvider 하위 필수).')
    return ctx
}
