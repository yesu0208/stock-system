export interface UserDto {
    userId: number
    username: string
    nickname: string // [신규] 백엔드 UserDto에 맞춰 nickname 필드 추가
    createdDateTime: string
}