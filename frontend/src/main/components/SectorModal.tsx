import { useEffect, useState } from 'react'
import ModalV2 from '../../components/ModalV2'
import { getUpjongs, getUpjongStocks } from '../../api/marketInfo'
import type { UpjongInfo, UpjongStock, UpjongRankItem } from '../../types/marketWidgets'
import './SectorModal.css'

interface Props {
    open: boolean
    onClose: () => void
}

function RankList({ title, items }: { title: string; items: UpjongRankItem[] }) {
    if (items.length === 0) return null
    return (
        <div className="sector-rank-block">
            <div className="sector-rank-title">{title}</div>
            {items.map((r) => (
                <div key={r.code} className="sector-rank-row">
                    <img
                        className="sector-rank-logo"
                        src={r.itemLogoUrl}
                        alt=""
                        onError={(e) => { (e.currentTarget as HTMLImageElement).style.visibility = 'hidden' }}
                    />
                    <span className="sector-rank-name">{r.name}</span>
                    <span className="sector-rank-code">({r.code})</span>
                    <span className="sector-rank-value">{r.value}</span>
                </div>
            ))}
        </div>
    )
}

export default function SectorModal({ open, onClose }: Props) {
    const [upjongs, setUpjongs] = useState<UpjongInfo[]>([])
    const [loading, setLoading] = useState(false)

    const [expandedNo, setExpandedNo] = useState<string | null>(null)
    const [stockList, setStockList] = useState<UpjongStock[]>([])
    const [stockListLoading, setStockListLoading] = useState(false)

    useEffect(() => {
        if (!open) return
        setLoading(true)
        getUpjongs()
            .then((res) => setUpjongs(res.items))
            .catch(() => setUpjongs([]))
            .finally(() => setLoading(false))
    }, [open])

    useEffect(() => {
        if (!open) {
            setExpandedNo(null)
            setStockList([])
        }
    }, [open])

    const handleToggle = (no: string) => {
        if (expandedNo === no) {
            setExpandedNo(null)
            setStockList([])
            return
        }

        setExpandedNo(no)
        setStockList([])
        setStockListLoading(true)
        getUpjongStocks(no)
            .then((res) => setStockList(res.items))
            .catch(() => setStockList([]))
            .finally(() => setStockListLoading(false))
    }

    return (
        <ModalV2 open={open} title="업종" onClose={onClose}>
            <div className="sector-wrap">
                {loading ? (
                    <div className="sector-loading">불러오는 중...</div>
                ) : (
                    upjongs.map((u) => (
                        <div key={u.no} className="sector-item">
                            <div className="sector-row" onClick={() => handleToggle(u.no)}>
                                <span className="sector-name">{u.name}</span>
                                <span className="sector-no">(no: {u.no})</span>
                                <span
                                    className="sector-change"
                                    style={{ color: u.changeRate.startsWith('-') ? '#4F9DFF' : '#FF6347' }}
                                >
                                    {u.changeRate}
                                </span>
                                <span className="sector-breadth">
                                    상승 {u.rise} · 보합 {u.steady} · 하락 {u.fall} · 전체 {u.total}
                                </span>
                                <span className="sector-graph-ratio">그래프비율 {u.graphRatio}</span>
                            </div>

                            <div className="sector-detail-line">
                                <span>시가총액 {u.totalMarketCap}</span>
                                <span>거래량 {u.totalTradingVolume}</span>
                                <span>거래대금 {u.totalTradingValue}</span>
                            </div>

                            <RankList title="등락률 상위" items={u.topByChangeRate} />
                            <RankList title="시가총액 상위" items={u.topByMarketCap} />
                            <RankList title="거래대금 상위" items={u.topByTradingValue} />

                            {expandedNo === u.no && (
                                <div className="sector-stock-list">
                                    {stockListLoading ? (
                                        <div className="sector-loading">종목 불러오는 중...</div>
                                    ) : stockList.length === 0 ? (
                                        <div className="sector-loading">종목이 없습니다</div>
                                    ) : (
                                        stockList.map((s) => (
                                            <div key={s.code} className="sector-stock-row">
                                                <span className="sector-stock-name">{s.name}</span>
                                                <span className="sector-stock-code">({s.code})</span>
                                                <span
                                                    className="sector-stock-price"
                                                    style={{ color: s.direction === 'DOWN' ? '#4F9DFF' : s.direction === 'UP' ? '#FF6347' : '#fff' }}
                                                >
                                                    {s.price}
                                                </span>
                                                <span
                                                    className="sector-stock-change"
                                                    style={{ color: s.direction === 'DOWN' ? '#4F9DFF' : s.direction === 'UP' ? '#FF6347' : '#fff' }}
                                                >
                                                    {s.change} ({s.rate})
                                                </span>
                                                <span className="sector-stock-volume">거래량 {s.volume}</span>
                                                <span className="sector-stock-tradingValue">거래대금 {s.tradingValue}</span>
                                                <span className="sector-stock-marketCap">시총 {s.marketCap}</span>
                                            </div>
                                        ))
                                    )}
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>
        </ModalV2>
    )
}
