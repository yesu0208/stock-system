import './HotStocksModal.css'
import { useEffect, useState } from 'react'
import ModalV2 from '../../components/ModalV2'
import Spinner from '../../components/Spinner'
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

function getColor(direction: string): string {
    switch (direction) {
        case 'UPPER_LIMIT':
        case 'UP':
            return '#ff6347'
        case 'LOWER_LIMIT':
        case 'DOWN':
            return '#4f9dff'
        case 'STEADY':
        default:
            return '#ffffff'
    }
}

function getArrow(direction: string): string {
    switch (direction) {
        case 'UPPER_LIMIT':
            return '⬆'
        case 'UP':
            return '▲'
        case 'LOWER_LIMIT':
            return '⬇'
        case 'DOWN':
            return '▼'
        case 'STEADY':
            return '-'
        default:
            return ''
    }
}

function formatPrice(price: string): string {
    const num = Number(price.replace(/[^0-9.-]/g, ''))
    if (isNaN(num)) return price
    return num.toLocaleString()
}

function getSignColor(value: number): string {
    if (value < 0) return '#4f9dff'
    if (value > 0) return '#ff6347'
    return '#ffffff'
}

function formatToMillion(amount: number): string {
    const millions = Math.trunc(amount / 1_000_000)
    return millions.toLocaleString()
}

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

    return (
        <ModalV2 open={show} title="인기 종목" onClose={onClose}>
            <div className="hs-wrap">
                <div className="tab">
                    <button
                        className={tab === 'popular' ? 'active' : ''}
                        onClick={() => setTab('popular')}
                    >
                        인기
                    </button>
                    <button
                        className={tab === 'foreign' ? 'active' : ''}
                        onClick={() => setTab('foreign')}
                    >
                        외국인
                    </button>
                    <button
                        className={tab === 'institution' ? 'active' : ''}
                        onClick={() => setTab('institution')}
                    >
                        기관
                    </button>
                </div>

                {(tab === 'foreign' || tab === 'institution') && (
                    <div className="filters">
                        <div className="filter-group">
                            <button className={market === 'KOSPI' ? 'active' : ''} onClick={() => setMarket('KOSPI')}>코스피</button>
                            <button className={market === 'KOSDAQ' ? 'active' : ''} onClick={() => setMarket('KOSDAQ')}>코스닥</button>
                        </div>
                        <div className="filter-group">
                            <button className={dealType === 'BUY' ? 'active' : ''} onClick={() => setDealType('BUY')}>매수</button>
                            <button className={dealType === 'SELL' ? 'active' : ''} onClick={() => setDealType('SELL')}>매도</button>
                        </div>
                        <div className="filter-group">
                            <button className={periodType === 'DAY' ? 'active' : ''} onClick={() => setPeriodType('DAY')}>일</button>
                            <button className={periodType === 'WEEK' ? 'active' : ''} onClick={() => setPeriodType('WEEK')}>주</button>
                            <button className={periodType === 'MONTH' ? 'active' : ''} onClick={() => setPeriodType('MONTH')}>1개월</button>
                            <button className={periodType === 'THREE_MONTH' ? 'active' : ''} onClick={() => setPeriodType('THREE_MONTH')}>3개월</button>
                        </div>
                    </div>
                )}

                <div className="list">
                    {tab === 'popular' && (
                        loading ? (
                            <Spinner />
                        ) : (
                            <div className="hot-list tab-content" key={`${tab}-data`}>
                                {stocks.map(s => (
                                    <button
                                        key={s.code}
                                        className="hot-item"
                                        onClick={() => onSelectStock?.(s.code)}
                                        disabled={!onSelectStock}
                                    >
                                        <div className="hot-rank">{s.rank}</div>
                                        <div className="hot-name">{s.name}</div>
                                        <div className="hot-price" style={{ color: getColor(s.direction) }}>
                                            <span className="hot-price-arrow">{getArrow(s.direction)}</span>
                                            <span className="hot-price-value">{formatPrice(s.price)}</span>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )
                    )}

                    {(tab === 'foreign' || tab === 'institution') && (
                        dealLoading ? (
                            <Spinner />
                        ) : (
                            <div className="tab-content" key={`${tab}-${market}-${dealType}-${periodType}`}>
                                {dealDays.map(day => (
                                    <div key={day.dealDate}>
                                        <div className="day-header-sticky">
                                            <div className="date-label">{day.dealDate}</div>

                                            <div className="list-header">
                                                <span className="col-rank"></span>
                                                <span className="col-name"></span>
                                                <span className="col-num">수량 (주)</span>
                                                <span className="col-num-wide">거래대금 (백만)</span>
                                                <span className="col-num">거래량 (주)</span>
                                            </div>
                                        </div>

                                        {day.items.map(item => (
                                            <button
                                                key={item.stockCode}
                                                className="row"
                                                onClick={() => onSelectStock?.(item.stockCode)}
                                                disabled={!onSelectStock}
                                            >
                                                <span className="col-rank deal-rank">{item.rank}</span>
                                                <span className="col-name">{item.stockName}</span>
                                                <span className="col-num" style={{ color: getSignColor(item.quantity) }}>{item.quantity.toLocaleString()}</span>
                                                <span className="col-num-wide" style={{ color: getSignColor(item.amount) }}>{formatToMillion(item.amount)}</span>
                                                <span className="col-num">{item.volume.toLocaleString()}</span>
                                            </button>
                                        ))}
                                    </div>
                                ))}
                            </div>
                        )
                    )}
                </div>
            </div>
        </ModalV2>
    )
}
