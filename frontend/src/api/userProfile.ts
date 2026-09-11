import instance from './axios'
import type { UserDto } from '../types/user'

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
