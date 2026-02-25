import axios, { AxiosError } from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // refreshToken 쿠키 전송
})

let isRefreshing = false
let refreshQueue: ((token: string) => void)[] = []

// 요청 인터셉터
api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // refresh 요청은 Authorization 헤더 제외
        if (!config.url?.includes('/auth/refresh')) {
            const token = localStorage.getItem('accessToken')
            if (token) {
                config.headers.Authorization = `Bearer ${token}`
            }
        }
        return config
    },
    error => Promise.reject(error)
)

// 응답 인터셉터
api.interceptors.response.use(
    response => response,
    async (error: AxiosError) => {
        const originalRequest: any = error.config

        // 401 + 재시도 안 했고 + refresh 요청 아님
        if (
            error.response?.status === 401 &&
            !originalRequest?._retry &&
            !originalRequest?.url?.includes('/auth/refresh')
        ) {
            originalRequest._retry = true

            // 이미 refresh 중이면 큐에 쌓기
            if (isRefreshing) {
                return new Promise(resolve => {
                    refreshQueue.push(token => {
                        originalRequest.headers = {
                            ...originalRequest.headers,
                            Authorization: `Bearer ${token}`,
                        }
                        resolve(api(originalRequest))
                    })
                })
            }

            isRefreshing = true

            try {
                // refresh token으로 accessToken 재발급
                const refreshRes = await axios.post<{ accessToken: string }>(
                    `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
                    {},
                    { withCredentials: true }
                )

                const newAccessToken = refreshRes.data.accessToken
                localStorage.setItem('accessToken', newAccessToken)

                // 대기 중이던 요청들 재개
                refreshQueue.forEach(cb => cb(newAccessToken))
                refreshQueue = []

                // 원래 요청 재시도
                originalRequest.headers = {
                    ...originalRequest.headers,
                    Authorization: `Bearer ${newAccessToken}`,
                }

                return api(originalRequest)
            } catch (refreshError) {
                // refresh 실패 → 강제 로그아웃
                localStorage.removeItem('accessToken')
                refreshQueue = []

                // 필요 시 로그인 페이지 이동
                window.location.href = '/login'

                return Promise.reject(refreshError)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(error)
    }
)

export default api
