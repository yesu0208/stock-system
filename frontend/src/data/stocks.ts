import type { StockInfo } from '../types/stock'

export const STOCKS: StockInfo[] = [
    { code: '005930', name: '삼성전자', isSupported: true },
    { code: '000660', name: 'SK하이닉스', isSupported: true },
    { code: '005935', name: '삼성전자우', isSupported: true },
    { code: '005380', name: '현대차', isSupported: true },
    { code: '373220', name: 'LG에너지솔루션', isSupported: true },
    { code: '000250', name: '삼천당제약', isSupported: true },
    { code: '196170', name: '알테오젠', isSupported: true },
    { code: '086520', name: '에코프로', isSupported: true },
    { code: '247540', name: '에코프로비엠', isSupported: true },
    { code: '277810', name: '레인보우로보틱스', isSupported: true },

    { code: '035420', name: 'NAVER', isSupported: false },
    { code: '035720', name: '카카오', isSupported: false },
    { code: '005490', name: 'POSCO홀딩스', isSupported: false },
    { code: '051910', name: 'LG화학', isSupported: false },
    { code: '006400', name: '삼성SDI', isSupported: false },
]

export const stockNameMap = STOCKS.reduce<Record<string, string>>((acc, s) => {
    acc[s.code] = s.name
    return acc
}, {})

export function findStock(code: string): StockInfo | undefined {
    return STOCKS.find((s) => s.code === code)
}

export function isSupportedStock(code: string): boolean {
    return findStock(code)?.isSupported === true
}

export function findSupportedStock(code: string): StockInfo | undefined {
    const stock = findStock(code)
    return stock?.isSupported ? stock : undefined
}
