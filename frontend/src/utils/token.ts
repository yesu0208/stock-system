const TOKEN_KEY = 'accessToken'
const channel = new BroadcastChannel('auth_channel')

type TokenListener = (token: string | null) => void
let listeners: TokenListener[] = []

export const tokenStorage = {
    get(): string | null {
        return localStorage.getItem(TOKEN_KEY)
    },
    set(token: string) {
        localStorage.setItem(TOKEN_KEY, token)
        channel.postMessage({ type: 'login', token })
        listeners.forEach(cb => cb(token))
    },
    clear() {
        localStorage.removeItem(TOKEN_KEY)
        channel.postMessage({ type: 'logout' })
        listeners.forEach(cb => cb(null))
    },
    subscribe(cb: TokenListener) {
        listeners.push(cb)
        return () => {
            listeners = listeners.filter(l => l !== cb)
        }
    },
}

// BroadcastChannel 수신
channel.onmessage = (event) => {
    const { type, token } = event.data
    if (type === 'login') {
        localStorage.setItem(TOKEN_KEY, token)
        listeners.forEach(cb => cb(token))
    } else if (type === 'logout') {
        localStorage.removeItem(TOKEN_KEY)
        listeners.forEach(cb => cb(null))
    }
}

// storage 이벤트 (크로스 브라우저)
window.addEventListener('storage', (event) => {
    if (event.key === TOKEN_KEY) {
        listeners.forEach(cb => cb(event.newValue))
    }
})
