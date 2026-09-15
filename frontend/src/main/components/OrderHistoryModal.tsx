import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { getOrderHistory, getOrderCancelHistory, getUnfilledOrders, getTradeHistory } from "../../api/orderHistory";
import { getAutoOrderHistory, getAutoOrderCancelHistory, getAutoOrderUnfilled, getAutoOrderTriggered } from "../../api/autoOrderHistory";
import { stockNameMap } from "../../constants/stocks";
import type { OrderHistoryItem, TradeHistoryItem, AutoOrderHistoryItem, HistoryPageResponse } from "../../types/history";
import "./OrderHistoryModal.css";

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

function LeverageBadge({ leverageRatio }: { leverageRatio: string | null }) {
    const ratio = leverageRatio ?? "SPOT";
    const isCash = ratio === "SPOT";
    const num = LEVERAGE_NUM[ratio] ?? 0;
    const levClass = isCash ? "cash" : num === 1.5 ? "lev-2" : num === 2 ? "lev-3" : num === 2.5 ? "lev-5" : "lev-other";

    return (
        <span className="oh-col oh-leverage">
            <span className={`oh-leverage-badge ${levClass}`}>
                {isCash ? "현금" : `${num}x`}
            </span>
        </span>
    );
}

function MarginCell({ notionalValue, initialMargin }: { notionalValue: number; initialMargin: number | null }) {
    return (
        <span className="oh-col oh-margin">
            <span className="oh-margin-notional">{notionalValue.toLocaleString()}</span>
            <span className="oh-margin-initial">
                {initialMargin != null ? initialMargin.toLocaleString() : <span className="oh-dash">—</span>}
            </span>
        </span>
    );
}

function LiquidationCell({ maintenanceMarginRate, liquidationPrice }: { maintenanceMarginRate: number | null; liquidationPrice: number | null }) {
    return (
        <span className="oh-col oh-liquidation">
            <span className="oh-liq-rate">
                {maintenanceMarginRate != null ? `${(maintenanceMarginRate * 100).toFixed(0)}%` : <span className="oh-dash">—</span>}
            </span>
            <span className="oh-liq-price">
                {liquidationPrice != null ? liquidationPrice.toLocaleString() : <span className="oh-dash">—</span>}
            </span>
        </span>
    );
}

type Fetcher = (params: { page: number; size: number }) => Promise<HistoryPageResponse<AnyOrderItem>>;

function resolveFetcher(main: MainTab, sub: SubTab): Fetcher {
    if (main === "체결") return getTradeHistory as unknown as Fetcher;
    if (main === "주문") return (sub === "일반" ? getOrderHistory : getAutoOrderHistory) as unknown as Fetcher;
    if (main === "취소") return (sub === "일반" ? getOrderCancelHistory : getAutoOrderCancelHistory) as unknown as Fetcher;
    // 미체결
    return (sub === "일반" ? getUnfilledOrders : getAutoOrderUnfilled) as unknown as Fetcher;
}

