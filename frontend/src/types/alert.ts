export type AlertDirection = 'ABOVE' | 'BELOW'

export interface AlertResponseMessage {
    alertId: number
    username: string
    stockCode: string
    direction: AlertDirection
    triggerPrice: number
    registeredTime: string
}

export interface AlertFiredResponse {
    alertId: number
    username: string
    stockCode: string
    direction: AlertDirection
    triggerPrice: number
    currentPrice: number
    firedTime: string
}
