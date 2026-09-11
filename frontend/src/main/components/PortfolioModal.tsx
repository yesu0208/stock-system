import { useState } from 'react'
import Modal from '../../components/Modal'
import { usePortfolio } from '../context/PortfolioContext'
import { stockNameMap } from '../../constants/stocks'

/**
 * PortfolioModal
 * CSS conic-gradient로 구현
 */

const SECTOR_COLORS = [
    '#4F9DFF', '#FF6347', '#39A54A', '#FFC107', '#9C27B0',
    '#00BCD4', '#FF9800', '#8BC34A', '#E91E63', '#607D8B',
]

interface Props {
    show: boolean
    onClose: () => void
}

export default function PortfolioModal({ show, onClose }: Props) {
    const { portfolio } = usePortfolio()
    const [expandedSector, setExpandedSector] = useState<string | null>(null)

    if (!show) return null

    if (!portfolio) {
        return (
            <Modal show={show} onClose={onClose}>
                <div style={{ width: '320px', textAlign: 'center', padding: '20px 0', color: '#666' }}>
                    포트폴리오 데이터를 불러오는 중...
                </div>
            </Modal>
        )
    }

    const segments = [
        { label: '현금', ratio: portfolio.cashRatio, color: '#555' },
        ...portfolio.sectors.map((s, i) => ({
            label: s.sector,
            ratio: s.ratioInTotal,
            color: SECTOR_COLORS[i % SECTOR_COLORS.length],
        })),
    ]

    let acc = 0
    const gradientStops = segments
        .map(seg => {
            const start = acc
            acc += seg.ratio
            return `${seg.color} ${start}% ${acc}%`
        })
        .join(', ')

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '360px', maxHeight: '70vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>포트폴리오</h3>

                <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
                    <div
                        style={{
                            width: 140,
                            height: 140,
                            borderRadius: '50%',
                            background: `conic-gradient(${gradientStops})`,
                            position: 'relative',
                        }}
                    >
                        <div
                            style={{
                                position: 'absolute',
                                top: '20%',
                                left: '20%',
                                width: '60%',
                                height: '60%',
                                borderRadius: '50%',
                                backgroundColor: '#1A1A1A',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                flexDirection: 'column',
                            }}
                        >
                            <span style={{ fontSize: '11px', color: '#888' }}>총 자산</span>
                            <span style={{ fontSize: '13px', fontWeight: 600, color: '#FFF' }}>
                                {portfolio.totalAssetValue.toLocaleString()}원
                            </span>
                        </div>
                    </div>
                </div>

                <div style={{ flex: 1, overflowY: 'auto' }}>
                    <div style={styles.legendRow}>
                        <span style={{ ...styles.dot, backgroundColor: '#555' }} />
                        <span style={{ flex: 1 }}>현금</span>
                        <span>{portfolio.cashRatio.toFixed(1)}%</span>
                    </div>

                    {portfolio.sectors.map((s, i) => (
                        <div key={s.sector}>
                            <button
                                onClick={() => setExpandedSector(expandedSector === s.sector ? null : s.sector)}
                                style={styles.legendButton}
                            >
                                <span style={{ ...styles.dot, backgroundColor: SECTOR_COLORS[i % SECTOR_COLORS.length] }} />
                                <span style={{ flex: 1, textAlign: 'left' }}>{s.sector}</span>
                                <span>{s.ratioInTotal.toFixed(1)}%</span>
                            </button>

                            {expandedSector === s.sector && (
                                <div style={{ paddingLeft: '20px' }}>
                                    {s.stocks.map(stock => (
                                        <div key={stock.stockCode} style={styles.stockRow}>
                                            <span>{stockNameMap[stock.stockCode] ?? stock.stockCode}</span>
                                            <span>{stock.totalAmount.toLocaleString()}원 ({stock.ratioInSector.toFixed(1)}%)</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    ))}
                </div>

                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    legendRow: { display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', padding: '4px 0' },
    legendButton: { display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', padding: '6px 0', width: '100%', background: 'none', border: 'none', color: '#FFF', cursor: 'pointer' },
    dot: { width: 10, height: 10, borderRadius: '50%', display: 'inline-block' },
    stockRow: { display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#AAA', padding: '2px 0' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
