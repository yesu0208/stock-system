import "./TopBar.css";

import { useEffect, useState } from "react";
import { FaStar, FaRegStar, FaBell, FaRegBell, FaSearch } from "react-icons/fa";

import { STOCKS } from "../data/stocks";
import { useStock } from "../context/StockContext";
import { useRealtime } from "../context/RealtimeContext";
import { useWatchList } from "../context/WatchListContext";
import { useAlert } from "../context/AlertContext";
import AlertModal from "./AlertModal";
import Tooltip from "../../tooltip/Tooltip";
import { getStockDetailExtra } from "../../api/stockInfo";
import type { StockDetailExtraResponse } from "../../api/stockInfo";
import type { StockDetailTickMessage } from "../../types/stockDetail";
import type { TradePriceTickMessage } from "../../types/tradePriceTickMessage";

import {
    isRealtimeStock,
    DIRECTION_CLASS,
    calcStockStats,
} from "../../utils/stockUtils";

export { isRealtimeStock };

const WARNING_CLASS: Record<string, string> = {
    "투자주의": "badge--caution",
    "투자경고": "badge--warning",
    "투자위험": "badge--danger",
};

export default function TopBar() {
    const { selectedStock, setSelectedStock } = useStock();
    const { subscribeStock } = useRealtime();
    const { isWatched, addStock, removeStock } = useWatchList();

    const [alertOpen, setAlertOpen] = useState(false);
    const [query, setQuery] = useState("");
    const [open, setOpen] = useState(false);

    const isRealtime = isRealtimeStock(selectedStock.code);

    const [priceTick, setPriceTick] = useState<TradePriceTickMessage | null>(null);
    const [detailTick, setDetailTick] = useState<StockDetailTickMessage | null>(null);
    const [detailExtra, setDetailExtra] = useState<StockDetailExtraResponse | null>(null);

    useEffect(() => {
        setPriceTick(null);
        setDetailTick(null);
        setDetailExtra(null);

        getStockDetailExtra(selectedStock.code)
            .then(setDetailExtra)
            .catch(() => setDetailExtra(null));
    }, [selectedStock.code]);

    useEffect(() => {
        return subscribeStock(selectedStock.code, (tick: any) => {
            if (tick.tickMessageType === "TRADEPRICE") {
                setPriceTick(tick as TradePriceTickMessage);
            }
            if (tick.tickMessageType === "DETAIL") {
                setDetailTick(tick as StockDetailTickMessage);
            }
        });
    }, [selectedStock.code, subscribeStock]);

    const filteredStocks = STOCKS.filter((s) =>
        `${s.name} ${s.code}`.toLowerCase().includes(query.toLowerCase())
    );

    const handleSelect = (stock: any) => {
        setSelectedStock(stock);
        setQuery("");
        setOpen(false);
    };

    const warningType = detailExtra?.warningType ?? "";
    const manage = detailExtra?.manage ?? "";
    const marketName = detailExtra?.market ?? "-";
    const warningBadgeClass = WARNING_CLASS[warningType] ?? "badge--caution";

    const stats = isRealtime
        ? calcStockStats(true, priceTick, null)
        : calcStockStats(false, null, detailTick
            ? { ...detailTick, prevClosePrice: detailTick.prevPrice }
            : null);

    const { alerts } = useAlert();
    const bellOn = alerts.some((a) => a.stockCode === selectedStock.code);
    const starred = isWatched(selectedStock.code);

    const handleBellClick = () => setAlertOpen(true);
    const handleAlertClose = () => setAlertOpen(false);

    const handleStarClick = () => {
        if (starred) {
            removeStock(selectedStock.code);
        } else {
            addStock(selectedStock.code, selectedStock.name);
        }
    };

    return (
        <>
            <div className="top-bar">
                <div className="cell search-cell">
                    <div className="search-box">
                        <div className="search-icon">
                            <FaSearch />
                        </div>
                        <input
                            value={query}
                            placeholder="종목명·종목코드 검색"
                            onChange={(e) => {
                                setQuery(e.target.value);
                                setOpen(true);
                            }}
                            onFocus={() => setOpen(true)}
                        />

                        {open && query && (
                            <div className="dropdown">
                                {filteredStocks.length > 0 ? (
                                    filteredStocks.map((s) => (
                                        <div
                                            key={s.code}
                                            className={`item${s.realtimeSupported ? " item--tradable" : ""}`}
                                            onClick={() => handleSelect(s)}
                                        >
                                            <span className="name">{s.name}</span>
                                            <span className="code">{s.code}</span>
                                        </div>
                                    ))
                                ) : (
                                    <div className="empty">검색 결과 없음</div>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                <div className="cell stock-cell">
                    <img
                        className="stock-mark"
                        src={`https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${selectedStock.code}.svg`}
                        alt=""
                        onError={(e) => {
                            (e.currentTarget as HTMLImageElement).style.visibility = "hidden";
                        }}
                    />
                    <div className="stock-info">
                        <div className="name-row">
                            <div className="name">{selectedStock.name}</div>
                        </div>
                        <div className="meta">
                            <span className="meta-text">
                                {selectedStock.code} / {marketName}
                            </span>

                            {warningType && (
                                <span className={`badge ${warningBadgeClass}`}>{warningType}</span>
                            )}
                            {manage && (
                                <span className="badge badge--manage">{manage}</span>
                            )}
                        </div>
                    </div>
                </div>

                <div className="cell price-cell">
                    <div className={`cur ${stats ? DIRECTION_CLASS[stats.cur.direction] : ""}`}>
                        {stats?.cur.price ?? "-"}
                    </div>
                    <div className={`diff ${stats ? DIRECTION_CLASS[stats.cur.direction] : ""}`}>
                        {stats?.cur.diffText ?? "-"}
                        {stats?.cur.rateText && <span> ({stats.cur.rateText})</span>}
                    </div>
                </div>

                <div className="cell stat-cell">
                    <div className="label-row">
                        <div className="label">시가</div>
                        <div className={`rate-tooltip ${stats ? DIRECTION_CLASS[stats.open.direction] : ""}`}>
                            {stats?.open.rateText ?? ""}
                        </div>
                    </div>
                    <div className={`value ${stats ? DIRECTION_CLASS[stats.open.direction] : ""}`}>
                        {stats?.open.price ?? "-"}
                    </div>
                </div>

                <div className="cell stat-cell">
                    <div className="label-row">
                        <div className="label">고가</div>
                        <div className={`rate-tooltip ${stats ? DIRECTION_CLASS[stats.high.direction] : ""}`}>
                            {stats?.high.rateText ?? ""}
                        </div>
                    </div>
                    <div className={`value ${stats ? DIRECTION_CLASS[stats.high.direction] : ""}`}>
                        {stats?.high.price ?? "-"}
                    </div>
                </div>

                <div className="cell stat-cell">
                    <div className="label-row">
                        <div className="label">저가</div>
                        <div className={`rate-tooltip ${stats ? DIRECTION_CLASS[stats.low.direction] : ""}`}>
                            {stats?.low.rateText ?? ""}
                        </div>
                    </div>
                    <div className={`value ${stats ? DIRECTION_CLASS[stats.low.direction] : ""}`}>
                        {stats?.low.price ?? "-"}
                    </div>
                </div>

                <div className="cell stat-cell">
                    <div className="label">거래량</div>
                    <div className="value">
                        {stats?.volume ?? "-"}
                        <span className="unit">주</span>
                    </div>
                </div>

                <div className="cell stat-cell">
                    <div className="label">거래대금</div>
                    <div className="value">
                        {stats?.tradingValue ?? "-"}
                        <span className="unit">백만</span>
                    </div>
                </div>

                <div className="cell actions">
                    <Tooltip text={starred ? "관심종목 해제" : "관심종목 등록"} placement="top">
                        <button className={`icon ${starred ? "on" : ""}`} onClick={handleStarClick}>
                            {starred ? <FaStar /> : <FaRegStar />}
                        </button>
                    </Tooltip>

                    <Tooltip text={isRealtime ? "알림 설정" : "알림 미지원 종목"} placement="top">
                        <button
                            className={`icon ${bellOn ? "on" : ""}`}
                            onClick={handleBellClick}
                            disabled={!isRealtime}
                        >
                            {bellOn ? <FaBell /> : <FaRegBell />}
                        </button>
                    </Tooltip>
                </div>
            </div>

            <AlertModal open={alertOpen} onClose={handleAlertClose} />
        </>
    );
}
