import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '../utils/token'
import instance from './axios'
import type { UserDto } from './auth'

let stockClient: Client | null = null
let orderClient: Client | null = null

function createClient(endpoint: string, debugLabel: string): Client {
    const client = new Client({
        webSocketFactory: () =>
            new SockJS(`${import.meta.env.VITE_WS_BASE_URL}${endpoint}`),
        connectHeaders: {
            Authorization: `Bearer ${tokenStorage.get()}`,
        },
        debug: (str) => console.log(`[${debugLabel}]`, str),
        reconnectDelay: 5000,
    })

    client.onStompError = async () => {
        console.log(`[${debugLabel}] STOMP ERROR → refresh 트리거 시도`)
        try {
            await instance.get<UserDto>('/users/user')
            console.log(`[${debugLabel}] Refresh 성공 → STOMP 재연결`)
            await reconnectStomp()
        } catch (e) {
            console.log(`[${debugLabel}] Refresh 실패 → 로그아웃 상태`)
            await disconnectStomp()
        }
    }

    return client
}

function updateConnectHeaders(client: Client) {
    const token = tokenStorage.get()
    if (!token) return
    client.connectHeaders = {
        Authorization: `Bearer ${token}`,
    }
}

async function reconnectClient(client: Client | null) {
    if (!client) return
    updateConnectHeaders(client)
    if (client.active) {
        await client.deactivate()
    }
    client.activate()
}

export function getStockClient(): Client {
    if (!stockClient) {
        stockClient = createClient('/ws-stock', 'STOCK')
    }
    return stockClient
}

export function getOrderClient(): Client {
    if (!orderClient) {
        orderClient = createClient('/ws-order', 'ORDER')
    }
    return orderClient
}

export async function reconnectStomp() {
    await reconnectClient(stockClient)
    await reconnectClient(orderClient)
}

export async function disconnectStomp() {
    if (stockClient) {
        await stockClient.deactivate()
        stockClient = null
    }
    if (orderClient) {
        await orderClient.deactivate()
        orderClient = null
    }
}