export default function OrderHistoryModal() {
    const [mainTab, setMainTab] = useState<MainTab>("주문");
    const [subTab, setSubTab] = useState<SubTab>("일반");

    const [items, setItems] = useState<AnyOrderItem[]>([]);
    const [page, setPage] = useState(0);
    const [hasNext, setHasNext] = useState(false);
    const [loading, setLoading] = useState(false);

    const sentinelRef = useRef<HTMLDivElement>(null);
    const listWrapperRef = useRef<HTMLDivElement>(null);

    const showSubTabs = mainTab !== "체결";

    const fetcher = useMemo(() => resolveFetcher(mainTab, showSubTabs ? subTab : "일반"), [mainTab, subTab, showSubTabs]);

    const loadPage = useCallback(async (pageToLoad: number) => {
        setLoading(true);
        try {
            const res = await fetcher({ page: pageToLoad, size: 20 });
            setItems((prev) => (pageToLoad === 0 ? res.items : [...prev, ...res.items]));
            setHasNext(res.hasNext);
            setPage(pageToLoad);
        } catch (e) {
            console.error("[OrderHistoryModal] 목록 조회 실패", e);
        } finally {
            setLoading(false);
        }
    }, [fetcher]);

    useEffect(() => {
        setItems([]);
        setHasNext(false);
        loadPage(0);
    }, [fetcher]);

    useEffect(() => {
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
    }, [hasNext, loading, page, loadPage]);

    const handleMainTabChange = (t: MainTab) => {
        setMainTab(t);
        if (t === "체결") setSubTab("일반");
    };

    const isPending = mainTab === "미체결";

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
                {isPending ? (
                    <div className="oh-header-row oh-grid-pending">
                        <span className="oh-col oh-time">시간</span>
                        <span className="oh-col oh-name">종목</span>
                        <span className="oh-col oh-side">구분</span>
                        <span className="oh-col oh-leverage">레버리지</span>
                        <span className="oh-col oh-order-type">주문구분</span>
                        <span className="oh-col oh-trigger">감시가</span>
                        <span className="oh-col oh-qty">잔량/수량</span>
                        <span className="oh-col oh-price">가격</span>
                        <span className="oh-col oh-margin">명목가치/증거금</span>
                        <span className="oh-col oh-liquidation">유지증거금율/청산가</span>
                    </div>
                ) : (
                    <div className="oh-header-row oh-grid-executed">
                        <span className="oh-col oh-time">시간</span>
                        <span className="oh-col oh-name">종목</span>
                        <span className="oh-col oh-side">구분</span>
                        <span className="oh-col oh-leverage">레버리지</span>
                        <span className="oh-col oh-qty">수량</span>
                        <span className="oh-col oh-price">가격</span>
                        <span className="oh-col oh-margin">명목가치/증거금</span>
                    </div>
                )}

                <div className="oh-list-wrapper" ref={listWrapperRef}>
                    <ul className="oh-list">
                        {items.length === 0 && !loading ? (
                            <li className="oh-empty">내역이 없습니다.</li>
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

                                if (isPending) {
                                    const triggerPrice = auto ? (item as AutoOrderHistoryItem).triggerPrice : null;
                                    const remainingQuantity = !auto
                                        ? (item as OrderHistoryItem).remainingQuantity
                                        : item.orderQuantity;

                                    return (
                                        <li key={`${auto ? "auto" : "order"}-${id}`} className="oh-item oh-grid-pending">
                                            <TimeCell isoString={time} />
                                            <span className="oh-col oh-name">
                                                <span className="oh-stock-name">{stockName}</span>
                                                <span className="oh-stock-code">{stockCode}</span>
                                            </span>
                                            <span className={`oh-col oh-side ${side === "BUY" ? "buy" : "sell"}`}>
                                                {side === "BUY" ? "매수" : "매도"}
                                            </span>
                                            <LeverageBadge leverageRatio={item.leverageRatio} />
                                            <span className="oh-col oh-order-type">{auto ? "조건부" : "지정가"}</span>
                                            <span className="oh-col oh-trigger">
                                                {triggerPrice != null ? triggerPrice.toLocaleString() : <span className="oh-dash">—</span>}
                                            </span>
                                            <span className="oh-col oh-qty">
                                                {remainingQuantity.toLocaleString()}
                                                <span className="oh-qty-sep">/</span>
                                                {item.orderQuantity.toLocaleString()}
                                            </span>
                                            <span className="oh-col oh-price">{item.orderPrice.toLocaleString()}</span>
                                            <MarginCell notionalValue={item.notionalValue} initialMargin={item.initialMargin} />
                                            <LiquidationCell
                                                maintenanceMarginRate={item.maintenanceMarginRate}
                                                liquidationPrice={item.liquidationPrice}
                                            />
                                        </li>
                                    );
                                }

                                return (
                                    <li key={`${auto ? "auto" : "order"}-${id}`} className="oh-item oh-grid-executed">
                                        <TimeCell isoString={time} />
                                        <span className="oh-col oh-name">
                                            <span className="oh-stock-name">{stockName}</span>
                                            <span className="oh-stock-code">{stockCode}</span>
                                        </span>
                                        <span className={`oh-col oh-side ${side === "BUY" ? "buy" : "sell"}`}>
                                            {side === "BUY" ? "매수" : "매도"}
                                        </span>
                                        <LeverageBadge leverageRatio={item.leverageRatio} />
                                        <span className="oh-col oh-qty">{item.orderQuantity.toLocaleString()}주</span>
                                        <span className="oh-col oh-price">{item.orderPrice.toLocaleString()}</span>
                                        <MarginCell notionalValue={item.notionalValue} initialMargin={item.initialMargin} />
                                    </li>
                                );
                            })
                        )}
                    </ul>

                    <div ref={sentinelRef} className="oh-sentinel">
                        {loading && <span className="oh-loading">불러오는 중…</span>}
                        {!hasNext && items.length > 0 && <span className="oh-end-mark">마지막 데이터입니다</span>}
                    </div>
                </div>
            </div>
        </div>
    );
}
