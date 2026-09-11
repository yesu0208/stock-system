export type StockTalkMessageType = 'ENTER' | 'CHAT' | 'LEAVE'

export interface StockTalkMessage {
    type: StockTalkMessageType
    ticker: string
    sender: string
    senderNickname: string
    senderProfileImageUrl: string | null
    content: string
    sentAt: string
    participantCount: number
}

export interface StockTalkJoinResponse {
    ticker: string
    participantCount: number
    messages: StockTalkMessage[]
}
