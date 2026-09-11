import { createContext, useCallback, useContext, useState } from 'react'
import type { ReactNode } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import './Toast.css'

export type ToastVariant = 'success' | 'error' | 'info'

interface ToastItem {
    id: number
    message: string
    variant: ToastVariant
}

interface ToastContextValue {
    showToast: (message: string, variant?: ToastVariant) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

let toastSeq = 0

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toasts, setToasts] = useState<ToastItem[]>([])

    const showToast = useCallback((message: string, variant: ToastVariant = 'info') => {
        const id = ++toastSeq
        setToasts(prev => [...prev, { id, message, variant }])

        window.setTimeout(() => {
            setToasts(prev => prev.filter(t => t.id !== id))
        }, 3000)
    }, [])

    return (
        <ToastContext.Provider value={{ showToast }}>
            {children}

            <div className="toast-stack">
                <AnimatePresence>
                    {toasts.map(t => (
                        <motion.div
                            key={t.id}
                            className={`toast-item toast-${t.variant}`}
                            initial={{ opacity: 0, y: -12, scale: 0.96 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, x: 40, transition: { duration: 0.2 } }}
                            transition={{ duration: 0.25, ease: 'easeOut' }}
                        >
                            {t.message}
                        </motion.div>
                    ))}
                </AnimatePresence>
            </div>
        </ToastContext.Provider>
    )
}

export function useToast() {
    const ctx = useContext(ToastContext)
    if (!ctx) throw new Error('useToast는 ToastProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
