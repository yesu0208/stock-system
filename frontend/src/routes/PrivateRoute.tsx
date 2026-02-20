import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { tokenStorage } from '../utils/token'

export default function PrivateRoute({ children }: { children: ReactNode }) {
    const token = tokenStorage.get()
    if (!token) return <Navigate to="/login" replace />
    return <>{children}</>
}
