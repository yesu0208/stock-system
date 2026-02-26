import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '../utils/token'
import instance from './axios'
import type {UserDto} from "../types/user.ts"; // refresh 트리거용 HTTP 요청

let stockClient: Client | null = null
let orderClient: Client | null = null

// -----------------------------
// 내부 공통 함수
// -----------------------------
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

    // 🔥 STOMP ERROR 발생 시 refresh 트리거
    client.onStompError = async () => {
        console.log(`[${debugLabel}] STOMP ERROR → refresh 트리거 시도`)

        try {
            // 보호된 API 호출로 refresh 트리거
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

// -----------------------------
// 외부 공개 API
// -----------------------------
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

// 🔥 토큰 refresh 후 호출할 함수
export async function reconnectStomp() {
    await reconnectClient(stockClient)
    await reconnectClient(orderClient)
}

// 로그아웃 시 완전 종료
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