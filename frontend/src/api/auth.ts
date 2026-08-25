import axios from './axios'
import { tokenStorage } from '../utils/token'
import { disconnectStomp } from './stompClient'

export interface SignUpRequest { username: string; nickname: string; password: string } // [수정] nickname 추가
export interface UserDto { id: number; username: string }
export interface LoginRequest { username: string; password: string }
export interface LoginResponse { accessToken: string }

// 회원가입
export async function signUp(req: SignUpRequest): Promise<UserDto> {
    const res = await axios.post<UserDto>('/users', req)
    return res.data
}

// 로그인 (JWT)
export async function login(req: LoginRequest): Promise<LoginResponse> {
    const res = await axios.post<LoginResponse>('/users/authenticate', req)
    tokenStorage.set(res.data.accessToken) // Broadcast + localStorage
    return res.data
}

// username 중복 체크
export const checkUsernameAPI = async (username: string): Promise<{ exists: boolean }> => {
    const response = await axios.get(`/users/check-username`, { params: { username } })
    return response.data
}

// [신규] nickname 중복 체크
export const checkNicknameAPI = async (nickname: string): Promise<{ exists: boolean }> => {
    const response = await axios.get(`/users/check-nickname`, { params: { nickname } })
    return response.data
}

// 로그아웃
export async function logout() {
    await axios.post('/users/logout')

    // 1) 토큰 제거
    tokenStorage.clear()

    // 2) STOMP 연결 종료
    disconnectStomp()
}