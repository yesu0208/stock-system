export const STOCKS = [
    { code: '005930', name: '삼성전자' },
    { code: '000660', name: 'SK하이닉스' },
    { code: '005935', name: '삼성전자우' },
    { code: '005380', name: '현대차' },
    { code: '373220', name: 'LG에너지솔루션' },
    { code: '000250', name: '삼천당제약' },
    { code: '196170', name: '알테오젠' },
    { code: '086520', name: '에코프로' },
    { code: '247540', name: '에코프로비엠' },
    { code: '277810', name: '레인보우로보틱스' },
]

export const stockNameMap = STOCKS.reduce<Record<string, string>>(
    (acc, s) => {
        acc[s.code] = s.name
        return acc
    },
    {}
)
