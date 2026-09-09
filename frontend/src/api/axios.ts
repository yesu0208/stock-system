import axios from 'axios'
import type { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios'
import { tokenStorage } from '../utils/token'
import { disconnectStomp } from './stompClient'

interface CustomAxiosRequestConfig extends AxiosRequestConfig {
    _retry?: boolean
}

const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true,
})

let isRefreshing = false
let refreshQueue: {
    resolve: (token: string) => void
    reject: (error: unknown) => void
}[] = []

instance.interceptors.request.use(config => {
    if (!config.url?.includes('/auth/refresh')) {
        const token = tokenStorage.get()
        if (token && token !== 'undefined') {
            config.headers = config.headers ?? {}
            config.headers.Authorization = `Bearer ${token}`
        } else if (config.headers) {
            delete config.headers.Authorization
        }
    }
    return config
})

instance.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config as CustomAxiosRequestConfig

        if (
            error.response?.status === 401 &&
            originalRequest?.url?.includes('/auth/refresh')
        ) {
            setTimeout(() => tokenStorage.clear(), 0)
            return Promise.reject(error)
        }

        if (
            error.response?.status === 401 &&
            originalRequest &&
            !originalRequest._retry
        ) {
            originalRequest._retry = true

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    refreshQueue.push({
                        resolve: (token: string) => {
                            originalRequest.headers = originalRequest.headers ?? {}
                            originalRequest.headers.Authorization = `Bearer ${token}`
                            resolve(instance(originalRequest))
                        },
                        reject,
                    })
                })
            }

            isRefreshing = true

            try {
                const refreshRes = await instance.post<{ accessToken: string }>(
                    '/auth/refresh',
                    {}
                )
                const newAccessToken = refreshRes.data.accessToken
                tokenStorage.set(newAccessToken)

                refreshQueue.forEach(p => p.resolve(newAccessToken))
                refreshQueue = []

                originalRequest.headers = originalRequest.headers ?? {}
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`

                return instance(originalRequest)
            } catch (refreshError) {
                setTimeout(() => {
                    tokenStorage.clear()
                    disconnectStomp()
                }, 0)
                refreshQueue.forEach(p => p.reject(refreshError))
                refreshQueue = []
                return Promise.reject(refreshError)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(error)
    }
)

export default instance
