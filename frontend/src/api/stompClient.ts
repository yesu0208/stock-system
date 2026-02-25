import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '../utils/token'

let stockClient: Client | null = null
let orderClient: Client | null = null

// 기존 연결 종료
export function disconnectStomp() {
    if (stockClient && stockClient.active) {
        stockClient.deactivate()
        stockClient = null
    }
    if (orderClient && orderClient.active) {
        orderClient.deactivate()
        orderClient = null
    }
}

export function getStockClient(): Client {
    if (stockClient) return stockClient

    const token = tokenStorage.get()
    if (!token) throw new Error('JWT token not found')

    stockClient = new Client({
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_WS_BASE_URL}/ws-stock`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        debug: (str) => console.log('[STOCK]', str),
        reconnectDelay: 5000,
    })

    return stockClient
}

export function getOrderClient(): Client {
    if (orderClient) return orderClient

    const token = tokenStorage.get()
    if (!token) throw new Error('JWT token not found')

    orderClient = new Client({
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_WS_BASE_URL}/ws-order`),
        connectHeaders: { Authorization: `Bearer ${token}` },
        debug: (str) => console.log('[ORDER]', str),
        reconnectDelay: 5000,
    })

    return orderClient
}
