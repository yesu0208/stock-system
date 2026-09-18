import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { FiSearch } from "react-icons/fi";
import { getOrderHistory, getOrderCancelHistory, getUnfilledOrders, getTradeHistory, cancelOrder } from "../../api/orderHistory";
import { getAutoOrderHistory, getAutoOrderCancelHistory, getAutoOrderUnfilled, cancelAutoOrder } from "../../api/autoOrderHistory";
import { useRealtime } from "../context/RealtimeContext";
import { STOCKS } from "../data/stocks";
import { stockNameMap } from "../../constants/stocks";
import type { OrderHistoryItem, TradeHistoryItem, AutoOrderHistoryItem, HistoryPageResponse } from "../../types/history";
import "./OrderHistoryModal.css";
import Tooltip from "../../tooltip/Tooltip";

type MainTab = "주문" | "취소" | "미체결" | "체결";
type SubTab = "일반" | "자동";

type AnyOrderItem = OrderHistoryItem | AutoOrderHistoryItem | TradeHistoryItem;

function isAutoOrder(item: AnyOrderItem): item is AutoOrderHistoryItem {
    return "autoOrderId" in item;
}
function isTrade(item: AnyOrderItem): item is TradeHistoryItem {
    return "tradeId" in item;
}

function formatTimeParts(isoString: string): { date: string; time: string } {
    const d = new Date(isoString);
    const yy = String(d.getFullYear()).slice(-2);
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const min = String(d.getMinutes()).padStart(2, "0");
    const sec = String(d.getSeconds()).padStart(2, "0");
    return { date: `${yy}.${mm}.${dd}`, time: `${hh}:${min}:${sec}` };
}

function TimeCell({ isoString }: { isoString: string }) {
    const { date, time } = formatTimeParts(isoString);
    return (
        <span className="oh-col oh-time">
            <span className="oh-time-date">{date}</span>
            <span className="oh-time-clock">{time}</span>
        </span>
    );
}

const LEVERAGE_NUM: Record<string, number> = { SPOT: 1, X1_5: 1.5, X2: 2, X2_5: 2.5 };

function leverageClass(leverageRatio: string | null): string {
    const ratio = leverageRatio ?? "SPOT";
    const isCash = ratio === "SPOT";
    const num = LEVERAGE_NUM[ratio] ?? 0;
    return isCash ? "cash" : num === 1.5 ? "lev-2" : num === 2 ? "lev-3" : num === 2.5 ? "lev-5" : "lev-other";
}

function LeverageBadge({ leverageRatio }: { leverageRatio: string | null }) {
    const ratio = leverageRatio ?? "SPOT";
    const isCash = ratio === "SPOT";
    const num = LEVERAGE_NUM[ratio] ?? 0;

    return (
        <span className="oh-col oh-leverage">
            <span className={`oh-leverage-badge ${leverageClass(leverageRatio)}`}>
                {isCash ? "현금" : `${num}x`}
            </span>
        </span>
    );
}

function MarginCell({ notionalValue, initialMargin }: { notionalValue: number | null; initialMargin: number | null }) {
    return (
        <span className="oh-col oh-margin">
            <span className="oh-margin-notional">
                {notionalValue != null ? notionalValue.toLocaleString() : <span className="oh-dash">-</span>}
            </span>
            <span className="oh-margin-initial">
                {initialMargin != null ? initialMargin.toLocaleString() : <span className="oh-dash">-</span>}
            </span>
        </span>
    );
}

function CancelButton({ onCancel }: { onCancel: () => void }) {
    return (
        <span className="oh-col oh-cancel">
            <button
                type="button"
                className="oh-cancel-btn"
                onClick={(e) => {
                    e.stopPropagation();
                    onCancel();
                }}
            >
                취소
            </button>
        </span>
    );
}

interface SearchFilter {
    stockCode: string;
    dateFrom: string;
    dateTo: string;
}

const EMPTY_FILTER: SearchFilter = { stockCode: "", dateFrom: "", dateTo: "" };

