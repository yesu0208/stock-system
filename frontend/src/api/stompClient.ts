import { Client } from '@stomp/stompjs'
import type { IFrame } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '../utils/token'
import instance from './axios'
import type { UserDto } from './auth'

let stockClient: Client | null = null
let orderClient: Client | null = null

function createClient(endpoint: string, debugLabel: string): Client {
    const client = new Client({
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_WS_BASE_URL}${endpoint}`),
        connectHeaders: { Authorization: `Bearer ${tokenStorage.get()}` },
        debug: (str) => console.log(`[${debugLabel}]`, str),
        reconnectDelay: 5000,
    })

    client.onStompError = async () => {
        console.log(`[${debugLabel}] STOMP ERROR → refresh 트리거 시도`)
        try {
            await instance.get<UserDto>('/users/user')
            console.log(`[${debugLabel}] Refresh 성공 → STOMP 재연결`)
            await reconnectStomp()
        } catch {
            console.log(`[${debugLabel}] Refresh 실패 → 로그아웃 상태`)
            await disconnectStomp()
        }
    }

    return client
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

function updateConnectHeaders(client: Client) {
    const token = tokenStorage.get()
    if (!token) return
    client.connectHeaders = { Authorization: `Bearer ${token}` }
}

async function reconnectClient(client: Client | null) {
    if (!client) return
    updateConnectHeaders(client)
    if (client.active) await client.deactivate()
    client.activate()
}

export async function reconnectStomp() {
    await reconnectClient(stockClient)
    await reconnectClient(orderClient)
}

export async function disconnectStomp() {
    if (stockClient) { await stockClient.deactivate(); stockClient = null }
    if (orderClient) { await orderClient.deactivate(); orderClient = null }
}

type ConnectCallback = () => void | (() => void)

const stockListeners = new Map<ConnectCallback, (() => void) | void>()
const orderListeners = new Map<ConnectCallback, (() => void) | void>()

function chain(
    a: (frame: IFrame) => void,
    b: () => void,
) {
    return (frame: IFrame) => {
        a(frame)
        b()
    }
}

/**
 * stock 소켓이 연결된 시점에(이미 연결되어 있으면 즉시) fn을 실행
 * fn이 반환하는 함수는 "구독 해제 콜백"으로 등록해 뒀다가,
 * 반환된 destroy 함수를 호출하면 그 구독을 해제
 */
export function onStockConnect(fn: ConnectCallback): () => void {
    const client = getStockClient()

    const run = () => {
        const cleanup = fn()
        stockListeners.set(fn, cleanup)
    }

    if (client.connected) {
        run()
    } else {
        client.onConnect = client.onConnect ? chain(client.onConnect, run) : run
    }

    if (!client.active) client.activate()

    return () => {
        const cleanup = stockListeners.get(fn)
        if (typeof cleanup === 'function') cleanup()
        stockListeners.delete(fn)
    }
}

export function onOrderConnect(fn: ConnectCallback): () => void {
    const client = getOrderClient()

    const run = () => {
        const cleanup = fn()
        orderListeners.set(fn, cleanup)
    }

    if (client.connected) {
        run()
    } else {
        client.onConnect = client.onConnect ? chain(client.onConnect, run) : run
    }

    if (!client.active) client.activate()

    return () => {
        const cleanup = orderListeners.get(fn)
        if (typeof cleanup === 'function') cleanup()
        orderListeners.delete(fn)
    }
}
