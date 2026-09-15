import { useState } from 'react'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import TradePage from './pages/TradePage'
import MainLayout from './layouts/MainLayout'
import { tokenStorage } from './utils/token'

import { RealtimeProvider } from './main/context/RealtimeContext'
import { StockProvider } from './main/context/StockContext'
import { AccountProvider } from './main/context/AccountContext'
import { MarketDataProvider } from './main/context/MarketDataContext'
import { AlertProvider } from './main/context/AlertContext'
import { WatchListProvider } from './main/context/WatchListContext'
import { UserProvider } from './main/context/UserContext'
import { PortfolioProvider } from './main/context/PortfolioContext'
import { ChartDataProvider } from './main/context/ChartDataContext'
import {StockRealtimeProvider} from "./main/context/StockRealtimeContext.tsx";
import {OrderPriceProvider} from "./main/context/OrderPriceContext.tsx";
import { PendingOrderProvider } from './main/context/PendingOrderContext'

/**
 * RealtimeProvider / MarketDataProvider를 isLoggedIn 분기
 * 바깥(최상단)으로 이동. 로그인 전에도 STOMP 연결(익명)이 열려 있어야
 * 로그인 화면에서 실시간 KOSPI/KOSDAQ 시세를 보여줄 수 있기 때문.
 *
 * 반면 UserProvider/AccountProvider 등 로그인 후에만 의미 있는(인증 필요)
 * Provider들은 기존처럼 isLoggedIn === true 분기 안에만 유지.
 */
export default function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(() => !!tokenStorage.get())
    const [authView, setAuthView] = useState<'login' | 'signup'>('login')

    return (
        <RealtimeProvider>
            <MarketDataProvider>
                {!isLoggedIn ? (
                    authView === 'login' ? (
                        <LoginPage
                            onLoginSuccess={() => setIsLoggedIn(true)}
                            onNavigateToSignup={() => setAuthView('signup')}
                        />
                    ) : (
                        <SignupPage onNavigateToLogin={() => setAuthView('login')} />
                    )
                ) : (
                    <UserProvider>
                        <StockProvider>
                            <StockRealtimeProvider>
                                <OrderPriceProvider>
                                    <AccountProvider>
                                        <PendingOrderProvider>
                                            <PortfolioProvider>
                                                <ChartDataProvider>
                                                    <AlertProvider>
                                                        <WatchListProvider>
                                                            <MainLayout onLoggedOut={() => setIsLoggedIn(false)}>
                                                                <TradePage />
                                                            </MainLayout>
                                                        </WatchListProvider>
                                                    </AlertProvider>
                                                </ChartDataProvider>
                                            </PortfolioProvider>
                                        </PendingOrderProvider>
                                    </AccountProvider>
                                </OrderPriceProvider>
                            </StockRealtimeProvider>
                        </StockProvider>
                    </UserProvider>
                )}
            </MarketDataProvider>
        </RealtimeProvider>
    )
}
