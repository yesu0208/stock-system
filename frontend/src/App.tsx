import { Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import TradePage from './pages/TradePage'
import PrivateRoute from './routes/PrivateRoute'

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route
                path="/trade"
                element={
                    <PrivateRoute>
                        <TradePage />
                    </PrivateRoute>
                }
            />
            <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
    )
}
