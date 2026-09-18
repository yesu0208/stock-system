import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { FiSearch } from "react-icons/fi";
import { getOrderHistory, getOrderCancelHistory, getUnfilledOrders, getTradeHistory } from "../../api/orderHistory";
import { getAutoOrderHistory, getAutoOrderCancelHistory, getAutoOrderUnfilled } from "../../api/autoOrderHistory";
import { useRealtime } from "../context/RealtimeContext";
import { STOCKS } from "../data/stocks";
import { stockNameMap } from "../../constants/stocks";
import type { OrderHistoryItem, TradeHistoryItem, AutoOrderHistoryItem, HistoryPageResponse } from "../../types/history";
import "./OrderHistoryModal.css";
import Tooltip from "../../tooltip/Tooltip";
import CancelConfirmModal from "./modal/cancel/CancelConfirmModal";

type MainTab = "주문" | "취소" | "미체결" | "체결";
type SubTab = "일반" | "자동";

type AnyOrderItem = OrderHistoryItem | AutoOrderHistoryItem | TradeHistoryItem;
type PendingItem = OrderHistoryItem | AutoOrderHistoryItem;

function isAutoOrder(item: AnyOrderItem): item is AutoOrderHistoryItem {
    return "autoOrderId" in item;
}
function isTrade(item: AnyOrderItem): item is TradeHistoryItem {
    return "tradeId" in item;
}

