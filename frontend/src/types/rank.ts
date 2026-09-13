export type RankTier = 'UNRANKED' | 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND' | 'MASTER'

export interface RankResponse {
    username: string
    tier: RankTier
    subTier: number | null
    rp: number
    highestTierReached: RankTier
    currentRankMinRp: number
    nextRankMinRp: number | null
}

export interface RankHistoryItem {
    date: string
    tier: RankTier
    subTier: number | null
    rp: number
    rpChange: number
}

export interface RankHistoryResponse {
    items: RankHistoryItem[]
    hasNext: boolean
}
