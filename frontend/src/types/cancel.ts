export type CancelResultResponse = {
    responseType: 'SUCCESS' | 'ERROR'
    orderId: number
    username: string
    stockCode: string
    orderType: 'BUY' | 'SELL'
    orderPrice: number
    orderQuantity: number
    errorMessage?: string | null
}
