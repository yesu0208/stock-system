import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '../utils/token'

let stockClient: Client | null = null
let orderClient: Client | null = null

export function getStockClient(): Client {
    if (stockClient) return stockClient

    const token = tokenStorage.get()
    if (!token) throw new Error('JWT token not found')

    stockClient = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws-stock'),
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
        webSocketFactory: () => new SockJS('http://localhost:8080/ws-order'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        debug: (str) => console.log('[ORDER]', str),
        reconnectDelay: 5000,
    })

    return orderClient
}
