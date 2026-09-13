import instance from './axios'
import type { UserDto } from '../types/user'
import type { RankHistoryResponse } from '../types/rank'

export interface ChangePasswordRequest {
    currentPassword: string
    newPassword: string
}

export interface ChangeNicknameRequest {
    nickname: string
}

export async function changePassword(req: ChangePasswordRequest): Promise<UserDto> {
    const res = await instance.patch<UserDto>('/users/password', req)
    return res.data
}

export async function changeNickname(req: ChangeNicknameRequest): Promise<UserDto> {
    const res = await instance.patch<UserDto>('/users/nickname', req)
    return res.data
}

export async function updateProfileImage(file: File): Promise<UserDto> {
    const formData = new FormData()
    formData.append('image', file)
    const res = await instance.post<UserDto>('/users/profile-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
}

export async function getRankHistory(page: number, size: number): Promise<RankHistoryResponse> {
    const res = await instance.get<RankHistoryResponse>('/users/rank/history', {
        params: { page, size },
    })
    return res.data
}
