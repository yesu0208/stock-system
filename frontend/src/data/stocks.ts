import type { StockInfo } from '../types/stock'

export const STOCKS: StockInfo[] = [
    { code: '005930', name: '삼성전자', group: 'A' },
    { code: '000660', name: 'SK하이닉스', group: 'A' },
    { code: '005935', name: '삼성전자우', group: 'A' },
    { code: '005380', name: '현대차', group: 'B' },
    { code: '373220', name: 'LG에너지솔루션', group: 'B' },
    { code: '000250', name: '삼천당제약', group: 'B' },
    { code: '196170', name: '알테오젠', group: 'B' },
    { code: '086520', name: '에코프로', group: 'B' },
    { code: '247540', name: '에코프로비엠', group: 'B' },
    { code: '277810', name: '레인보우로보틱스', group: 'B' },
]

export const stockNameMap = STOCKS.reduce<Record<string, string>>((acc, s) => {
    acc[s.code] = s.name
    return acc
}, {})
