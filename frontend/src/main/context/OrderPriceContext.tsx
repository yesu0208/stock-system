import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'

interface OrderPriceContextValue {
    /** 호가창에서 클릭한 가격. 아직 클릭 전이면 null */
    selectedPrice: number | null
    /** AskList/BidList에서 가격 클릭 시 호출 -> selectedPrice 갱신 */
    setSelectedPrice: (price: number) => void
}

const OrderPriceContext = createContext<OrderPriceContextValue | undefined>(undefined)

export function OrderPriceProvider({ children }: { children: ReactNode }) {
    const [selectedPrice, setSelectedPrice] = useState<number | null>(null)

    return (
        <OrderPriceContext.Provider value={{ selectedPrice, setSelectedPrice }}>
            {children}
        </OrderPriceContext.Provider>
    )
}

export function useOrderPrice() {
    const ctx = useContext(OrderPriceContext)
    if (!ctx) {
        throw new Error('useOrderPrice는 OrderPriceProvider 내부에서만 사용할 수 있습니다.')
    }
    return ctx
}
