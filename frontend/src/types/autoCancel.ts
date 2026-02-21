export type AutoCancelResultResponse = {
    responseType: 'SUCCESS' | 'ERROR'
    autoOrderId: number
    username: string
    stockCode: string
    autoOrderType: 'BUY' | 'SELL'
    triggerPrice: number
    orderPrice: number
    orderQuantity: number
    errorMessage?: string | null
}
