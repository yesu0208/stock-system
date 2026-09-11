export type RankTier = 'UNRANKED' | 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND' | 'MASTER'

export interface RankResponse {
    username: string
    tier: RankTier
    subTier: number | null
    rp: number
    highestTierReached: RankTier
}
