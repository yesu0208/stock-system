import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { getPopularStocks, getDealRank } from '../../api/marketInfo'
import type {
    PopularStock,
    DealRankDay,
    DealRankMarket,
    InvestorType,
    DealType,
    PeriodType,
} from '../../types/marketWidgets'

interface Props {
    show: boolean
    onClose: () => void
    onSelectStock?: (code: string) => void
}

type TabType = 'popular' | 'foreign' | 'institution'

export default function HotStocksModal({ show, onClose, onSelectStock }: Props) {
    const [tab, setTab] = useState<TabType>('popular')

    const [stocks, setStocks] = useState<PopularStock[]>([])
    const [loading, setLoading] = useState(false)

    const [market, setMarket] = useState<DealRankMarket>('KOSPI')
    const [dealType, setDealType] = useState<DealType>('BUY')
    const [periodType, setPeriodType] = useState<PeriodType>('DAY')

    const [dealDays, setDealDays] = useState<DealRankDay[]>([])
    const [dealLoading, setDealLoading] = useState(false)

    useEffect(() => {
        if (!show || tab !== 'popular') return
        setLoading(true)
        getPopularStocks().then(setStocks).catch(() => setStocks([])).finally(() => setLoading(false))
    }, [show, tab])

    useEffect(() => {
        if (!show) return
        if (tab !== 'foreign' && tab !== 'institution') return

        const investorType: InvestorType = tab === 'foreign' ? 'FOREIGN' : 'INSTITUTION'

        setDealLoading(true)
        getDealRank(market, investorType, dealType, periodType)
            .then(res => setDealDays(res.days))
            .catch(() => setDealDays([]))
            .finally(() => setDealLoading(false))
    }, [show, tab, market, dealType, periodType])

    useEffect(() => {
        if (!show) {
            setTab('popular')
        }
    }, [show])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '1000px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>인기 종목</h3>

                <div style={{ display: 'flex', gap: '4px', marginBottom: '8px' }}>
                    <button onClick={() => setTab('popular')} style={styles.tabButton(tab === 'popular')}>인기</button>
                    <button onClick={() => setTab('foreign')} style={styles.tabButton(tab === 'foreign')}>외국인</button>
                    <button onClick={() => setTab('institution')} style={styles.tabButton(tab === 'institution')}>기관</button>
                </div>

                {(tab === 'foreign' || tab === 'institution') && (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '8px' }}>
                        <div style={{ display: 'flex', gap: '4px' }}>
                            <button onClick={() => setMarket('KOSPI')} style={styles.filterButton(market === 'KOSPI')}>코스피</button>
                            <button onClick={() => setMarket('KOSDAQ')} style={styles.filterButton(market === 'KOSDAQ')}>코스닥</button>
                        </div>
                        <div style={{ display: 'flex', gap: '4px' }}>
                            <button onClick={() => setDealType('BUY')} style={styles.filterButton(dealType === 'BUY')}>매수</button>
                            <button onClick={() => setDealType('SELL')} style={styles.filterButton(dealType === 'SELL')}>매도</button>
                        </div>
                        <div style={{ display: 'flex', gap: '4px' }}>
                            <button onClick={() => setPeriodType('DAY')} style={styles.filterButton(periodType === 'DAY')}>일</button>
                            <button onClick={() => setPeriodType('WEEK')} style={styles.filterButton(periodType === 'WEEK')}>주</button>
                            <button onClick={() => setPeriodType('MONTH')} style={styles.filterButton(periodType === 'MONTH')}>1개월</button>
                            <button onClick={() => setPeriodType('THREE_MONTH')} style={styles.filterButton(periodType === 'THREE_MONTH')}>3개월</button>
                        </div>
                    </div>
                )}

                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {/* 인기 종목 탭 */}
                    {tab === 'popular' && (
                        loading ? (
                            <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                        ) : (
                            <>
                                <div style={{ display: 'flex', gap: '8px', padding: '4px', fontSize: '11px', color: '#666' }}>
                                    <span style={{ width: '24px' }}></span>
                                    <span style={{ flex: 1 }}>종목명</span>
                                    <span style={{ width: '60px' }}>코드</span>
                                    <span style={{ width: '70px', textAlign: 'right' }}>현재가</span>
                                    <span style={{ width: '40px', textAlign: 'right' }}>등락</span>
                                </div>

                                {stocks.map(s => (
                                    <button
                                        key={s.code}
                                        onClick={() => onSelectStock?.(s.code)}
                                        style={styles.row}
                                        disabled={!onSelectStock}
                                    >
                                        <span style={{ width: '24px', color: '#888' }}>{s.rank}</span>
                                        <span style={{ flex: 1, textAlign: 'left' }}>{s.name}</span>
                                        {/* [수정] code 필드 표시 */}
                                        <span style={{ width: '60px', color: '#888', fontSize: '12px' }}>{s.code}</span>
                                        <span style={{
                                            width: '70px',
                                            textAlign: 'right',
                                            color: s.direction === '상승' ? '#FF6347' : s.direction === '하락' ? '#4F9DFF' : '#FFF',
                                        }}>
                                            {s.price}
                                        </span>
                                        <span style={{
                                            width: '40px',
                                            textAlign: 'right',
                                            fontSize: '12px',
                                            color: s.direction === '상승' ? '#FF6347' : s.direction === '하락' ? '#4F9DFF' : '#FFF',
                                        }}>
                                            {s.direction}
                                        </span>
                                    </button>
                                ))}
                            </>
                        )
                    )}

                    {(tab === 'foreign' || tab === 'institution') && (
                        dealLoading ? (
                            <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                        ) : (
                            dealDays.map(day => (
                                <div key={day.dealDate}>
                                    <div style={{ color: '#888', fontSize: '12px', padding: '6px 4px' }}>{day.dealDate}</div>

                                    <div style={{ display: 'flex', gap: '8px', padding: '4px', fontSize: '11px', color: '#666' }}>
                                        <span style={{ width: '20px' }}></span>
                                        <span style={{ flex: 1 }}>종목</span>
                                        <span style={{ width: '70px', textAlign: 'right' }}>수량</span>
                                        <span style={{ width: '90px', textAlign: 'right' }}>거래대금</span>
                                        <span style={{ width: '70px', textAlign: 'right' }}>거래량</span>
                                    </div>

                                    {day.items.map(item => (
                                        <button
                                            key={item.stockCode}
                                            onClick={() => onSelectStock?.(item.stockCode)}
                                            style={styles.dealRow}
                                            disabled={!onSelectStock}
                                        >
                                            <span style={{ width: '20px', color: '#888', fontSize: '12px' }}>{item.rank}</span>
                                            <span style={{ flex: 1, textAlign: 'left' }}>{item.stockName}</span>
                                            <span style={{ width: '70px', textAlign: 'right', fontSize: '12px' }}>
                                                {item.quantity.toLocaleString()}
                                            </span>
                                            <span style={{ width: '90px', textAlign: 'right', fontSize: '12px' }}>
                                                {item.amount.toLocaleString()}
                                            </span>
                                            <span style={{ width: '70px', textAlign: 'right', fontSize: '12px' }}>
                                                {item.volume.toLocaleString()}
                                            </span>
                                        </button>
                                    ))}
                                </div>
                            ))
                        )
                    )}
                </div>
                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    row: { display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '8px 4px', borderBottom: '1px solid #262626', background: 'none', border: 'none', color: '#FFF', fontSize: '13px', cursor: 'pointer' },
    // [신규] 외국인/기관 탭 전용 행 스타일 (컬럼이 많아 폭이 넓음)
    dealRow: { display: 'flex', alignItems: 'center', gap: '8px', width: '100%', padding: '8px 4px', borderBottom: '1px solid #262626', background: 'none', border: 'none', color: '#FFF', fontSize: '13px', cursor: 'pointer', minWidth: '280px' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    tabButton: (active: boolean) => ({
        flex: 1,
        padding: '8px',
        fontSize: '13px',
        backgroundColor: active ? '#444' : '#222',
        color: '#FFF',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
    }),
    filterButton: (active: boolean) => ({
        flex: 1,
        padding: '6px',
        fontSize: '12px',
        backgroundColor: active ? '#3a5f8a' : '#222',
        color: '#FFF',
        border: 'none',
        borderRadius: '4px',
        cursor: 'pointer',
    }),
} as const