function SearchBar({
                       filter,
                       onChange,
                       onReset,
                   }: {
    filter: SearchFilter;
    onChange: (f: SearchFilter) => void;
    onReset: () => void;
}) {
    const [query, setQuery] = useState("");
    const [open, setOpen] = useState(false);
    const wrapRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const selectedStock = filter.stockCode
        ? STOCKS.find((s) => s.code === filter.stockCode)
        : null;

    const filteredStocks = STOCKS.filter((s) =>
        `${s.name} ${s.code}`.toLowerCase().includes(query.toLowerCase())
    );

    const handleSelect = (stock: { code: string; name: string }) => {
        onChange({ ...filter, stockCode: stock.code });
        setQuery("");
        setOpen(false);
    };

    const handleClearStock = () => {
        onChange({ ...filter, stockCode: "" });
        setQuery("");
    };

    const handleDateFromChange = (newFrom: string) => {
        if (newFrom && filter.dateTo && newFrom > filter.dateTo) {
            onChange({ ...filter, dateFrom: newFrom, dateTo: newFrom });
        } else {
            onChange({ ...filter, dateFrom: newFrom });
        }
    };

    const handleDateToChange = (newTo: string) => {
        if (newTo && filter.dateFrom && newTo < filter.dateFrom) {
            onChange({ ...filter, dateFrom: newTo, dateTo: newTo });
        } else {
            onChange({ ...filter, dateTo: newTo });
        }
    };

    return (
        <div className="oh-search-bar">
            <div className="oh-search-keyword-wrap" ref={wrapRef}>
                <FiSearch className="oh-search-icon" />
                {selectedStock ? (
                    <div className="oh-search-selected" onClick={() => setOpen(true)}>
                        <span>{selectedStock.name}</span>
                        <span className="oh-search-selected-code">{selectedStock.code}</span>
                        <button
                            className="oh-search-selected-clear"
                            onClick={(e) => { e.stopPropagation(); handleClearStock(); }}
                        >
                            ✕
                        </button>
                    </div>
                ) : (
                    <input
                        className="oh-search-input oh-search-keyword"
                        type="text"
                        placeholder="종목명·종목코드 검색"
                        value={query}
                        onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
                        onFocus={() => setOpen(true)}
                    />
                )}

                {open && query && !selectedStock && (
                    <div className="oh-search-dropdown">
                        {filteredStocks.length > 0 ? (
                            filteredStocks.slice(0, 8).map((s) => (
                                <div key={s.code} className="oh-search-dropdown-item" onClick={() => handleSelect(s)}>
                                    <span className="oh-search-dropdown-name">{s.name}</span>
                                    <span className="oh-search-dropdown-code">{s.code}</span>
                                </div>
                            ))
                        ) : (
                            <div className="oh-search-dropdown-empty">검색 결과 없음</div>
                        )}
                    </div>
                )}
            </div>

            <input
                className="oh-search-input oh-search-date"
                type="date"
                value={filter.dateFrom}
                max={filter.dateTo || undefined}
                onChange={(e) => handleDateFromChange(e.target.value)}
            />
            <span className="oh-search-sep">~</span>
            <input
                className="oh-search-input oh-search-date"
                type="date"
                value={filter.dateTo}
                min={filter.dateFrom || undefined}
                onChange={(e) => handleDateToChange(e.target.value)}
            />

            <Tooltip text="검색 초기화" placement="top">
                <button className="oh-search-reset" onClick={onReset}>
                    ✕
                </button>
            </Tooltip>
        </div>
    );
}

type Fetcher = (params: { page: number; size: number; stockCode?: string; from?: string; to?: string }) => Promise<HistoryPageResponse<AnyOrderItem>>;

function resolveFetcher(main: MainTab, sub: SubTab): Fetcher {
    if (main === "체결") return getTradeHistory as unknown as Fetcher;
    if (main === "주문") return (sub === "일반" ? getOrderHistory : getAutoOrderHistory) as unknown as Fetcher;
    if (main === "취소") return (sub === "일반" ? getOrderCancelHistory : getAutoOrderCancelHistory) as unknown as Fetcher;
    return (sub === "일반" ? getUnfilledOrders : getAutoOrderUnfilled) as unknown as Fetcher;
}

