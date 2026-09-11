import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useRealtime } from './RealtimeContext'
import type { PortfolioResponse } from '../../types/portfolio'

/**
 * PortfolioContext
 * 백엔드 PortfolioPushService.java: '/user/sub/portfolio'로
 * 섹터/종목별 자산 배분 데이터가 push. AccountContext와
 * 같은 계좌 데이터에서 파생되지만 별개 목적(자산 배분 시각화)이라
 * 별도 Context로 분리
 * RealtimeProvider 하위에 위치
 */

interface PortfolioContextValue {
    portfolio: PortfolioResponse | null
}

const PortfolioContext = createContext<PortfolioContextValue | null>(null)

export function PortfolioProvider({ children }: { children: ReactNode }) {
    const [portfolio, setPortfolio] = useState<PortfolioResponse | null>(null)
    const { subscribeDestination } = useRealtime()

    useEffect(() => {
        return subscribeDestination('/user/sub/portfolio', (data: PortfolioResponse) => {
            setPortfolio(data)
        })
    }, [subscribeDestination])

    return (
        <PortfolioContext.Provider value={{ portfolio }}>
            {children}
        </PortfolioContext.Provider>
    )
}

export function usePortfolio() {
    const ctx = useContext(PortfolioContext)
    if (!ctx) throw new Error('usePortfolio는 PortfolioProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
