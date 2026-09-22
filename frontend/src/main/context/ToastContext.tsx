import { createContext, useCallback, useContext, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { FiX } from 'react-icons/fi'
import './Toast.css'

export type ToastVariant = 'success' | 'error' | 'info' | 'buy' | 'sell'

interface ToastItem {
    id: number
    message: string
    variant: ToastVariant
}

interface ToastContextValue {
    showToast: (message: string, variant?: ToastVariant) => void
    isVisible: boolean
    toggleVisible: () => void
    toastCount: number
}

const ToastContext = createContext<ToastContextValue | null>(null)

let toastSeq = 0

export function ToastProvider({ children }: { children: ReactNode }) {
    const [toasts, setToasts] = useState<ToastItem[]>([])
    const [isVisible, setIsVisible] = useState(true)
    const [unreadCount, setUnreadCount] = useState(0)

    const isVisibleRef = useRef(isVisible)
    isVisibleRef.current = isVisible

    const showToast = useCallback((message: string, variant: ToastVariant = 'info') => {
        const id = ++toastSeq
        setToasts(prev => [...prev, { id, message, variant }])

        if (!isVisibleRef.current) {
            setUnreadCount(prev => prev + 1)
        }

        window.setTimeout(() => {
            setToasts(prev => prev.filter(t => t.id !== id))
        }, 5000)
    }, [])

    const toggleVisible = useCallback(() => {
        setIsVisible(prev => {
            const next = !prev
            if (next) {
                setUnreadCount(0)
            }
            return next
        })
    }, [])

    return (
        <ToastContext.Provider
            value={{ showToast, isVisible, toggleVisible, toastCount: unreadCount }}
        >
            {children}

            {isVisible && (
                <div className="toast-stack">
                    {toasts.length > 0 && (
                        <button
                            className="toast-stack__close-btn"
                            onClick={toggleVisible}
                            aria-label="메시지 창 숨기기"
                        >
                            <FiX />
                        </button>
                    )}

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
            )}
        </ToastContext.Provider>
    )
}

export function useToast() {
    const ctx = useContext(ToastContext)
    if (!ctx) throw new Error('useToast는 ToastProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
