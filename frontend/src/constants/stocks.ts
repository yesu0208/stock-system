export const STOCKS = [
    { code: '005930', name: '삼성전자' },
    { code: '000660', name: 'SK하이닉스' },
    { code: '086520', name: '에코프로' },
    { code: '247540', name: '에코프로비엠' },
]

export const stockNameMap = STOCKS.reduce<Record<string, string>>(
    (acc, s) => {
        acc[s.code] = s.name
        return acc
    },
    {}
)
