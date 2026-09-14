import './HotStocksModal.css'
import { useEffect, useState } from 'react'
import ModalV2 from '../../components/ModalV2'
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

// [신규] 등락 방향에 따른 색상 클래스 매핑 (IndexModal의 color() 헬퍼와 동일한 접근)
function directionClass(direction: string): string {
    if (direction === '상승') return 'up'
    if (direction === '하락') return 'down'
    return 'neutral'
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
            {/* [수정] hs-wrap 클래스로 전체 레이아웃 감싸기 (인라인 스타일 제거) */}
            <div className="hs-wrap">
                {/* [수정] IndexModal과 동일한 밑줄 탭 스타일 적용 */}
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
                            <div className="empty">불러오는 중...</div>
                        ) : (
                            <>
                                <div className="list-header">
                                    <span className="col-rank"></span>
                                    <span className="col-name">종목명</span>
                                    <span className="col-code">코드</span>
                                    <span className="col-price">현재가</span>
                                    <span className="col-direction">등락</span>
                                </div>

                                {stocks.map(s => (
                                    <button
                                        key={s.code}
                                        className="row"
                                        onClick={() => onSelectStock?.(s.code)}
                                        disabled={!onSelectStock}
                                    >
                                        <span className="col-rank">{s.rank}</span>
                                        <span className="col-name">{s.name}</span>
                                        <span className="col-code">{s.code}</span>
                                        <span className={`col-price ${directionClass(s.direction)}`}>{s.price}</span>
                                        <span className={`col-direction ${directionClass(s.direction)}`}>{s.direction}</span>
                                    </button>
                                ))}
                            </>
                        )
                    )}

                    {(tab === 'foreign' || tab === 'institution') && (
                        dealLoading ? (
                            <div className="empty">불러오는 중...</div>
                        ) : (
                            dealDays.map(day => (
                                <div key={day.dealDate}>
                                    <div className="date-label">{day.dealDate}</div>

                                    <div className="list-header">
                                        <span className="col-rank"></span>
                                        <span className="col-name">종목</span>
                                        <span className="col-num">수량</span>
                                        <span className="col-num-wide">거래대금</span>
                                        <span className="col-num">거래량</span>
                                    </div>

                                    {day.items.map(item => (
                                        <button
                                            key={item.stockCode}
                                            className="row"
                                            onClick={() => onSelectStock?.(item.stockCode)}
                                            disabled={!onSelectStock}
                                        >
                                            <span className="col-rank">{item.rank}</span>
                                            <span className="col-name">{item.stockName}</span>
                                            <span className="col-num">{item.quantity.toLocaleString()}</span>
                                            <span className="col-num-wide">{item.amount.toLocaleString()}</span>
                                            <span className="col-num">{item.volume.toLocaleString()}</span>
                                        </button>
                                    ))}
                                </div>
                            ))
                        )
                    )}
                </div>
            </div>
        </ModalV2>
    )
}
