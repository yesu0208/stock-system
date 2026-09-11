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

/**
 * react-router(Routes/Route/Navigate/PrivateRoute)를 완전히
 * 걷어내고, isLoggedIn / authView 두 개의 로컬 state로 화면을 직접 전환
 *
 * 의도적으로 tokenStorage.subscribe()를 여기서 구독하지 않음.
 * MainLayout.tsx가 이미 "세션 만료" 모달을 띄운 뒤 사용자가 확인 버튼을
 * 눌러야 로그인 화면으로 돌아가는 UX를 갖고 있는데, 여기서 토큰 삭제를
 * 실시간으로 감지해 isLoggedIn을 바로 false로 내리면 모달이 뜨기도 전에
 * MainLayout이 언마운트되어버림. 그래서 "로그아웃/만료로 인한 화면 전환"은
 * MainLayout이 onLoggedOut 콜백을 호출하는 시점에만 일어나도록 유지
 * (최초 렌더링 시 자동 로그인 판단에만 tokenStorage.get()을 1회성으로 사용)
 *
 * TradePage.tsx의 종목 선택기는 아직 constants/stocks.ts(10종목)를
 * 그대로 사용. StockProvider(전체 카탈로그)는 이번 단계에서 트리에
 * 마운트만 해두고, 실제 연결은 이후(OrderBook/TradingChart 도입)에서 함.
 */
export default function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(() => !!tokenStorage.get())
    const [authView, setAuthView] = useState<'login' | 'signup'>('login')

    if (!isLoggedIn) {
        return authView === 'login' ? (
            <LoginPage
                onLoginSuccess={() => setIsLoggedIn(true)}
                onNavigateToSignup={() => setAuthView('signup')}
            />
        ) : (
            <SignupPage onNavigateToLogin={() => setAuthView('login')} />
        )
    }

    return (
        <RealtimeProvider>
            <UserProvider>
                <StockProvider>
                    <AccountProvider>
                        <PortfolioProvider>
                            <ChartDataProvider>
                                <MarketDataProvider>
                                    <AlertProvider>
                                        <WatchListProvider>
                                            <MainLayout onLoggedOut={() => setIsLoggedIn(false)}>
                                                <TradePage />
                                            </MainLayout>
                                        </WatchListProvider>
                                    </AlertProvider>
                                </MarketDataProvider>
                            </ChartDataProvider>
                        </PortfolioProvider>
                    </AccountProvider>
                </StockProvider>
        </UserProvider>
        </RealtimeProvider>
    )
}
