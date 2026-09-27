export interface MarketHolidayResponse {
    holidayDate: string
    memo: string | null
}

export interface MarketHolidayCreateRequest {
    holidayDate: string
    memo?: string
}
