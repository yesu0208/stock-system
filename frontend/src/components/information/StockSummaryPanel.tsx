import { useEffect, useState } from 'react'
import type { StockSummaryTickMessage } from '../../types/stockSummary.ts'

interface Props {
    summaries: (StockSummaryTickMessage & { stockName: string })[]
}

export default function StockSummaryPanel({ summaries }: Props) {

    const [now, setNow] = useState(new Date())

    // 1분마다 시간 갱신
    useEffect(() => {
        const interval = setInterval(() => {
            setNow(new Date())
        }, 1000) // 1초마다 갱신

        return () => clearInterval(interval)
    }, [])

    const formatDateTime = (date: Date) => {
        const yyyy = date.getFullYear()
        const mm = String(date.getMonth() + 1).padStart(2, '0')
        const dd = String(date.getDate()).padStart(2, '0')
        const hh = String(date.getHours()).padStart(2, '0')
        const min = String(date.getMinutes()).padStart(2, '0')

        return `${yyyy}.${mm}.${dd} ${hh}:${min}`
    }

    const isMarketOpen = (date: Date) => {
        const day = date.getDay() // 0=일, 6=토
        const hours = date.getHours()
        const minutes = date.getMinutes()

        if (day === 0 || day === 6) return false // 주말

        const currentMinutes = hours * 60 + minutes
        const start = 9 * 60        // 09:00
        const end = 15 * 60 + 30    // 15:30

        return currentMinutes >= start && currentMinutes <= end
    }

    const marketStatus = isMarketOpen(now)
        ? formatDateTime(now) + ' 기준'
        : '장 종료'

    return (
        <div style={styles.wrapper}>
            <div style={styles.titleRow}>
                <div style={styles.title}>실시간 종목 정보</div>
                <div style={styles.time}>{marketStatus}</div>
            </div>

            <div style={styles.divider} />

            {summaries.map(s => {
                const cur = s.curPrice ?? 0
                const prev = s.prevClose ?? 0
                const diff = cur - prev
                const percent = prev !== 0 ? ((diff / prev) * 100).toFixed(2) : '0.00'

                let diffSymbol = '━'
                let diffColor = '#DDD'

                if (diff > 0) {
                    diffSymbol = '▲'
                    diffColor = '#FF6347'
                } else if (diff < 0) {
                    diffSymbol = '▼'
                    diffColor = '#4F9DFF'
                }

                return (
                    <div key={s.stockCode} style={styles.row}>
                        <span>
                            {s.stockName}{' '}
                            <span style={{ fontSize: '0.75em', color: '#AAA' }}>
                                ({s.stockCode})
                            </span>
                        </span>

                        <span>
                            <span style={{ color: '#DDD', marginRight: 6 }}>
                                {cur.toLocaleString()}
                            </span>
                            <span style={{ color: diffColor }}>
                                {diffSymbol} {Math.abs(diff).toLocaleString()} ({percent}%)
                            </span>
                        </span>
                    </div>
                )
            })}
        </div>
    )
}

const styles = {
    wrapper: {
        background: '#1E1E1E',
        borderRadius: '10px',
        padding: '12px',
    },
    titleRow: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    title: {
        fontWeight: 'bold',
        color: '#FFF'
    },
    time: {
        fontSize: '12px',
        color: '#888'
    },
    row: {
        display: 'flex',
        justifyContent: 'space-between',
        padding: '4px 0',
        color: '#DDD',
        fontSize: '13px'
    },
    divider: {
        height: '1px',
        backgroundColor: '#333',
        margin: '8px 0',
    }
} as const
