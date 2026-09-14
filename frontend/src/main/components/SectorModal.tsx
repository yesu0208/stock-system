import "./SectorModal.css";
import ModalV2 from "../../components/ModalV2";
import { useEffect, useState } from "react";
import { FiArrowLeft, FiSearch } from "react-icons/fi";
import { getUpjongs, getUpjongStocks } from "../../api/marketInfo";
import type { UpjongInfo, UpjongStock, UpjongRankItem } from "../../types/marketWidgets";
import Tooltip from "../../tooltip/Tooltip";

interface Props {
    open: boolean;
    onClose: () => void;
}

function RankList({ title, items }: { title: string; items: UpjongRankItem[] }) {
    if (items.length === 0) return null;
    return (
        <div className="sector-rank-block">
            <div className="sector-rank-title">{title}</div>
            {items.map((r) => (
                <div key={r.code} className="sector-rank-row">
                    <img
                        className="sector-rank-logo"
                        src={r.itemLogoUrl}
                        alt=""
                        onError={(e) => { (e.currentTarget as HTMLImageElement).style.visibility = "hidden"; }}
                    />
                    <span className="sector-rank-name">{r.name}</span>
                    <span className="sector-rank-value">{r.value}</span>
                </div>
            ))}
        </div>
    );
}

export default function SectorModal({ open, onClose }: Props) {
    const [upjongs, setUpjongs] = useState<UpjongInfo[]>([]);
    const [loading, setLoading] = useState(false);

    const [animate, setAnimate] = useState(false);

    const [selectedUpjong, setSelectedUpjong] = useState<UpjongInfo | null>(null);
    const [stocks, setStocks] = useState<UpjongStock[]>([]);
    const [stockLoading, setStockLoading] = useState(false);

    const [search, setSearch] = useState("");

    useEffect(() => {
        if (!open) return;
        setLoading(true);
        getUpjongs()
            .then((res) => setUpjongs(res.items))
            .catch(() => setUpjongs([]))
            .finally(() => setLoading(false));
    }, [open]);

    useEffect(() => {
        if (!open) {
            setSelectedUpjong(null);
            setStocks([]);
            setSearch("");
            setAnimate(false);
            return;
        }
        const timer = setTimeout(() => setAnimate(true), 50);
        return () => clearTimeout(timer);
    }, [open]);

    const getRate = (rate: string) => parseFloat(rate.replace("%", "")) || 0;

    const maxAbsRate = upjongs.length > 0
        ? Math.max(...upjongs.map((u) => Math.abs(getRate(u.changeRate))))
        : 0;

    const handleSelectUpjong = async (item: UpjongInfo) => {
        setSelectedUpjong(item);
        setStockLoading(true);
        try {
            const res = await getUpjongStocks(item.no);
            setStocks(res.items);
        } catch (e) {
            console.error(e);
            setStocks([]);
        } finally {
            setStockLoading(false);
        }
    };

    const handleBack = () => {
        setSelectedUpjong(null);
        setStocks([]);
        setSearch("");
    };

    const filteredStocks = stocks.filter((stock) => {
        const keyword = search.toLowerCase();
        return (
            stock.name.toLowerCase().includes(keyword) ||
            stock.code.toLowerCase().includes(keyword)
        );
    });

    return (
        <ModalV2 open={open} title="업종" onClose={onClose}>
            <div className="sector-wrap">
                {loading ? (
                    <div className="sector-loading">불러오는 중...</div>
                ) : (
                    <div className={`sector-slider ${selectedUpjong ? "show-stock" : ""}`}>
                        {/* 업종 목록 */}
                        <div className="sector-page">
                            <div className="sector-grid">
                                {upjongs.map((item) => {
                                    const rate = getRate(item.changeRate);

                                    return (
                                        <div
                                            key={item.no}
                                            className="sector-card"
                                            onClick={() => handleSelectUpjong(item)}
                                        >
                                            <div className="sector-top">
                                                <span className="sector-name">{item.name}</span>
                                                <span className={rate > 0 ? "rise" : rate < 0 ? "fall" : "steady"}>
                                                    {item.changeRate}%
                                                </span>
                                            </div>

                                            <div className="sector-bottom">
                                                <span className="rise">↑ {item.rise}</span>
                                                <span className="steady">- {item.steady}</span>
                                                <span className="fall">↓ {item.fall}</span>
                                            </div>

                                            <div
                                                className="sector-bar"
                                                style={{
                                                    width: animate
                                                        ? `calc(${maxAbsRate > 0 ? (Math.abs(rate) / maxAbsRate) * 100 : 0}% - 24px)`
                                                        : "0px",
                                                    background: rate >= 0 ? "#ff6347" : "#4f9dff",
                                                }}
                                            />
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        {/* 종목 목록 */}
                        <div className="sector-page stock-page">
                            <div className="stock-header">
                                <span className="stock-title">{selectedUpjong?.name}</span>

                                <Tooltip text="뒤로가기" placement="top">
                                    <button className="stock-back-btn" onClick={handleBack} aria-label="뒤로가기">
                                        <FiArrowLeft size={18} />
                                    </button>
                                </Tooltip>
                            </div>

                            {selectedUpjong && (
                                <div className="sector-summary-line">
                                    <span>시가총액 {selectedUpjong.totalMarketCap}</span>
                                    <span>거래량 {selectedUpjong.totalTradingVolume}</span>
                                    <span>거래대금 {selectedUpjong.totalTradingValue}</span>
                                </div>
                            )}

                            {selectedUpjong && (
                                <div className="sector-rank-section">
                                    <RankList title="등락률 상위" items={selectedUpjong.topByChangeRate} />
                                    <RankList title="시가총액 상위" items={selectedUpjong.topByMarketCap} />
                                    <RankList title="거래대금 상위" items={selectedUpjong.topByTradingValue} />
                                </div>
                            )}

                            <div className="stock-search-box">
                                <FiSearch size={14} className="stock-search-icon" />
                                <input
                                    className="stock-search"
                                    placeholder="종목명·종목코드 검색"
                                    value={search}
                                    onChange={(e) => setSearch(e.target.value)}
                                />
                            </div>

                            <div className="stock-list">
                                {stockLoading ? (
                                    <div className="sector-loading">불러오는 중...</div>
                                ) : (
                                    filteredStocks.map((stock) => {
                                        const rate = parseFloat(stock.rate);
                                        const rateClass = rate > 0 ? "rise" : rate < 0 ? "fall" : "steady";

                                        return (
                                            <div key={stock.code} className="stock-card">
                                                <div className="stock-left">
                                                    <div className="stock-name">{stock.name}</div>
                                                    <div className="stock-code">{stock.code}</div>
                                                </div>

                                                <div className="stock-right">
                                                    <div className={`stock-price ${rateClass}`}>{stock.price}</div>
                                                    <div className={`stock-rate ${rateClass}`}>{stock.rate}</div>
                                                </div>
                                            </div>
                                        );
                                    })
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </ModalV2>
    );
}
