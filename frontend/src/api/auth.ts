import axios from './axios'
import { tokenStorage } from '../utils/token'
import { disconnectStomp } from './stompClient'

export interface SignUpRequest { username: string; nickname: string; password: string }
export interface UserDto { userId: number; username: string; nickname: string; createdDateTime: string }
export interface LoginRequest { username: string; password: string }
export interface LoginResponse { accessToken: string }

export async function signUp(req: SignUpRequest): Promise<UserDto> {
    const res = await axios.post<UserDto>('/users', req)
    return res.data
}

export async function login(req: LoginRequest): Promise<LoginResponse> {
    const res = await axios.post<LoginResponse>('/users/authenticate', req)
    tokenStorage.set(res.data.accessToken)
    return res.data
}

export const checkUsernameAPI = async (username: string): Promise<{ exists: boolean }> => {
    const response = await axios.get(`/users/check-username`, { params: { username } })
    return response.data
}

export const checkNicknameAPI = async (nickname: string): Promise<{ exists: boolean }> => {
    const response = await axios.get(`/users/check-nickname`, { params: { nickname } })
    return response.data
}

export async function logout() {
    await axios.post('/users/logout')
    tokenStorage.clear()
    disconnectStomp()
}
