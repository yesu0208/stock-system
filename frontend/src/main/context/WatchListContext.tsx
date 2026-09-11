import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { getWatchList, addWatchList, removeWatchList } from '../../api/watchList'
import { useMsg } from './MsgContext'
import type { WatchListItem } from '../../types/watchList'

interface WatchListContextValue {
    watchList: WatchListItem[]
    loading: boolean
    isWatched: (stockCode: string) => boolean
    addStock: (stockCode: string, stockName: string) => Promise<void>
    removeStock: (stockCode: string) => Promise<void>
}

const WatchListContext = createContext<WatchListContextValue | null>(null)

export function WatchListProvider({ children }: { children: ReactNode }) {
    const [watchList, setWatchList] = useState<WatchListItem[]>([])
    const [loading, setLoading] = useState(true)
    const { success, error } = useMsg()

    useEffect(() => {
        let cancelled = false

        getWatchList()
            .then(data => { if (!cancelled) setWatchList(data) })
            .catch(() => { if (!cancelled) setWatchList([]) })
            .finally(() => { if (!cancelled) setLoading(false) })

        return () => { cancelled = true }
    }, [])

    const isWatched = useCallback(
        (stockCode: string) => watchList.some(w => w.stockCode === stockCode),
        [watchList]
    )

    const addStock = useCallback(async (stockCode: string, stockName: string) => {
        try {
            const saved = await addWatchList({ stockCode, stockName })
            setWatchList(prev => [...prev, saved])
            success(`${stockName} 관심종목에 추가했습니다.`)
        } catch (e) {
            error('관심종목 추가에 실패했습니다.')
            throw e
        }
    }, [success, error])

    const removeStock = useCallback(async (stockCode: string) => {
        const target = watchList.find(w => w.stockCode === stockCode)
        try {
            await removeWatchList(stockCode)
            setWatchList(prev => prev.filter(w => w.stockCode !== stockCode))
            if (target) success(`${target.stockName} 관심종목에서 제거했습니다.`)
        } catch (e) {
            error('관심종목 삭제에 실패했습니다.')
            throw e
        }
    }, [watchList, success, error])

    return (
        <WatchListContext.Provider value={{ watchList, loading, isWatched, addStock, removeStock }}>
            {children}
        </WatchListContext.Provider>
    )
}

export function useWatchList() {
    const ctx = useContext(WatchListContext)
    if (!ctx) throw new Error('useWatchList는 WatchListProvider 내부에서만 사용할 수 있습니다.')
    return ctx
}