export default function OrderHistoryModal() {
    const [mainTab, setMainTab] = useState<MainTab>("주문");
    const [subTab, setSubTab] = useState<SubTab>("일반");

    const [filter, setFilter] = useState<SearchFilter>(EMPTY_FILTER);

    const [items, setItems] = useState<AnyOrderItem[]>([]);
    const [page, setPage] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [loading, setLoading] = useState(false);

    const sentinelRef = useRef<HTMLDivElement>(null);
    const listWrapperRef = useRef<HTMLDivElement>(null);

    const { subscribeDestination } = useRealtime();

    const showSubTabs = mainTab !== "체결";
    const isPending = mainTab === "미체결";

    const fetcher = useMemo(() => resolveFetcher(mainTab, showSubTabs ? subTab : "일반"), [mainTab, subTab, showSubTabs]);

    const loadPage = useCallback(async (pageToLoad: number) => {
        setLoading(true);
        try {
            const res = await fetcher({
                page: pageToLoad,
                size: 20,
                ...(mainTab !== "미체결" && {
                    stockCode: filter.stockCode || undefined,
                    from: filter.dateFrom ? `${filter.dateFrom}T00:00:00+09:00` : undefined,
                    to: filter.dateTo ? `${filter.dateTo}T23:59:59+09:00` : undefined,
                }),
            });
            setItems((prev) => (pageToLoad === 0 ? res.items : [...prev, ...res.items]));
            setHasNext(res.hasNext);
            setPage(pageToLoad);
        } catch (e) {
            console.error("[OrderHistoryModal] 목록 조회 실패", e);
        } finally {
            setLoading(false);
        }
    }, [fetcher, filter, mainTab]);

    useEffect(() => {
        if (isPending) return;
        setItems([]);
        setHasNext(false);
        loadPage(0);
    }, [fetcher, isPending, filter.dateFrom, filter.dateTo, filter.stockCode]);

    useEffect(() => {
        if (!isPending) return;

        setItems([]);
        setHasNext(false);

        const loadInitialPending = subTab === "일반"
            ? getUnfilledOrders({ page: 0, size: 100 })
            : getAutoOrderUnfilled({ page: 0, size: 100 });

        loadInitialPending
            .then((res) => setItems(res.items))
            .catch((e) => console.error("[OrderHistoryModal] 미체결 초기 조회 실패", e));

        if (subTab === "일반") {
            return subscribeDestination("/user/sub/order", (data: OrderHistoryItem[]) => {
                setItems(data.filter((o) => o.remainingQuantity > 0));
            });
        }

        return subscribeDestination("/user/sub/auto/order", (data: AutoOrderHistoryItem[]) => {
            setItems(data);
        });
    }, [isPending, subTab, subscribeDestination]);

    useEffect(() => {
        if (isPending) return;
        const sentinel = sentinelRef.current;
        const listWrapper = listWrapperRef.current;
        if (!sentinel || !listWrapper) return;

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting && hasNext && !loading) {
                    loadPage(page + 1);
                }
            },
            { root: listWrapper, threshold: 0.1 }
        );

        observer.observe(sentinel);
        return () => observer.disconnect();
    }, [isPending, hasNext, loading, page, loadPage]);

    const handleMainTabChange = (t: MainTab) => {
        setMainTab(t);
        if (t === "체결") setSubTab("일반");
    };

    const handleCancel = async (auto: boolean, id: number, stockCode: string) => {
        if (!window.confirm("주문을 취소하시겠습니까?")) return;
        try {
            if (auto) {
                await cancelAutoOrder(id, stockCode);
            } else {
                await cancelOrder(id, stockCode);
            }
            setItems((prev) =>
                prev.filter((item) => {
                    if (isTrade(item)) return true;
                    if (auto) return !isAutoOrder(item) || item.autoOrderId !== id;
                    return isAutoOrder(item) || (item as OrderHistoryItem).orderId !== id;
                })
            );
        } catch (e) {
            console.error("[OrderHistoryModal] 취소 실패", e);
            alert("취소에 실패했습니다.");
        }
    };

    const showTrigger = subTab === "자동" && mainTab !== "체결";
    const gridClass = isPending
        ? (showTrigger ? "oh-grid-pending" : "oh-grid-pending-general")
        : (showTrigger ? "oh-grid-cancelled" : "oh-grid-executed");

    const hasFilter = !!(filter.stockCode || filter.dateFrom || filter.dateTo);

    return (
        <div className="oh-modal">
            <div className="oh-tabs">
                {(["주문", "취소", "미체결", "체결"] as MainTab[]).map((t) => (
                    <button
                        key={t}
                        className={`oh-tab ${mainTab === t ? "active" : ""}`}
                        onClick={() => handleMainTabChange(t)}
                    >
                        {t}
                    </button>
                ))}
            </div>

            {showSubTabs && (
                <div className="oh-tabs oh-subtabs">
                    {(["일반", "자동"] as SubTab[]).map((t) => (
                        <button
                            key={t}
                            className={`oh-tab ${subTab === t ? "active" : ""}`}
                            onClick={() => setSubTab(t)}
                        >
                            {t}주문
                        </button>
                    ))}
                </div>
            )}

            <div className="oh-tab-content">
                {!isPending && (
                    <SearchBar
                        filter={filter}
                        onChange={setFilter}
                        onReset={() => setFilter(EMPTY_FILTER)}
                    />
                )}

                {!isPending && (
                    <div className={`oh-header-row ${gridClass}`}>
                        <span className="oh-col oh-time">시간</span>
                        <span className="oh-col oh-name">종목</span>
                        <span className="oh-col oh-side">구분</span>
                        <span className="oh-col oh-leverage">레버리지</span>
                        {showTrigger && <span className="oh-col oh-order-type">주문구분</span>}
                        {showTrigger && <span className="oh-col oh-trigger">감시가</span>}
                        <span className="oh-col oh-qty">수량</span>
                        <span className="oh-col oh-price">가격</span>
                        <span className="oh-col oh-margin oh-header-split">
                            <span className="oh-header-line">주문금액</span>
                            <span className="oh-header-line">개시증거금</span>
                        </span>
                    </div>
                )}

                <div className="oh-list-wrapper" ref={listWrapperRef}>
                    {isPending ? (
                        items.length === 0 && !loading ? (
                            <div className="oh-pending-empty">내역이 없습니다</div>
                        ) : (
                            <ul className="oh-pending-list">
                                {items.map((item) => {
                                    if (isTrade(item)) return null;

                                    const stockCode = item.stockCode;
                                    const stockName = stockNameMap[stockCode] ?? stockCode;
                                    const auto = isAutoOrder(item);
                                    const id = auto ? item.autoOrderId : (item as OrderHistoryItem).orderId;
                                    const side = auto ? item.autoOrderType : (item as OrderHistoryItem).orderType;
                                    const triggerPrice = auto ? (item as AutoOrderHistoryItem).triggerPrice : null;
                                    const remaining = !auto ? (item as OrderHistoryItem).remainingQuantity : item.orderQuantity;
                                    const total = item.orderQuantity;
                                    const fillPct = total > 0 ? Math.min(100, Math.max(0, (remaining / total) * 100)) : 0;
                                    const sideClass = side === "BUY" ? "bar--buy" : "bar--sell";
                                    const { date, time } = formatTimeParts(item.orderTime);

                                    return (
                                        <li key={`${auto ? "auto" : "order"}-${id}`} className="oh-pending-item">
                                            <div className="oh-pending-item__info">
                                                <div className="oh-pending-item__stock-name-row">
                                                    <span className="oh-pending-item__stock-name">{stockName}</span>
                                                    <span className="oh-pending-item__stock-code">{stockCode}</span>
                                                    <span className="oh-pending-item__time">{date} {time}</span>
                                                </div>

                                                <div className="oh-pending-item__top">
                                                    <span className={`oh-pending-item__badge ${side === "BUY" ? "badge--buy" : "badge--sell"}`}>
                                                        {side === "BUY" ? "매수" : "매도"}
                                                    </span>
                                                    <span className={`oh-pending-item__leverage-badge ${leverageClass(item.leverageRatio)}`}>
                                                        {(item.leverageRatio ?? "SPOT") === "SPOT" ? "현금" : `${LEVERAGE_NUM[item.leverageRatio ?? "SPOT"]}x`}
                                                    </span>
                                                    <span className="oh-pending-item__order-type">
                                                        {auto ? "조건부" : "지정가"}
                                                    </span>
                                                </div>

                                                <div className="oh-pending-item__price-row">
                                                    <span className="oh-pending-item__price-label">주문가</span>
                                                    <span className="oh-pending-item__price">{item.orderPrice.toLocaleString()}원</span>
                                                    {auto && triggerPrice != null && (
                                                        <>
                                                            <span className="oh-pending-item__trigger-label">감시가</span>
                                                            <span className="oh-pending-item__trigger-value">{triggerPrice.toLocaleString()}원</span>
                                                        </>
                                                    )}
                                                </div>

                                                <div className="oh-pending-item__qty-row">
                                                    <span className="oh-pending-item__remaining">{remaining.toLocaleString()}</span>
                                                    <span className="oh-pending-item__unit">주</span>
                                                    <span className="oh-pending-item__qty-sep">/</span>
                                                    <span className="oh-pending-item__total"> {total.toLocaleString()}</span>
                                                    <span className="oh-pending-item__unit">주</span>
                                                    <span className={`oh-pending-item__fill-badge ${remaining === total ? "fill-badge--none" : "fill-badge--partial"}`}>
                                                        {remaining === total ? "미체결" : "부분체결"}
                                                    </span>
                                                </div>

                                                <div className="oh-pending-item__bar-track">
                                                    <div
                                                        className={`oh-pending-item__bar-fill ${sideClass}`}
                                                        style={{ width: `${fillPct}%` }}
                                                    />
                                                </div>
                                            </div>

                                            <CancelButton onCancel={() => handleCancel(auto, id, stockCode)} />
                                        </li>
                                    );
                                })}
                            </ul>
                        )
                    ) : (
                        <ul className="oh-list">
                            {items.length === 0 && !loading ? (
                                <li className="oh-empty">
                                    {hasFilter ? "검색 결과가 없습니다" : "내역이 없습니다"}
                                </li>
                            ) : (
                                items.map((item) => {
                                    const stockCode = item.stockCode;
                                    const stockName = stockNameMap[stockCode] ?? stockCode;

                                    if (isTrade(item)) {
                                        return (
                                            <li key={`trade-${item.tradeId}`} className="oh-item oh-grid-executed">
                                                <TimeCell isoString={item.executedAt} />
                                                <span className="oh-col oh-name">
                                                    <span className="oh-stock-name">{stockName}</span>
                                                    <span className="oh-stock-code">{stockCode}</span>
                                                </span>
                                                <span className={`oh-col oh-side ${item.tradeType === "BUY" ? "buy" : "sell"}`}>
                                                    {item.tradeType === "BUY" ? "매수" : "매도"}
                                                </span>
                                                <span className="oh-col oh-leverage">—</span>
                                                <span className="oh-col oh-qty">{item.tradeQuantity.toLocaleString()}주</span>
                                                <span className="oh-col oh-price">{item.tradePrice.toLocaleString()}</span>
                                                <span className="oh-col oh-margin">—</span>
                                            </li>
                                        );
                                    }

                                    const auto = isAutoOrder(item);
                                    const id = auto ? item.autoOrderId : (item as OrderHistoryItem).orderId;
                                    const side = auto ? item.autoOrderType : (item as OrderHistoryItem).orderType;
                                    const time = item.orderTime;

                                    return (
                                        <li key={`${auto ? "auto" : "order"}-${id}`} className={`oh-item ${gridClass}`}>
                                            <TimeCell isoString={time} />
                                            <span className="oh-col oh-name">
                                                <span className="oh-stock-name">{stockName}</span>
                                                <span className="oh-stock-code">{stockCode}</span>
                                            </span>
                                            <span className={`oh-col oh-side ${side === "BUY" ? "buy" : "sell"}`}>
                                                {side === "BUY" ? "매수" : "매도"}
                                            </span>
                                            <LeverageBadge leverageRatio={item.leverageRatio} />
                                            {showTrigger && <span className="oh-col oh-order-type">조건부</span>}
                                            {showTrigger && (
                                                <span className="oh-col oh-trigger">
                                                    {auto && (item as AutoOrderHistoryItem).triggerPrice != null
                                                        ? (item as AutoOrderHistoryItem).triggerPrice.toLocaleString()
                                                        : <span className="oh-dash">-</span>}
                                                </span>
                                            )}
                                            <span className="oh-col oh-qty">{item.orderQuantity.toLocaleString()}주</span>
                                            <span className="oh-col oh-price">{item.orderPrice.toLocaleString()}</span>
                                            <MarginCell notionalValue={item.notionalValue} initialMargin={item.initialMargin} />
                                        </li>
                                    );
                                })
                            )}
                        </ul>
                    )}

                    {!isPending && (
                        <div ref={sentinelRef} className="oh-sentinel">
                            {loading && <span className="oh-loading">불러오는 중…</span>}
                            {!hasNext && items.length > 0 && <span className="oh-end-mark">마지막 데이터입니다</span>}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
