export type OtocoEntryDirection = 'ABOVE' | 'BELOW'
export type OtocoExitMode = 'PRICE' | 'PCT'
export type OtocoStatus = 'WAITING_ENTRY' | 'ENTRY_ORDER_PLACED' | 'WAITING_EXIT' | 'COMPLETED' | 'CANCELED'
export type LeverageRatio = 'SPOT' | 'X1_5' | 'X2' | 'X2_5'

export interface OtocoRequest {
    stockCode: string
    entryDirection: OtocoEntryDirection
    orderQuantity: number
    entryTriggerPrice: number
    tpMode: OtocoExitMode
    tpPrice?: number | null
    tpPct?: number | null
    slMode: OtocoExitMode
    slPrice?: number | null
    slPct?: number | null
    leverageRatio?: LeverageRatio | null
}

export interface OtocoResponse extends OtocoRequest {
    username: string
}

export interface OtocoResultResponse {
    responseType: 'SUCCESS' | 'ERROR'
    otocoId: number | null
    username: string
    stockCode: string
    entryDirection: OtocoEntryDirection
    leverageRatio: LeverageRatio | null
    orderQuantity: number
    entryTriggerPrice: number
    tpTriggerPrice: number | null
    slTriggerPrice: number | null
    otocoStatus: OtocoStatus | null
    orderTime: string
    errorMessage: string | null
}

export interface OtocoResponseMessage {
    otocoId: number
    username: string
    stockCode: string
    entryDirection: OtocoEntryDirection
    leverageRatio: LeverageRatio | null
    orderQuantity: number
    entryTriggerPrice: number
    tpTriggerPrice: number | null
    slTriggerPrice: number | null
    otocoStatus: OtocoStatus
    orderTime: string
}
