import type { RankResponse } from './rank'

export type Role = 'USER' | 'ADMIN'

export interface UserDto {
    userId: number
    username: string
    nickname: string
    createdDateTime: string
    role: Role
    rank: RankResponse | null
    profileImageUrl: string | null
}