function pendingItemKey(item: PendingItem): string {
    return isAutoOrder(item) ? `auto-${item.autoOrderId}` : `order-${item.orderId}`;
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

    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);

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
        setSelectedIds(new Set());

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

    const pendingItems: PendingItem[] = isPending
        ? (items.filter((item) => !isTrade(item)) as PendingItem[])
        : [];

    function toggleSelect(key: string) {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            next.has(key) ? next.delete(key) : next.add(key);
            return next;
        });
    }

    function toggleAll() {
        if (selectedIds.size === pendingItems.length) {
            setSelectedIds(new Set());
        } else {
            setSelectedIds(new Set(pendingItems.map(pendingItemKey)));
        }
    }

    function handleCancelClick() {
        if (selectedIds.size === 0) return;
        setCancelConfirmOpen(true);
    }

    function handleCancelConfirmSuccess() {
        setSelectedIds(new Set());
    }

    const selectedOrdersForCancel = pendingItems
        .filter((item) => selectedIds.has(pendingItemKey(item)))
        .map((item) => {
            const auto = isAutoOrder(item);
            return {
                rawId: auto ? item.autoOrderId : (item as OrderHistoryItem).orderId,
                stockCode: item.stockCode,
                isAuto: auto,
                orderId: pendingItemKey(item),
            };
        });

    const showTrigger = subTab === "자동" && mainTab !== "체결";
    const showOrderType = mainTab === "주문" || mainTab === "취소";
    const gridClass = isPending
        ? (showTrigger ? "oh-grid-pending" : "oh-grid-pending-general")
        : mainTab === "체결"
            ? "oh-grid-executed"
            : showTrigger
                ? "oh-grid-cancelled"
                : "oh-grid-with-type";

    const hasFilter = !!(filter.stockCode || filter.dateFrom || filter.dateTo);
    const hasSelection = selectedIds.size > 0;

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

                {isPending ? (
                    <div className="oh-pending-header">
                        <span className="oh-pending-header__count">미체결 주문 {pendingItems.length}건</span>
                        <Tooltip
                            text={selectedIds.size === pendingItems.length && pendingItems.length > 0 ? "전체 취소 해제" : "전체 취소"}
                            placement="top"
                        >
                            <label className="oh-all-check">
                                <input
                                    type="checkbox"
                                    checked={selectedIds.size === pendingItems.length && pendingItems.length > 0}
                                    onChange={toggleAll}
                                />
                                전체 선택
                            </label>
                        </Tooltip>
                    </div>
                ) : (
                    <div className={`oh-header-row ${gridClass}`}>
                        <span className="oh-col oh-time">시간</span>
                        <span className="oh-col oh-name">종목</span>
                        <span className="oh-col oh-side">구분</span>
                        <span className="oh-col oh-leverage">레버리지</span>
                        {showOrderType && <span className="oh-col oh-order-type">주문구분</span>}
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
                        pendingItems.length === 0 && !loading ? (
                            <div className="oh-pending-empty">내역이 없습니다</div>
                        ) : (
                            <ul className="oh-pending-list">
                                {pendingItems.map((item) => {
                                    const stockCode = item.stockCode;
                                    const stockName = stockNameMap[stockCode] ?? stockCode;
                                    const auto = isAutoOrder(item);
                                    const key = pendingItemKey(item);
                                    const side = auto ? item.autoOrderType : (item as OrderHistoryItem).orderType;
                                    const triggerPrice = auto ? (item as AutoOrderHistoryItem).triggerPrice : null;
                                    const remaining = !auto ? (item as OrderHistoryItem).remainingQuantity : item.orderQuantity;
                                    const total = item.orderQuantity;
                                    const fillPct = total > 0 ? Math.min(100, Math.max(0, (remaining / total) * 100)) : 0;
                                    const sideClass = side === "BUY" ? "bar--buy" : "bar--sell";
                                    const { time } = formatTimeParts(item.orderTime);
                                    const selected = selectedIds.has(key);

                                    return (
                                        <li
                                            key={key}
                                            className={`oh-pending-item ${selected ? "oh-pending-item--selected" : ""}`}
                                            onClick={() => toggleSelect(key)}
                                        >
                                            <Tooltip text={selected ? "취소 해제" : "취소"} placement="top">
                                                <input
                                                    type="checkbox"
                                                    checked={selected}
                                                    onChange={() => toggleSelect(key)}
                                                    onClick={(e) => e.stopPropagation()}
                                                />
                                            </Tooltip>

                                            <div className="oh-pending-item__info">
                                                <div className="oh-pending-item__stock-name-row">
                                                    <span className="oh-pending-item__stock-name">{stockName}</span>
                                                    <span className="oh-pending-item__stock-code">{stockCode}</span>
                                                    <span className="oh-pending-item__time">{time}</span>
                                                </div>

                                                <div className="oh-pending-item__top">
                                                    <span className={`oh-pending-item__badge ${side === "BUY" ? "badge--buy" : "badge--sell"}`}>
                                                        {side === "BUY" ? "매수" : "매도"}
                                                    </span>
                                                    <span className={`oh-pending-item__leverage-badge ${leverageClass(item.leverageRatio)}`}>
                                                        {(item.leverageRatio ?? "SPOT") === "SPOT" ? "현금" : `${LEVERAGE_NUM[item.leverageRatio ?? "SPOT"]}x`}
                                                    </span>
                                                    <span className="oh-pending-item__order-type">
                                                        {auto ? "조건부" : (item as OrderHistoryItem).orderExecutionType === "MARKET" ? "시장가" : "지정가"}
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
                                                <LeverageBadge leverageRatio={item.leverageRatio} />
                                                <span className="oh-col oh-qty">{item.tradeQuantity.toLocaleString()}주</span>
                                                <span className="oh-col oh-price">{item.tradePrice.toLocaleString()}</span>
                                                <MarginCell notionalValue={item.notionalValue} initialMargin={item.initialMargin} />
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
                                            <span className="oh-col oh-order-type">
                                                {auto
                                                    ? "조건부"
                                                    : (item as OrderHistoryItem).orderExecutionType === "MARKET" ? "시장가" : "지정가"}
                                            </span>
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

                {isPending && (
                    <div className="oh-pending-footer">
                        <span className={`oh-pending-footer__count ${hasSelection ? "oh-pending-footer__count--active" : ""}`}>
                            {hasSelection ? `${selectedIds.size}건 선택됨` : "항목을 선택하세요"}
                        </span>
                        <button
                            className={`oh-pending-cancel-btn ${!hasSelection ? "oh-pending-cancel-btn--disabled" : ""}`}
                            onClick={handleCancelClick}
                            disabled={!hasSelection}
                        >
                            주문 취소
                        </button>
                    </div>
                )}
            </div>

            <CancelConfirmModal
                open={cancelConfirmOpen}
                onClose={() => setCancelConfirmOpen(false)}
                onConfirm={handleCancelConfirmSuccess}
                count={selectedIds.size}
                selectedOrders={selectedOrdersForCancel}
            />
        </div>
    );
}
