import { useState, useEffect, useRef, useCallback } from "react";
import ModalV2 from "../../../components/ModalV2";
import "./AdvancedOrder.css";
import { useStock } from "../../context/StockContext";
import { useRealtime } from "../../context/RealtimeContext";
import { useStockRealtime } from "../../context/StockRealtimeContext";
import { useAccount } from "../../context/AccountContext";
import { STOCKS } from "../../data/stocks";
import type { StockInfo } from "../../types/stock";
import MiniLineChart from "./advancedorder/MiniLineChart";
import { useUser } from "../../context/UserContext";
import type { RankTier } from "../../../types/rank";

import { AdvancedOrderProvider, useAdvancedOrders,
    type TrailingStopPendingOrder, type OtocoPendingOrder }
    from "../../context/AdvancedOrderContext";

import { placeTrailingStop, cancelTrailingStop } from "../../../api/trailingStop";
import { placeOtocoOrder, cancelOtocoOrder } from "../../../api/otoco";
import type { TrailingStopType } from "../../../types/trailingStop";
import type { OtocoEntryDirection as ApiOtocoEntryDirection, OtocoExitMode as ApiOtocoExitMode } from "../../../types/otoco";

import {
    isRealtimeStock,
    DIRECTION_CLASS,
    calcStockStats,
} from "../../../utils/stockUtils";
import { FaSearch } from "react-icons/fa";

import TrailingOrderConfirmModal, {
    type TrailingConfirmData,
} from "./advancedorder/TrailingOrderConfirmModal";
import OtocoOrderConfirmModal, {
    type OtocoConfirmData,
} from "./advancedorder/OtocoOrderConfirmModal";
import Tooltip from "../../../tooltip/Tooltip";

type TradeTab = "buy" | "sell";
type OuterTab = "trailing" | "otoco";
type InnerTab = "info" | "order" | "pending";
type EntryDirection = "above" | "below";
type ExitMode = "price" | "pct";

const RATIO_OPTIONS: { label: string; ratio: number }[] = [
    { label: "10%",  ratio: 0.1  },
    { label: "25%",  ratio: 0.25 },
    { label: "50%",  ratio: 0.5  },
    { label: "75%",  ratio: 0.75 },
    { label: "전체", ratio: 1    },
];

const LEVERAGE_OPTIONS: { leverage: number; label: string; marginRate: number }[] = [
    { leverage: 1.5, label: "1.5x", marginRate: 66.7 },
    { leverage: 2,   label: "2x",   marginRate: 50   },
    { leverage: 2.5, label: "2.5x", marginRate: 40   },
];

const RANK_ORDER: RankTier[] = ["UNRANKED", "BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND", "MASTER"];

const LEVERAGE_REQUIRED_TIER: Record<number, RankTier | null> = {
    1.5: null,
    2:   "GOLD",
    2.5: "PLATINUM",
};

function isRankAtLeast(current: RankTier | undefined, required: RankTier): boolean {
    if (!current) return false;
    return RANK_ORDER.indexOf(current) >= RANK_ORDER.indexOf(required);
}

function toLeverageRatio(leverage: number): "X1_5" | "X2" | "X2_5" | null {
    switch (leverage) {
        case 1.5: return "X1_5";
        case 2:   return "X2";
        case 2.5: return "X2_5";
        default:  return null;
    }
}

const STOP_MIN  = 0.1;
const STOP_MAX  = 30;
const STOP_STEP = 0.1;
const DEFAULT_BUY_STOP  = 3.0;
const DEFAULT_SELL_STOP = 5.0;

const MAX_QTY = 99_999_999;
const GAP_PCT = 0.08;

const OTOCO_MIN_PCT     = 0.1;
const OTOCO_MAX_PCT     = 50;
const OTOCO_STEP_PCT    = 0.1;
const DEFAULT_TP_PCT    = 5.0;
const DEFAULT_SL_PCT    = 3.0;

interface ScenarioOption<T extends string> {
    value: T;
    label: string;
    icon?: React.ReactNode;
}

function getTickSize(price: number): number {
    if (price < 2_000) return 1;
    if (price < 5_000) return 5;
    if (price < 20_000) return 10;
    if (price < 50_000) return 50;
    if (price < 200_000) return 100;
    if (price < 500_000) return 500;
    return 1_000;
}

function getTickBaseline(price: number): number {
    if (price < 2_000) return 0;
    if (price < 5_000) return 2_000;
    if (price < 20_000) return 5_000;
    if (price < 50_000) return 20_000;
    if (price < 200_000) return 50_000;
    if (price < 500_000) return 200_000;
    return 500_000;
}

function snapToTick(price: number): number {
    const clamped = Math.min(MAX_QTY, Math.max(0, price));
    const tick = getTickSize(clamped);
    const baseline = getTickBaseline(clamped);
    const offset = Math.round((clamped - baseline) / tick) * tick;
    return Math.min(MAX_QTY, Math.max(0, baseline + offset));
}

function stepPrice(current: number, direction: 1 | -1): number {
    const tick = getTickSize(current);
    const next = current + direction * tick;
    return snapToTick(next);
}

function ScenarioSelect<T extends string>({
                                              options,
                                              value,
                                              onChange,
                                              ariaLabel,
                                          }: {
    options: ScenarioOption<T>[];
    value: T;
    onChange: (v: T) => void;
    ariaLabel?: string;
}) {
    const [open, setOpen] = useState(false);
    const rootRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (rootRef.current && !rootRef.current.contains(e.target as Node)) {
                setOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
    }, []);

    const current = options.find(o => o.value === value);

    return (
        <div className="ao-scenario" ref={rootRef}>
            <button
                type="button"
                className={`ao-scenario__trigger ${open ? "ao-scenario__trigger--open" : ""}`}
                onClick={() => setOpen(v => !v)}
                aria-haspopup="listbox"
                aria-expanded={open}
                aria-label={ariaLabel}
            >
                {current?.label}
                <svg
                    className={`ao-scenario__chevron ${open ? "ao-scenario__chevron--open" : ""}`}
                    width="10" height="10" viewBox="0 0 10 10" fill="none"
                >
                    <path d="M1.5 3.5l3.5 3.5 3.5-3.5"
                          stroke="currentColor" strokeWidth="1.5"
                          strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
            </button>

            <div
                className={`ao-scenario__menu ${open ? "ao-scenario__menu--open" : ""}`}
                role="listbox"
            >
                {options.map((opt, i) => (
                    <div key={opt.value}>
                        {i > 0 && <div className="ao-scenario__divider" />}
                        <button
                            type="button"
                            role="option"
                            aria-selected={opt.value === value}
                            className={`ao-scenario__option ${opt.value === value ? "ao-scenario__option--active" : ""}`}
                            onClick={() => {
                                onChange(opt.value);
                                setOpen(false);
                            }}
                        >
                            {opt.icon}
                            {opt.label}
                        </button>
                    </div>
                ))}
            </div>
        </div>
    );
}

function formatNumber(value: number): string {
    return value.toLocaleString();
}

function parseNumber(value: string): number {
    const onlyDigits = value.replace(/[^0-9]/g, "");
    return onlyDigits === "" ? 0 : Number(onlyDigits);
}

function clampQty(value: number): number {
    return Math.min(MAX_QTY, Math.max(0, value));
}

function clampStop(value: number): number {
    return Math.min(STOP_MAX, Math.max(STOP_MIN, Math.round(value * 10) / 10));
}

function clampOtocoPct(value: number): number {
    return Math.min(OTOCO_MAX_PCT, Math.max(OTOCO_MIN_PCT, Math.round(value * 10) / 10));
}

function calcPct(value: number): string {
    const pct = ((value - STOP_MIN) / (STOP_MAX - STOP_MIN)) * 100;
    return `${Math.min(100, Math.max(0, pct)).toFixed(1)}%`;
}

function formatTimestamp(iso: string): string {
    const d = new Date(iso);
    const pad = (n: number) => String(n).padStart(2, "0");
    return (
        `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}` +
        `  ${pad(d.getHours())}:${pad(d.getMinutes())}`
    );
}

function LeverageBadge({ leverage }: { leverage: number }) {
    const isCash = leverage === 1;
    const levClass =
        isCash           ? "cash"  :
            leverage === 1.5 ? "lev-2" :
                leverage === 2   ? "lev-3" :
                    leverage === 2.5 ? "lev-5" :
                        "lev-other";
    return (
        <span className={`ao-pending-badge__leverage ${levClass}`}>
            {isCash ? "현금" : `${leverage}x`}
        </span>
    );
}

function LeverageTag({ leverage }: { leverage: number }) {
    const isCash = leverage === 1;
    const levClass =
        isCash           ? "cash"  :
            leverage === 1.5 ? "lev-2" :
                leverage === 2   ? "lev-3" :
                    leverage === 2.5 ? "lev-5" :
                        "lev-other";
    return (
        <span className={`ao-pending-badge__leverage ${levClass}`}>
            {isCash ? "현금" : `${leverage}x`}
        </span>
    );
}

function SimpleCancelConfirmModal({
                                      open, count, onClose, onConfirm,
                                  }: {
    open: boolean;
    count: number;
    onClose: () => void;
    onConfirm: () => void | Promise<void>;
}) {
    const [loading, setLoading] = useState(false);

    async function handleConfirm() {
        setLoading(true);
        try {
            await onConfirm();
        } finally {
            setLoading(false);
            onClose();
        }
    }

    return (
        <ModalV2 open={open} title="주문 취소" onClose={onClose}>
            <div style={{ padding: 20 }}>
                <p style={{ fontSize: 13, color: "var(--color-text-secondary, #8d929b)", margin: "0 0 16px" }}>
                    선택하신 {count}건의 주문을 취소하시겠습니까?
                </p>
                <div style={{ display: "flex", gap: 8 }}>
                    <button
                        type="button"
                        onClick={handleConfirm}
                        disabled={loading}
                        style={{
                            flex: 1, padding: "10px 0", borderRadius: 8, border: "none",
                            background: "#22c07a", color: "#fff", fontWeight: 700,
                            fontSize: 13, cursor: loading ? "not-allowed" : "pointer",
                        }}
                    >
                        {loading ? "취소 처리 중…" : "취소 확정"}
                    </button>
                    <button
                        type="button"
                        onClick={onClose}
                        disabled={loading}
                        style={{
                            flex: 1, padding: "10px 0", borderRadius: 8,
                            border: "1px solid var(--color-border, #2e3138)", background: "transparent",
                            color: "var(--color-text-secondary, #8d929b)", fontWeight: 600,
                            fontSize: 13, cursor: loading ? "not-allowed" : "pointer",
                        }}
                    >
                        닫기
                    </button>
                </div>
            </div>
        </ModalV2>
    );
}

interface AdvancedOrderProps {
    open: boolean;
    onClose: () => void;
}

export default function AdvancedOrder({ open, onClose }: AdvancedOrderProps) {
    const [outerTab, setOuterTab] = useState<OuterTab>("trailing");
    const [innerTab, setInnerTab] = useState<InnerTab>("info");

    return (
        <ModalV2 open={open} title="고급 주문" onClose={onClose}>
            <AdvancedOrderProvider>
                <div className="advanced-order">
                    <div className="ao-outer-tabs">
                        <button
                            className={`ao-outer-tab ${outerTab === "trailing" ? "ao-outer-tab--active" : ""}`}
                            onClick={() => setOuterTab("trailing")}
                        >
                            트레일링 스탑
                        </button>
                        <button
                            className={`ao-outer-tab ${outerTab === "otoco" ? "ao-outer-tab--active" : ""}`}
                            onClick={() => setOuterTab("otoco")}
                        >
                            OTOCO
                        </button>
                    </div>

                    <div className="ao-inner-tabs">
                        <button
                            className={`ao-inner-tab ${innerTab === "info" ? "ao-inner-tab--active" : ""}`}
                            onClick={() => setInnerTab("info")}
                        >
                            주문 설명
                        </button>
                        <button
                            className={`ao-inner-tab ${innerTab === "order" ? "ao-inner-tab--active" : ""}`}
                            onClick={() => setInnerTab("order")}
                        >
                            주문
                        </button>
                        <button
                            className={`ao-inner-tab ${innerTab === "pending" ? "ao-inner-tab--active" : ""}`}
                            onClick={() => setInnerTab("pending")}
                        >
                            미체결
                        </button>
                    </div>

                    <div className="ao-content">
                        {outerTab === "trailing" ? (
                            <>
                                {innerTab === "info"    && <TrailingStopInfo />}
                                {innerTab === "order"   && <TrailingOrderLayout />}
                                {innerTab === "pending" && <TrailingStopPendingList />}
                            </>
                        ) : (
                            <>
                                {innerTab === "info"    && <OtocoInfo />}
                                {innerTab === "order"   && <OtocoOrderLayout />}
                                {innerTab === "pending" && <OtocoPendingList />}
                            </>
                        )}
                    </div>
                </div>
            </AdvancedOrderProvider>
        </ModalV2>
    );
}

function TrailingOrderLayout() {
    const [mode, setMode] = useState<TradeTab>("buy");
    const isBuy = mode === "buy";

    return (
        <div className="adv-order__layout">
            <div className="adv-order__body">
                <StockInfoPanel />

                <div className="adv-order__order-wrap">
                    <div className="otoco-section__header adv-order__cond-header">
                        <span className="otoco-section__title">매수/매도 조건</span>
                    </div>

                    <label className="adv-order__mode-toggle">
                        <input
                            type="checkbox"
                            className="adv-order__mode-checkbox"
                            checked={isBuy}
                            onChange={(e) => setMode(e.target.checked ? "buy" : "sell")}
                            aria-label="매수/매도 전환"
                        />
                        <span className={`adv-order__mode-label adv-order__mode-label--buy ${isBuy ? "adv-order__mode-label--active" : ""}`}>
                            매수
                        </span>
                        <span className={`adv-order__mode-label adv-order__mode-label--sell ${!isBuy ? "adv-order__mode-label--active" : ""}`}>
                            매도
                        </span>
                    </label>

                    <OrderInputPanel mode={mode} />
                </div>
            </div>
        </div>
    );
}

function StockInfoPanel() {
    const { selectedStock, setSelectedStock } = useStock();
    const { priceTick } = useStockRealtime();
    const { subscribeStock } = useRealtime();

    const [detailTick, setDetailTick] = useState<any>(null);

    useEffect(() => {
        setDetailTick(null);
        return subscribeStock(selectedStock.code, (tick: any) => {
            if (tick.tickMessageType === "DETAIL") {
                setDetailTick({ ...tick, prevClosePrice: tick.prevPrice });
            }
        });
    }, [selectedStock.code, subscribeStock]);

    const [query, setQuery] = useState("");
    const [searchOpen, setSearchOpen] = useState(false);

    const filteredStocks = STOCKS.filter((s) =>
        `${s.name} ${s.code}`.toLowerCase().includes(query.toLowerCase())
    );

    const handleSelect = (stock: StockInfo) => {
        setSelectedStock(stock);
        setQuery("");
        setSearchOpen(false);
    };

    const isRealtime = isRealtimeStock(selectedStock.code);
    const stats      = calcStockStats(isRealtime, priceTick, detailTick);

    return (
        <div className="adv-order__stock-panel">

            <div className="adv-order__search-box">
                <span className="adv-order__search-icon">
                    <FaSearch />
                </span>
                <input
                    className="adv-order__search-input"
                    value={query}
                    placeholder="종목명·코드 검색"
                    onChange={(e) => {
                        setQuery(e.target.value);
                        setSearchOpen(true);
                    }}
                    onFocus={() => setSearchOpen(true)}
                />
                {searchOpen && query && (
                    <div className="adv-order__search-dropdown">
                        {filteredStocks.length > 0 ? (
                            filteredStocks.map((s) => {
                                const tradable = s.realtimeSupported;
                                return (
                                    <div
                                        key={s.code}
                                        className={`adv-order__search-item${tradable ? " adv-order__search-item--tradable" : " adv-order__search-item--disabled"}`}
                                        onClick={() => tradable && handleSelect(s)}
                                    >
                                        <div className="adv-order__search-item-left">
                                            <span className="adv-order__search-item-name">{s.name}</span>
                                            <span className="adv-order__search-item-code">{s.code}</span>
                                        </div>
                                        <span className={`adv-order__search-item-tag ${tradable ? "adv-order__search-item-tag--tradable" : "adv-order__search-item-tag--disabled"}`}>
                                            {tradable ? "주문 가능" : "주문 불가"}
                                        </span>
                                    </div>
                                );
                            })
                        ) : (
                            <div className="adv-order__search-empty">검색 결과 없음</div>
                        )}
                    </div>
                )}
            </div>

            <div className="adv-order__stock-name-row">
                <img
                    key={selectedStock.code}
                    className="adv-order__stock-mark"
                    src={`https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${selectedStock.code}.svg`}
                    alt={selectedStock.name ?? ""}
                    onError={(e) => {
                        const img = e.currentTarget as HTMLImageElement;
                        if (img.dataset.fallback) return;
                        img.dataset.fallback = "1";
                        img.src = "/marks/default.svg";
                    }}
                />
                <div className="adv-order__stock-name-block">
                    <span className="adv-order__stock-name">
                        {selectedStock.name ?? "—"}
                    </span>
                    <span className="adv-order__stock-code">
                        {selectedStock.code ?? "—"}
                    </span>
                </div>
            </div>

            <div className="adv-order__stock-divider" />

            <div className="adv-order__price-block">
                <span className={`adv-order__current-price ${stats ? DIRECTION_CLASS[stats.cur.direction] : ""}`}>
                    {stats?.cur.price ?? "—"}
                    <span className="adv-order__price-unit"></span>
                </span>
                <div className="adv-order__price-change-row">
                    <span className={`adv-order__price-change ${stats ? DIRECTION_CLASS[stats.cur.direction] : "flat"}`}>
                        {stats?.cur.diffText ?? "—"}
                    </span>
                    <span className={`adv-order__price-rate ${stats ? DIRECTION_CLASS[stats.cur.direction] : "flat"}`}>
                        {stats?.cur.rateText ? `(${stats.cur.rateText})` : ""}
                    </span>
                </div>
            </div>

            <div className="adv-order__mini-chart">
                <MiniLineChart
                    code={selectedStock.code}
                    backgroundColor="transparent"
                />
            </div>

            <div className="adv-order__stock-divider" />

            <div className="adv-order__stock-meta">

                <div className="adv-order__meta-row">
                    <span className="adv-order__meta-label">시가</span>
                    <span className="adv-order__meta-value-group">
                        <span className={`adv-order__meta-value ${stats ? DIRECTION_CLASS[stats.open.direction] : ""}`}>
                            {stats?.open.price ?? "—"}
                        </span>
                        {stats?.open.rateText && (
                            <span className={`adv-order__meta-rate ${DIRECTION_CLASS[stats.open.direction]}`}>
                                ({stats.open.rateText})
                            </span>
                        )}
                    </span>
                </div>

                <div className="adv-order__meta-row">
                    <span className="adv-order__meta-label">고가</span>
                    <span className="adv-order__meta-value-group">
                        <span className={`adv-order__meta-value ${stats ? DIRECTION_CLASS[stats.high.direction] : ""}`}>
                            {stats?.high.price ?? "—"}
                        </span>
                        {stats?.high.rateText && (
                            <span className={`adv-order__meta-rate ${DIRECTION_CLASS[stats.high.direction]}`}>
                                ({stats.high.rateText})
                            </span>
                        )}
                    </span>
                </div>

                <div className="adv-order__meta-row">
                    <span className="adv-order__meta-label">저가</span>
                    <span className="adv-order__meta-value-group">
                        <span className={`adv-order__meta-value ${stats ? DIRECTION_CLASS[stats.low.direction] : ""}`}>
                            {stats?.low.price ?? "—"}
                        </span>
                        {stats?.low.rateText && (
                            <span className={`adv-order__meta-rate ${DIRECTION_CLASS[stats.low.direction]}`}>
                                ({stats.low.rateText})
                            </span>
                        )}
                    </span>
                </div>

                <div className="adv-order__meta-row">
                    <span className="adv-order__meta-label">거래량</span>
                    <span className="adv-order__meta-value">
                        {stats?.volume ?? "—"}
                        <span className="adv-order__meta-unit">주</span>
                    </span>
                </div>

                <div className="adv-order__meta-row">
                    <span className="adv-order__meta-label">거래대금</span>
                    <span className="adv-order__meta-value">
                        {stats?.tradingValue ?? "—"}
                        <span className="adv-order__meta-unit">백만</span>
                    </span>
                </div>

            </div>
        </div>
    );
}

function OrderInputPanel({ mode }: { mode: TradeTab }) {
    const [quantity, setQuantity] = useState<number>(0);
    const [buyStop,  setBuyStop]  = useState<number>(DEFAULT_BUY_STOP);
    const [sellStop, setSellStop] = useState<number>(DEFAULT_SELL_STOP);
    const [isCredit, setIsCredit] = useState<boolean>(false);
    const [leverage, setLeverage] = useState<number>(1.5);

    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmData, setConfirmData] = useState<TrailingConfirmData | null>(null);

    const selectedLeverageOption =
        LEVERAGE_OPTIONS.find((opt) => opt.leverage === leverage) ?? LEVERAGE_OPTIONS[0];

    const isBuy       = mode === "buy";
    const accentClass = isBuy ? "accent--buy" : "accent--sell";

    const { account }       = useAccount();
    const { selectedStock } = useStock();
    const { priceTick }     = useStockRealtime();
    const { user }          = useUser();

    const currentPrice = priceTick?.curPrice ?? 0;

    const expectedFillPrice = isBuy
        ? currentPrice * (1 + buyStop  / 100)
        : currentPrice * (1 - sellStop / 100);

    const expectedAmountRaw   = Math.round(expectedFillPrice * quantity);
    const expectedAmountAfter = (isBuy && isCredit)
        ? Math.floor(expectedAmountRaw / leverage)
        : expectedAmountRaw;
    const showEstimatedBeforeAfter = isBuy && isCredit;

    const holding = account?.stocks[selectedStock.code];
    const orderableAmount = account?.availableCash ?? 0;
    const holdingAvailableQty = holding?.availableQuantity ?? 0;

    function handleRatioClick(ratio: number) {
        if (isBuy) {
            if (currentPrice <= 0) { setQuantity(0); return; }
            const qty = Math.floor((orderableAmount * ratio) / currentPrice);
            setQuantity(clampQty(qty));
        } else {
            const qty = Math.floor(holdingAvailableQty * ratio);
            setQuantity(clampQty(qty));
        }
    }

    function openConfirm() {
        setConfirmData({
            stockName:   selectedStock.name ?? "",
            stockCode:   selectedStock.code ?? "",
            side:        isBuy ? "BUY" : "SELL",
            credit:      isCredit,
            leverage:    isCredit ? leverage : 1,
            quantity,
            stopPercent: isBuy ? buyStop : sellStop,
            basePrice:   currentPrice,
        });
        setConfirmOpen(true);
    }

    async function handleOrder() {
        await placeTrailingStop({
            stockCode: selectedStock.code,
            trailingStopType: (isBuy ? "BUY" : "SELL") as TrailingStopType,
            orderQuantity: quantity,
            stopPercent: isBuy ? buyStop : sellStop,
            basePrice: Math.round(currentPrice),
            leverageRatio: isCredit ? toLeverageRatio(leverage) : null,
        });
    }

    const buySliderRef  = useRef<HTMLInputElement>(null);
    const sellSliderRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (buySliderRef.current) {
            buySliderRef.current.style.setProperty("--pct", calcPct(buyStop));
        }
    }, [buyStop]);

    useEffect(() => {
        if (sellSliderRef.current) {
            sellSliderRef.current.style.setProperty("--pct", calcPct(sellStop));
        }
    }, [sellStop]);

    const orderableLabel = isBuy
        ? `${orderableAmount.toLocaleString()} 원`
        : `${holdingAvailableQty.toLocaleString()} 주`;

    const requiredTier = isCredit ? LEVERAGE_REQUIRED_TIER[leverage] : null;
    const isLeverageAllowed = !requiredTier || isRankAtLeast(user?.rank?.tier, requiredTier);

    const leverageDisabledReason =
        isCredit && !isLeverageAllowed ? `${requiredTier} 등급 이상 가능` : null;
    const quantityDisabledReason =
        quantity <= 0 ? "수량 입력 필요" : null;

    const disabledTooltipText = leverageDisabledReason ?? quantityDisabledReason ?? "";
    const isOrderDisabled = leverageDisabledReason !== null || quantityDisabledReason !== null;

    return (
        <div className="adv-order__order-panel">

            <div className="ratio-group">
                {RATIO_OPTIONS.map(({ label, ratio }) => (
                    <button
                        key={label}
                        type="button"
                        className={`ratio-btn ${accentClass}`}
                        onClick={() => handleRatioClick(ratio)}
                    >
                        {label}
                    </button>
                ))}
            </div>

            <div className="form-row">
                <span className="form-label">수량</span>
                <div className="stepper">
                    <button
                        className={`stepper__btn ${accentClass}`}
                        onClick={() => setQuantity((q) => clampQty(q - 1))}
                    >
                        −
                    </button>
                    <input
                        className="stepper__input"
                        type="text"
                        inputMode="numeric"
                        value={formatNumber(quantity)}
                        onChange={(e) => setQuantity(clampQty(parseNumber(e.target.value)))}
                    />
                    <button
                        className={`stepper__btn ${accentClass}`}
                        onClick={() => setQuantity((q) => clampQty(q + 1))}
                    >
                        +
                    </button>
                    <span className="stepper__unit">주</span>
                </div>
            </div>

            <div className="divider" />

            <div className={`adv-order__stop-row ${!isBuy ? "adv-order__stop-row--disabled" : ""}`}>
                <span className="adv-order__stop-label">매수 스탑</span>
                <div className="adv-order__stop-control">
                    <input
                        className="adv-order__stop-input"
                        type="number"
                        min={STOP_MIN}
                        max={STOP_MAX}
                        step={STOP_STEP}
                        value={buyStop}
                        disabled={!isBuy}
                        onChange={(e) => setBuyStop(clampStop(Number(e.target.value)))}
                    />
                    <span className="adv-order__stop-unit">%</span>
                    <input
                        ref={buySliderRef}
                        className="adv-order__slider slider--buy"
                        type="range"
                        min={STOP_MIN}
                        max={STOP_MAX}
                        step={STOP_STEP}
                        value={buyStop}
                        disabled={!isBuy}
                        onChange={(e) => setBuyStop(clampStop(Number(e.target.value)))}
                        aria-label={`매수 스탑 비율 ${buyStop}%`}
                    />
                    <span className="adv-order__stop-chip chip--buy">
                        {buyStop.toFixed(1)}%
                    </span>
                </div>
            </div>

            <div className={`adv-order__stop-row ${isBuy ? "adv-order__stop-row--disabled" : ""}`}>
                <span className="adv-order__stop-label">매도 스탑</span>
                <div className="adv-order__stop-control">
                    <input
                        className="adv-order__stop-input"
                        type="number"
                        min={STOP_MIN}
                        max={STOP_MAX}
                        step={STOP_STEP}
                        value={sellStop}
                        disabled={isBuy}
                        onChange={(e) => setSellStop(clampStop(Number(e.target.value)))}
                    />
                    <span className="adv-order__stop-unit">%</span>
                    <input
                        ref={sellSliderRef}
                        className="adv-order__slider slider--sell"
                        type="range"
                        min={STOP_MIN}
                        max={STOP_MAX}
                        step={STOP_STEP}
                        value={sellStop}
                        disabled={isBuy}
                        onChange={(e) => setSellStop(clampStop(Number(e.target.value)))}
                        aria-label={`매도 스탑 비율 ${sellStop}%`}
                    />
                    <span className="adv-order__stop-chip chip--sell">
                        {sellStop.toFixed(1)}%
                    </span>
                </div>
            </div>

            <div className="adv-order__summary">
                <div className="adv-order__summary-row">
                    <span className="adv-order__summary-label">
                        {isBuy ? "주문가능금액" : "주문가능수량"}
                    </span>
                    <span className="adv-order__summary-value">{orderableLabel}</span>
                </div>

                <div className="adv-order__summary-row">
                    <span className="adv-order__summary-label">
                        예상 체결가
                        <span className="adv-order__summary-hint">(현재가 기준)</span>
                    </span>
                    <span
                        className={`adv-order__summary-value adv-order__summary-value--fill ${
                            isBuy ? "adv-order__summary-value--fill-buy" : "adv-order__summary-value--fill-sell"
                        }`}
                    >
                        {currentPrice > 0 ? `${Math.round(expectedFillPrice).toLocaleString()} 원` : "—"}
                    </span>
                </div>

                <div className="adv-order__summary-row">
                    <span className="adv-order__summary-label">
                        예상 금액
                        <LeverageTag leverage={isCredit ? leverage : 1} />
                    </span>
                    <span
                        className={`adv-order__summary-value adv-order__summary-value--fill ${
                            isBuy ? "adv-order__summary-value--fill-buy" : "adv-order__summary-value--fill-sell"
                        }`}
                    >
                        {showEstimatedBeforeAfter ? (
                            <>
                                <span className="adv-order__summary-value--after">
                                    {expectedAmountAfter.toLocaleString()} 원
                                </span>
                                <span className="adv-order__summary-value--before">
                                    {expectedAmountRaw.toLocaleString()} 원
                                </span>
                            </>
                        ) : (
                            `${expectedAmountAfter.toLocaleString()} 원`
                        )}
                    </span>
                </div>
            </div>

            <div className="order-btn-row">
                <div className={`credit-area ${isCredit ? "credit-area--active" : ""}`}>
                    <div className="credit-area__row1">
                        <Tooltip text={isCredit ? "신용 해제" : "신용 적용"} placement="top">
                            <label className={`credit-check ${isCredit ? "credit-check--active" : ""}`}>
                                <input
                                    type="checkbox"
                                    checked={isCredit}
                                    onChange={(e) => setIsCredit(e.target.checked)}
                                />
                                신용
                            </label>
                        </Tooltip>
                        <div className="leverage-group">
                            {LEVERAGE_OPTIONS.map(({ leverage: lv, label }) => {
                                const levClass =
                                    lv === 1.5 ? "lev-2" :
                                        lv === 2   ? "lev-3" :
                                            lv === 2.5 ? "lev-5" : "";
                                return (
                                    <button
                                        key={lv}
                                        type="button"
                                        disabled={!isCredit}
                                        className={`leverage-btn ${levClass} ${
                                            isCredit && leverage === lv ? "leverage-btn--active" : ""
                                        }`}
                                        onClick={() => setLeverage(lv)}
                                    >
                                        {label}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                    <div className={`credit-area__row2 ${!isCredit ? "credit-area__row2--disabled" : ""}`}>
                        <span className="credit-area__margin-label">개시증거금률</span>
                        <span className={`credit-area__margin-value ${
                            selectedLeverageOption.leverage === 1.5 ? "lev-2" :
                                selectedLeverageOption.leverage === 2   ? "lev-3" :
                                    selectedLeverageOption.leverage === 2.5 ? "lev-5" : ""
                        }`}>
                            {selectedLeverageOption.marginRate}%
                        </span>
                    </div>
                </div>

                <Tooltip text={disabledTooltipText} placement="top">
                    <button
                        className={`order-btn ${isBuy ? "order-btn--buy" : "order-btn--sell"}`}
                        onClick={openConfirm}
                        disabled={isOrderDisabled}
                    >
                        <span className="order-btn__line">{isCredit ? "신용" : "현금"}</span>
                        <span className="order-btn__line">{isBuy ? "매수" : "매도"}</span>
                    </button>
                </Tooltip>
            </div>

            <TrailingOrderConfirmModal
                open={confirmOpen}
                data={confirmData}
                onClose={() => setConfirmOpen(false)}
                onConfirm={handleOrder}
            />
        </div>
    );
}

function OtocoOrderLayout() {
    return (
        <div className="adv-order__layout">
            <div className="adv-order__body">
                <StockInfoPanel />
                <OtocoInputPanel />
            </div>
        </div>
    );
}

function OtocoInputPanel() {
    const [entryDirection, setEntryDirection] = useState<EntryDirection>("above");
    const [entryPrice, setEntryPrice] = useState<number>(0);
    const [entryQty,   setEntryQty]   = useState<number>(0);

    const [tpMode, setTpMode] = useState<ExitMode>("price");
    const [slMode, setSlMode] = useState<ExitMode>("price");
    const [tpPrice, setTpPrice] = useState<number>(0);
    const [slPrice, setSlPrice] = useState<number>(0);
    const [tpPct, setTpPct] = useState<number>(DEFAULT_TP_PCT);
    const [slPct, setSlPct] = useState<number>(DEFAULT_SL_PCT);

    const [isCredit, setIsCredit] = useState<boolean>(false);
    const [leverage, setLeverage] = useState<number>(1.5);

    const [confirmOpen, setConfirmOpen] = useState(false);
    const [confirmData, setConfirmData] = useState<OtocoConfirmData | null>(null);

    const selectedLeverageOption =
        LEVERAGE_OPTIONS.find((opt) => opt.leverage === leverage) ?? LEVERAGE_OPTIONS[0];

    const { account }       = useAccount();
    const { priceTick }     = useStockRealtime();
    const { selectedStock } = useStock();
    const { user }          = useUser();

    const currentPrice = priceTick?.curPrice ?? 0;
    const orderableAmount = account?.availableCash ?? 0;

    const tpDerivedPrice = tpMode === "price"
        ? tpPrice
        : Math.round(entryPrice * (1 + tpPct / 100));

    const slDerivedPrice = slMode === "price"
        ? slPrice
        : Math.round(entryPrice * (1 - slPct / 100));

    const initializedRef = useRef(false);
    useEffect(() => {
        if (initializedRef.current) return;
        if (currentPrice <= 0) return;
        initializedRef.current = true;
        setEntryPrice(snapToTick(currentPrice));
        setTpPrice(snapToTick(Math.round(currentPrice * (1 + DEFAULT_TP_PCT / 100))));
        setSlPrice(snapToTick(Math.round(currentPrice * (1 - DEFAULT_SL_PCT / 100))));
    }, [currentPrice]);

    const entryAmountRaw   = entryPrice * entryQty;
    const entryAmountAfter = isCredit
        ? Math.floor(entryAmountRaw / leverage)
        : entryAmountRaw;
    const showEntryBeforeAfter = isCredit;

    function handleRatioClick(ratio: number) {
        const basePrice = entryPrice > 0 ? entryPrice : currentPrice;
        if (basePrice <= 0) { setEntryQty(0); return; }
        const qty = Math.floor((orderableAmount * ratio) / basePrice);
        setEntryQty(clampQty(qty));
    }

    function openConfirm() {
        setConfirmData({
            stockName:      selectedStock.name ?? "",
            stockCode:      selectedStock.code ?? "",
            entryDirection: entryDirection.toUpperCase() as "ABOVE" | "BELOW",
            triggerPrice:   entryPrice,
            quantity:       entryQty,
            tpMode:         tpMode.toUpperCase() as "PRICE" | "PCT",
            tpPrice:        tpMode === "price" ? tpPrice : null,
            tpPct:          tpMode === "pct"   ? tpPct   : null,
            slMode:         slMode.toUpperCase() as "PRICE" | "PCT",
            slPrice:        slMode === "price" ? slPrice : null,
            slPct:          slMode === "pct"   ? slPct   : null,
            credit:         isCredit,
            leverage:       isCredit ? leverage : 1,
        });
        setConfirmOpen(true);
    }

    async function handleOrder() {
        await placeOtocoOrder({
            stockCode: selectedStock.code,
            entryDirection: entryDirection.toUpperCase() as ApiOtocoEntryDirection,
            orderQuantity: entryQty,
            entryTriggerPrice: entryPrice,
            tpMode: tpMode.toUpperCase() as ApiOtocoExitMode,
            tpPrice: tpMode === "price" ? tpPrice : null,
            tpPct: tpMode === "pct" ? tpPct : null,
            slMode: slMode.toUpperCase() as ApiOtocoExitMode,
            slPrice: slMode === "price" ? slPrice : null,
            slPct: slMode === "pct" ? slPct : null,
            leverageRatio: isCredit ? toLeverageRatio(leverage) : null,
        });
    }

    const dirClass = entryDirection === "above" ? "otoco-dir--above" : "otoco-dir--below";

    const requiredTier = isCredit ? LEVERAGE_REQUIRED_TIER[leverage] : null;
    const isLeverageAllowed = !requiredTier || isRankAtLeast(user?.rank?.tier, requiredTier);

    const leverageDisabledReason =
        isCredit && !isLeverageAllowed ? `${requiredTier} 등급 이상 가능` : null;
    const quantityDisabledReason =
        entryQty <= 0 ? "수량 입력 필요" : null;

    const disabledTooltipText = leverageDisabledReason ?? quantityDisabledReason ?? "";
    const isOrderDisabled = leverageDisabledReason !== null || quantityDisabledReason !== null;

    return (
        <div className="adv-order__order-panel otoco-panel">

            <div className="otoco-section">
                <div className="otoco-section__header">
                    <span className="otoco-section__title">진입 조건</span>
                    <span className="otoco-section__sub">(매수 진입 조건)</span>
                </div>

                <div className="otoco-direction-group">
                    <button
                        type="button"
                        className={`otoco-direction-btn ${
                            entryDirection === "above" ? "otoco-direction-btn--above otoco-direction-btn--active" : ""
                        }`}
                        onClick={() => setEntryDirection("above")}
                    >
                        이상 ↑
                    </button>
                    <button
                        type="button"
                        className={`otoco-direction-btn ${
                            entryDirection === "below" ? "otoco-direction-btn--below otoco-direction-btn--active" : ""
                        }`}
                        onClick={() => setEntryDirection("below")}
                    >
                        이하 ↓
                    </button>
                </div>

                <div className="form-row">
                    <span className={`form-label otoco-entry-label ${dirClass}`}>감시가</span>
                    <div className="stepper">
                        <button
                            className={`stepper__btn ${entryDirection === "above" ? "accent--above" : "accent--below"}`}
                            onClick={() => setEntryPrice((p) => stepPrice(p, -1))}
                        >
                            −
                        </button>
                        <input
                            className="stepper__input"
                            type="text"
                            inputMode="numeric"
                            value={formatNumber(entryPrice)}
                            onChange={(e) => setEntryPrice(snapToTick(Math.max(0, parseNumber(e.target.value))))}
                        />
                        <button
                            className={`stepper__btn ${entryDirection === "above" ? "accent--above" : "accent--below"}`}
                            onClick={() => setEntryPrice((p) => stepPrice(p, 1))}
                        >
                            +
                        </button>
                        <span className="stepper__unit">원</span>
                    </div>
                </div>

                <div className="ratio-group">
                    {RATIO_OPTIONS.map(({ label, ratio }) => (
                        <button
                            key={label}
                            type="button"
                            className={`ratio-btn ${entryDirection === "above" ? "accent--above" : "accent--below"}`}
                            onClick={() => handleRatioClick(ratio)}
                        >
                            {label}
                        </button>
                    ))}
                </div>

                <div className="form-row">
                    <span className="form-label">수량</span>
                    <div className="stepper">
                        <button
                            className={`stepper__btn ${entryDirection === "above" ? "accent--above" : "accent--below"}`}
                            onClick={() => setEntryQty((q) => clampQty(q - 1))}
                        >
                            −
                        </button>
                        <input
                            className="stepper__input"
                            type="text"
                            inputMode="numeric"
                            value={formatNumber(entryQty)}
                            onChange={(e) => setEntryQty(clampQty(parseNumber(e.target.value)))}
                        />
                        <button
                            className={`stepper__btn ${entryDirection === "above" ? "accent--above" : "accent--below"}`}
                            onClick={() => setEntryQty((q) => clampQty(q + 1))}
                        >
                            +
                        </button>
                        <span className="stepper__unit">주</span>
                    </div>
                </div>
            </div>

            <div className="divider" />

            <div className="otoco-section">
                <div className="otoco-section__header">
                    <span className="otoco-section__title">청산 조건</span>
                    <span className="otoco-section__sub">(익절 · 손절 조건)</span>
                </div>

                <div className="otoco-exit-row">
                    <span className="otoco-exit-label otoco-exit-label--tp">익절</span>
                    <div className="otoco-mode-toggle">
                        <button
                            type="button"
                            className={`otoco-mode-btn ${tpMode === "price" ? "otoco-mode-btn--active" : ""}`}
                            onClick={() => setTpMode("price")}
                        >
                            원
                        </button>
                        <button
                            type="button"
                            className={`otoco-mode-btn ${tpMode === "pct" ? "otoco-mode-btn--active" : ""}`}
                            onClick={() => setTpMode("pct")}
                        >
                            %
                        </button>
                    </div>
                    {tpMode === "price" ? (
                        <input
                            className="otoco-price-input"
                            type="text"
                            inputMode="numeric"
                            placeholder="0"
                            value={tpPrice > 0 ? formatNumber(tpPrice) : ""}
                            onChange={(e) => setTpPrice(snapToTick(Math.max(0, parseNumber(e.target.value))))}
                        />
                    ) : (
                        <input
                            className="otoco-price-input"
                            type="number"
                            min={OTOCO_MIN_PCT}
                            max={OTOCO_MAX_PCT}
                            step={OTOCO_STEP_PCT}
                            placeholder={`+${DEFAULT_TP_PCT}`}
                            value={tpPct}
                            onChange={(e) => setTpPct(clampOtocoPct(Number(e.target.value)))}
                        />
                    )}
                    <span className="otoco-price-unit">
                        {tpMode === "price" ? "원" : "%"}
                    </span>
                    <span className="otoco-derived-preview otoco-derived-preview--tp">
                        ≈ {entryPrice > 0 ? `${tpDerivedPrice.toLocaleString()}원` : "—"}
                    </span>
                </div>

                <div className="otoco-exit-row">
                    <span className="otoco-exit-label otoco-exit-label--sl">손절</span>
                    <div className="otoco-mode-toggle">
                        <button
                            type="button"
                            className={`otoco-mode-btn ${slMode === "price" ? "otoco-mode-btn--active" : ""}`}
                            onClick={() => setSlMode("price")}
                        >
                            원
                        </button>
                        <button
                            type="button"
                            className={`otoco-mode-btn ${slMode === "pct" ? "otoco-mode-btn--active" : ""}`}
                            onClick={() => setSlMode("pct")}
                        >
                            %
                        </button>
                    </div>
                    {slMode === "price" ? (
                        <input
                            className="otoco-price-input"
                            type="text"
                            inputMode="numeric"
                            placeholder="0"
                            value={slPrice > 0 ? formatNumber(slPrice) : ""}
                            onChange={(e) => setSlPrice(snapToTick(Math.max(0, parseNumber(e.target.value))))}
                        />
                    ) : (
                        <input
                            className="otoco-price-input"
                            type="number"
                            min={OTOCO_MIN_PCT}
                            max={OTOCO_MAX_PCT}
                            step={OTOCO_STEP_PCT}
                            placeholder={`-${DEFAULT_SL_PCT}`}
                            value={slPct}
                            onChange={(e) => setSlPct(clampOtocoPct(Number(e.target.value)))}
                        />
                    )}
                    <span className="otoco-price-unit">
                        {slMode === "price" ? "원" : "%"}
                    </span>
                    <span className="otoco-derived-preview otoco-derived-preview--sl">
                        ≈ {entryPrice > 0 ? `${slDerivedPrice.toLocaleString()}원` : "—"}
                    </span>
                </div>
            </div>

            <div className="adv-order__summary">
                <div className="adv-order__summary-row">
                    <span className="adv-order__summary-label">주문가능금액</span>
                    <span className="adv-order__summary-value">
                        {orderableAmount.toLocaleString()} 원
                    </span>
                </div>

                <div className="adv-order__summary-row">
                    <span className="adv-order__summary-label">
                        예상 진입금액
                        <LeverageTag leverage={isCredit ? leverage : 1} />
                    </span>
                    <span className="adv-order__summary-value adv-order__summary-value--fill adv-order__summary-value--fill-buy">
                        {showEntryBeforeAfter ? (
                            <>
                                <span className="adv-order__summary-value--after">
                                    {entryAmountAfter.toLocaleString()} 원
                                </span>
                                <span className="adv-order__summary-value--before">
                                    {entryAmountRaw.toLocaleString()} 원
                                </span>
                            </>
                        ) : (
                            `${entryAmountAfter.toLocaleString()} 원`
                        )}
                    </span>
                </div>
            </div>

            <div className="order-btn-row">
                <div className={`credit-area ${isCredit ? "credit-area--active" : ""}`}>
                    <div className="credit-area__row1">
                        <Tooltip text={isCredit ? "신용 해제" : "신용 적용"} placement="top">
                            <label className={`credit-check ${isCredit ? "credit-check--active" : ""}`}>
                                <input
                                    type="checkbox"
                                    checked={isCredit}
                                    onChange={(e) => setIsCredit(e.target.checked)}
                                />
                                신용
                            </label>
                        </Tooltip>
                        <div className="leverage-group">
                            {LEVERAGE_OPTIONS.map(({ leverage: lv, label }) => {
                                const levClass =
                                    lv === 1.5 ? "lev-2" :
                                        lv === 2   ? "lev-3" :
                                            lv === 2.5 ? "lev-5" : "";
                                return (
                                    <button
                                        key={lv}
                                        type="button"
                                        disabled={!isCredit}
                                        className={`leverage-btn ${levClass} ${
                                            isCredit && leverage === lv ? "leverage-btn--active" : ""
                                        }`}
                                        onClick={() => setLeverage(lv)}
                                    >
                                        {label}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                    <div className={`credit-area__row2 ${!isCredit ? "credit-area__row2--disabled" : ""}`}>
                        <span className="credit-area__margin-label">개시증거금률</span>
                        <span className={`credit-area__margin-value ${
                            selectedLeverageOption.leverage === 1.5 ? "lev-2" :
                                selectedLeverageOption.leverage === 2   ? "lev-3" :
                                    selectedLeverageOption.leverage === 2.5 ? "lev-5" : ""
                        }`}>
        {selectedLeverageOption.marginRate}%
    </span>
                    </div>
                </div>

                <Tooltip text={disabledTooltipText} placement="top">
                    <button
                        className="order-btn order-btn--buy"
                        onClick={openConfirm}
                        disabled={isOrderDisabled}
                    >
                        <span className="order-btn__line">{isCredit ? "신용" : "현금"}</span>
                        <span className="order-btn__line">매수</span>
                    </button>
                </Tooltip>
            </div>

            <OtocoOrderConfirmModal
                open={confirmOpen}
                data={confirmData}
                onClose={() => setConfirmOpen(false)}
                onConfirm={handleOrder}
            />
        </div>
    );
}

type OtocoScenario = {
    direction: "above" | "below";
    exit: "tp" | "sl";
};

function makeOtocoPricePoints(sc: OtocoScenario): {
    pts: PricePoint[];
    triggerIdx: number;
    tpPrice: number;
    slPrice: number;
} {
    const isAbove = sc.direction === "above";
    const isTp    = sc.exit === "tp";

    if (isAbove) {
        const TRIGGER = 110;
        const TP      = 120;
        const SL      = 103;
        const pts: PricePoint[] = [
            { t: 0,  p: 100 },
            { t: 1,  p: 102 },
            { t: 2,  p: 99  },
            { t: 3,  p: 101 },
            { t: 4,  p: 103 },
            { t: 5,  p: 100 },
            { t: 6,  p: 104 },
            { t: 7,  p: 101 },
            { t: 8,  p: TRIGGER },
            ...(isTp
                ? [
                    { t: 9,  p: 112 },
                    { t: 10, p: 111 },
                    { t: 11, p: 108 },
                    { t: 12, p: 107 },
                    { t: 13, p: 115 },
                    { t: 14, p: 118 },
                    { t: 15, p: TP  },
                ]
                : [
                    { t: 9,  p: 115 },
                    { t: 10, p: 117 },
                    { t: 11, p: 111 },
                    { t: 12, p: 108 },
                    { t: 13, p: 106 },
                    { t: 14, p: 104 },
                    { t: 15, p: SL  },
                ]),
        ];
        return { pts, triggerIdx: 8, tpPrice: TP, slPrice: SL };

    } else {
        const TRIGGER = 90;
        const TP      = 100;
        const SL      = 83;
        const pts: PricePoint[] = [
            { t: 0,  p: 100 },
            { t: 1,  p: 98  },
            { t: 2,  p: 101 },
            { t: 3,  p: 99  },
            { t: 4,  p: 97  },
            { t: 5,  p: 100 },
            { t: 6,  p: 96  },
            { t: 7,  p: 99  },
            { t: 8,  p: TRIGGER },
            ...(isTp
                ? [
                    { t: 9,  p: 88  },
                    { t: 10, p: 93  },
                    { t: 11, p: 91  },
                    { t: 12, p: 92  },
                    { t: 13, p: 95  },
                    { t: 14, p: 98  },
                    { t: 15, p: TP  },
                ]
                : [
                    { t: 9,  p: 88  },
                    { t: 10, p: 89  },
                    { t: 11, p: 87  },
                    { t: 12, p: 88  },
                    { t: 13, p: 85  },
                    { t: 14, p: 84  },
                    { t: 15, p: SL  },
                ]),
        ];
        return { pts, triggerIdx: 8, tpPrice: TP, slPrice: SL };
    }
}

function drawOtocoChart(
    canvas: HTMLCanvasElement,
    sc: OtocoScenario,
    progress: number
) {
    const dpr  = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = rect.width;
    const H = rect.height;
    if (W === 0 || H === 0) return;

    canvas.width  = W * dpr;
    canvas.height = H * dpr;
    const ctx = canvas.getContext("2d")!;
    ctx.scale(dpr, dpr);

    const { pts, triggerIdx, tpPrice, slPrice } = makeOtocoPricePoints(sc);

    const COLOR_PRICE   = "#e8eaed";
    const COLOR_TRIGGER = sc.direction === "above" ? "#f59e0b" : "#38bdf8";
    const COLOR_TP      = "#22c07a";
    const COLOR_SL      = "#f04a4a";
    const COLOR_GRID    = "rgba(255,255,255,0.05)";
    const COLOR_AXIS    = "#2e3138";
    const COLOR_TEXT    = "#e8eaed";

    const PAD = { top: 22, right: 16, bottom: 28, left: 42 };
    const cW = W - PAD.left - PAD.right;
    const cH = H - PAD.top  - PAD.bottom;

    const totalSteps = pts.length - 1;

    const allP = [...pts.map(p => p.p), tpPrice, slPrice];
    const minP = Math.min(...allP) - 5;
    const maxP = Math.max(...allP) + 5;

    const xOf = (t: number) => PAD.left + (t / totalSteps) * cW;
    const yOf = (p: number) => PAD.top  + (1 - (p - minP) / (maxP - minP)) * cH;

    const priceCoords = pts.map(p => ({ x: xOf(p.t), y: yOf(p.p) }));

    ctx.clearRect(0, 0, W, H);

    for (let i = 0; i <= 4; i++) {
        const y = PAD.top + (i / 4) * cH;
        ctx.beginPath();
        ctx.moveTo(PAD.left, y);
        ctx.lineTo(W - PAD.right, y);
        ctx.strokeStyle = COLOR_GRID;
        ctx.lineWidth = 0.5;
        ctx.stroke();
        const val = Math.round(maxP - (i / 4) * (maxP - minP));
        ctx.fillStyle = COLOR_TEXT;
        ctx.font = "10px system-ui, sans-serif";
        ctx.textAlign = "right";
        ctx.fillText(String(val), PAD.left - 5, y + 4);
    }

    ctx.beginPath();
    ctx.moveTo(PAD.left, PAD.top);
    ctx.lineTo(PAD.left, PAD.top + cH);
    ctx.lineTo(W - PAD.right, PAD.top + cH);
    ctx.strokeStyle = COLOR_AXIS;
    ctx.lineWidth = 1;
    ctx.stroke();

    for (let i = 0; i <= totalSteps; i += 2) {
        ctx.fillStyle = COLOR_TEXT;
        ctx.font = "9px system-ui, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(`T${i}`, xOf(i), H - PAD.bottom + 13);
    }

    const triggerProgress = triggerIdx / totalSteps;
    const priceProgress   = Math.min(1, progress);
    const exitProgress    = progress <= triggerProgress
        ? 0
        : Math.min(1, (progress - triggerProgress) / (1 - triggerProgress));

    const { visCoords: visPriceCoords, tipX: pTipX, tipY: pTipY } =
        interpAtProgress(priceCoords, priceProgress);

    const entryX = xOf(triggerIdx);
    const entryY = yOf(pts[triggerIdx].p);
    const exitEndX = entryX + (W - PAD.right - entryX) * exitProgress;
    const tpY      = yOf(tpPrice);
    const slY      = yOf(slPrice);

    if (exitProgress > 0) {
        ctx.beginPath();
        ctx.rect(entryX, Math.min(tpY, slY), exitEndX - entryX, Math.abs(tpY - slY));
        ctx.fillStyle = "rgba(34,192,122,0.05)";
        ctx.fill();
    }

    if (exitProgress > 0) {
        ctx.beginPath();
        ctx.moveTo(entryX, tpY);
        ctx.lineTo(exitEndX, tpY);
        ctx.strokeStyle = COLOR_TP;
        ctx.lineWidth = 1.5;
        ctx.setLineDash([6, 4]);
        ctx.globalAlpha = 0.85;
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.globalAlpha = 1;

        const tpFade = Math.min(1, exitProgress / 0.15);
        ctx.globalAlpha = tpFade;
        ctx.fillStyle = COLOR_TP;
        ctx.font = "600 10px system-ui, sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(`익절 ${tpPrice}`, entryX + 4, tpY - 5);
        ctx.globalAlpha = 1;
    }

    if (exitProgress > 0) {
        ctx.beginPath();
        ctx.moveTo(entryX, slY);
        ctx.lineTo(exitEndX, slY);
        ctx.strokeStyle = COLOR_SL;
        ctx.lineWidth = 1.5;
        ctx.setLineDash([6, 4]);
        ctx.globalAlpha = 0.85;
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.globalAlpha = 1;

        const slFade = Math.min(1, exitProgress / 0.15);
        ctx.globalAlpha = slFade;
        ctx.fillStyle = COLOR_SL;
        ctx.font = "600 10px system-ui, sans-serif";
        ctx.textAlign = "left";
        ctx.fillText(`손절 ${slPrice}`, entryX + 4, slY + 13);
        ctx.globalAlpha = 1;
    }

    if (visPriceCoords.length >= 2) {
        ctx.beginPath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = COLOR_PRICE;
        drawLinearPath(ctx, visPriceCoords, pTipX, pTipY);
        ctx.stroke();
    }

    const trigFadeIn = progress <= triggerProgress
        ? 0
        : Math.min(1, (progress - triggerProgress) / 0.08);

    ctx.globalAlpha = 0.35;
    ctx.beginPath();
    ctx.setLineDash([4, 5]);
    ctx.lineWidth = 1;
    ctx.strokeStyle = COLOR_TRIGGER;
    ctx.moveTo(PAD.left, entryY);
    ctx.lineTo(W - PAD.right, entryY);
    ctx.stroke();
    ctx.setLineDash([]);
    ctx.globalAlpha = 1;

    if (trigFadeIn > 0) {
        ctx.globalAlpha = trigFadeIn * 0.3;
        ctx.beginPath();
        ctx.setLineDash([3, 3]);
        ctx.lineWidth = 0.5;
        ctx.strokeStyle = COLOR_TRIGGER;
        ctx.moveTo(entryX, PAD.top);
        ctx.lineTo(entryX, entryY - 12);
        ctx.stroke();
        ctx.setLineDash([]);

        ctx.globalAlpha = trigFadeIn * 0.18;
        ctx.beginPath();
        ctx.arc(entryX, entryY, 13, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_TRIGGER;
        ctx.fill();

        ctx.globalAlpha = trigFadeIn;
        ctx.beginPath();
        ctx.arc(entryX, entryY, 8, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_PRICE;
        ctx.fill();

        ctx.beginPath();
        ctx.arc(entryX, entryY, 5.5, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_TRIGGER;
        ctx.fill();

        ctx.beginPath();
        ctx.lineWidth = 1.3;
        ctx.strokeStyle = COLOR_PRICE;
        ctx.moveTo(entryX - 4, entryY);
        ctx.lineTo(entryX + 4, entryY);
        ctx.moveTo(entryX, entryY - 4);
        ctx.lineTo(entryX, entryY + 4);
        ctx.stroke();

        ctx.fillStyle = COLOR_TRIGGER;
        ctx.font = "600 10px system-ui, sans-serif";
        ctx.textAlign = "right";
        ctx.fillText("진입가", entryX - 11, entryY - 5);
        ctx.fillStyle = COLOR_PRICE;
        ctx.font = "700 13px system-ui, sans-serif";
        ctx.fillText(`${pts[triggerIdx].p}`, entryX - 11, entryY + 9);

        ctx.globalAlpha = 1;
    }

    const exitFade = Math.min(1, Math.max(0, (exitProgress - 0.85) / 0.15));
    if (exitFade > 0 && exitProgress > 0) {
        const isTp_     = sc.exit === "tp";
        const exitColor = isTp_ ? COLOR_TP : COLOR_SL;
        const exitY     = isTp_ ? tpY : slY;
        const exitPrice = isTp_ ? tpPrice : slPrice;
        const exitLabel = isTp_ ? "익절 체결" : "손절 체결";
        const ex        = exitEndX;

        ctx.globalAlpha = exitFade * 0.2;
        ctx.beginPath();
        ctx.arc(ex, exitY, 11, 0, Math.PI * 2);
        ctx.fillStyle = exitColor;
        ctx.fill();

        ctx.globalAlpha = exitFade;
        ctx.beginPath();
        ctx.arc(ex, exitY, 7, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_PRICE;
        ctx.fill();

        ctx.beginPath();
        ctx.arc(ex, exitY, 4.5, 0, Math.PI * 2);
        ctx.fillStyle = exitColor;
        ctx.fill();

        const isRight = ex > W * 0.75;
        ctx.fillStyle = exitColor;
        ctx.font = "600 10px system-ui, sans-serif";
        ctx.textAlign = isRight ? "right" : "left";
        ctx.fillText(exitLabel, isRight ? ex - 10 : ex + 10, exitY - 8);
        ctx.fillStyle = COLOR_PRICE;
        ctx.font = "700 13px system-ui, sans-serif";
        ctx.fillText(`${exitPrice}`, isRight ? ex - 10 : ex + 10, exitY + 6);

        ctx.globalAlpha = 1;
    }

    ctx.beginPath();
    ctx.arc(pTipX, pTipY, 3, 0, Math.PI * 2);
    ctx.fillStyle = COLOR_PRICE;
    ctx.fill();
}

interface OtocoChartProps {
    sc: OtocoScenario;
    replayKey: number;
}

function OtocoChart({ sc, replayKey }: OtocoChartProps) {
    const canvasRef      = useRef<HTMLCanvasElement>(null);
    const rafRef         = useRef<number>(0);
    const isAnimatingRef = useRef<boolean>(false);

    const { direction, exit } = sc;

    const runAnimation = useCallback(() => {
        if (rafRef.current) cancelAnimationFrame(rafRef.current);

        const DURATION = 3200;
        const EXTRA    = DURATION * 0.15;
        const start    = performance.now();
        const scSnapshot: OtocoScenario = { direction, exit };

        isAnimatingRef.current = true;

        const frame = (now: number) => {
            const elapsed  = now - start;
            const progress = Math.max(0, elapsed / DURATION);
            if (canvasRef.current) drawOtocoChart(canvasRef.current, scSnapshot, progress);
            if (elapsed < DURATION + EXTRA) {
                rafRef.current = requestAnimationFrame(frame);
            } else {
                if (canvasRef.current) drawOtocoChart(canvasRef.current, scSnapshot, 1.15);
                isAnimatingRef.current = false;
            }
        };
        rafRef.current = requestAnimationFrame(frame);
    }, [direction, exit]);

    useEffect(() => {
        runAnimation();
        return () => {
            if (rafRef.current) cancelAnimationFrame(rafRef.current);
            isAnimatingRef.current = false;
        };
    }, [runAnimation, replayKey]);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ro = new ResizeObserver(() => {
            if (isAnimatingRef.current) return;
            if (canvasRef.current) drawOtocoChart(canvasRef.current, sc, 1.15);
        });
        ro.observe(canvas);
        return () => ro.disconnect();
    }, [direction, exit]);

    const dirLabel  = sc.direction === "above" ? "이상" : "이하";
    const exitLabel = sc.exit === "tp" ? "익절" : "손절";

    return (
        <canvas
            ref={canvasRef}
            className="ao-trailing-canvas"
            aria-label={`OTOCO ${dirLabel} 진입 → ${exitLabel} 체결 차트`}
        />
    );
}

function OtocoInfo() {
    const [direction, setDirection] = useState<EntryDirection>("above");
    const [exit, setExit] = useState<"tp" | "sl">("tp");
    const [replayKey, setReplayKey] = useState(0);

    const isAbove = direction === "above";
    const sc: OtocoScenario = { direction, exit };

    const handleDirection = (d: EntryDirection) => {
        setDirection(d);
        setReplayKey(k => k + 1);
    };

    return (
        <div className="ao-info">
            <div className="ao-info__mode-tabs">
                <button
                    className={`ao-info__mode-tab otoco-info-mode-tab--above ${isAbove ? "ao-info__mode-tab--active" : ""}`}
                    onClick={() => handleDirection("above")}
                >
                    이상(↑) 진입 OTOCO
                </button>
                <button
                    className={`ao-info__mode-tab otoco-info-mode-tab--below ${!isAbove ? "ao-info__mode-tab--active" : ""}`}
                    onClick={() => handleDirection("below")}
                >
                    이하(↓) 진입 OTOCO
                </button>
            </div>

            <div className="ao-diagram">
                <div className="ao-diagram__header">
                    <div className="ao-diagram__legend">
                        <div className="ao-diagram__legend-item">
                            <span className="ao-diagram__legend-line ao-diagram__legend-line--price" />
                            <span>현재 가격</span>
                        </div>
                        <div className="ao-diagram__legend-item">
                            <span
                                className="ao-diagram__legend-line"
                                style={{ background: isAbove ? "#f59e0b" : "#38bdf8" }}
                            />
                            <span>진입가</span>
                        </div>
                        <div className="ao-diagram__legend-item">
                            <span className="ao-diagram__legend-line otoco-legend-line--tp" />
                            <span>익절</span>
                        </div>
                        <div className="ao-diagram__legend-item">
                            <span className="ao-diagram__legend-line otoco-legend-line--sl" />
                            <span>손절</span>
                        </div>
                    </div>

                    <ScenarioSelect<"tp" | "sl">
                        ariaLabel="체결 시나리오 선택"
                        value={exit}
                        onChange={(v) => { setExit(v); setReplayKey(k => k + 1); }}
                        options={[
                            { value: "tp", label: "시나리오 A · 익절 체결" },
                            { value: "sl", label: "시나리오 B · 손절 체결" },
                        ]}
                    />
                </div>

                <OtocoChart sc={sc} replayKey={replayKey} />
            </div>

            <ul className="ao-info__points">
                {isAbove ? (
                    <>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--above" />
                            현재가가 감시가 이상으로 오르면 자동으로 매수 진입하고, 진입과 동시에 익절·손절 OCO 주문이 함께 등록됩니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--above" />
                            저항선 돌파 직후 추세를 따라 진입하면서도 손실 범위를 사전에 정의할 수 있습니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--above" />
                            익절·손절가는 원 단위 또는 감시가 대비 % 단위로 설정할 수 있고, 진입 직후 자동으로 등록됩니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--above" />
                            익절·손절 중 하나가 체결되면 나머지는 자동 취소되어 중복 청산을 방지합니다.
                        </li>
                    </>
                ) : (
                    <>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--below" />
                            현재가가 감시가 이하로 내리면 자동으로 매수 진입하고, 진입과 동시에 익절·손절 OCO 주문이 함께 등록됩니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--below" />
                            지지선 이탈을 역발상 매수 시점으로 활용하거나, 과매도 구간의 빠른 반등을 노릴 때 유용합니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--below" />
                            익절·손절가는 원 단위 또는 감시가 대비 % 단위로 설정할 수 있고, 진입 직후 자동으로 등록됩니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot otoco-point-dot--below" />
                            익절·손절 중 하나가 체결되면 나머지는 자동 취소되어 중복 청산을 방지합니다.
                        </li>
                    </>
                )}
            </ul>
        </div>
    );
}

function OtocoPendingList() {
    const { otocoOrders, connected } = useAdvancedOrders();

    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [canceling, setCanceling]     = useState<boolean>(false);

    const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);

    function toggleSelect(id: string) {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    }

    function toggleAll() {
        if (selectedIds.size === otocoOrders.length) {
            setSelectedIds(new Set());
        } else {
            setSelectedIds(new Set(otocoOrders.map((o) => o.orderId)));
        }
    }

    function openCancelConfirm() {
        if (selectedIds.size === 0 || canceling) return;
        setCancelConfirmOpen(true);
    }

    async function doCancel() {
        setCanceling(true);
        const targets = otocoOrders.filter((o) => selectedIds.has(o.orderId));
        try {
            await Promise.all(
                targets.map((o) =>
                    cancelOtocoOrder({
                        otocoId: Number(o.orderId),
                        stockCode: o.stockCode,
                    }).catch((e) => console.error("OTOCO 취소 실패:", e, o.orderId))
                )
            );
        } finally {
            setCanceling(false);
            setSelectedIds(new Set());
        }
    }

    const hasSelection = selectedIds.size > 0;

    if (!connected) {
        return (
            <div className="ao-pending__empty">
                <span className="ao-pending__empty-dot" />
                <p>연결 중…</p>
            </div>
        );
    }

    if (otocoOrders.length === 0) {
        return (
            <div className="ao-pending__empty">
                <span className="ao-pending__empty-icon">⌛</span>
                <p>미체결 OTOCO 주문이 없습니다.</p>
            </div>
        );
    }

    return (
        <div className="ao-pending">
            <div className="ao-pending__header">
                <span className="ao-pending__count">미체결 {otocoOrders.length}건</span>
                <Tooltip
                    text={selectedIds.size === otocoOrders.length && otocoOrders.length > 0 ? "전체 취소 해제" : "전체 취소"}
                    placement="top"
                >
                    <label className="ao-pending__all-check">
                        <input
                            type="checkbox"
                            checked={selectedIds.size === otocoOrders.length && otocoOrders.length > 0}
                            onChange={toggleAll}
                        />
                        전체선택
                    </label>
                </Tooltip>
            </div>

            <div className="ao-pending__list">
                {[...otocoOrders]
                    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                    .map((order) => (
                        <OtocoOrderCard
                            key={order.orderId}
                            order={order}
                            selected={selectedIds.has(order.orderId)}
                            onToggle={() => toggleSelect(order.orderId)}
                        />
                    ))}
            </div>

            <div className="ao-pending__footer">
                <span
                    className={`ao-pending__selected-count ${
                        hasSelection ? "ao-pending__selected-count--active" : ""
                    }`}
                >
                    {hasSelection ? `${selectedIds.size}건 선택됨` : "항목을 선택하세요"}
                </span>
                <button
                    className={`ao-pending__cancel-btn ${
                        !hasSelection ? "ao-pending__cancel-btn--disabled" : ""
                    }`}
                    onClick={openCancelConfirm}
                    disabled={!hasSelection || canceling}
                >
                    {canceling ? "취소 중…" : "주문 취소"}
                </button>
            </div>

            <SimpleCancelConfirmModal
                open={cancelConfirmOpen}
                count={selectedIds.size}
                onClose={() => setCancelConfirmOpen(false)}
                onConfirm={doCancel}
            />
        </div>
    );
}

interface OtocoOrderCardProps {
    order: OtocoPendingOrder;
    selected: boolean;
    onToggle: () => void;
}

function OtocoOrderCard({ order, selected, onToggle }: OtocoOrderCardProps) {
    const isAbove = order.entryDirection === "ABOVE";

    const tpMain = order.tpTriggerPrice != null
        ? `${order.tpTriggerPrice.toLocaleString()}원`
        : "—";

    const slMain = order.slTriggerPrice != null
        ? `${order.slTriggerPrice.toLocaleString()}원`
        : "—";

    return (
        <div
            className={`ao-pending-card ${selected ? "ao-pending-card--selected" : ""}`}
            onClick={onToggle}
        >
            <div className="ao-pending-card__head">
                <Tooltip
                    text={selected ? "취소 해제" : "취소"}
                    placement="top"
                >
                    <input
                        type="checkbox"
                        className="ao-pending-card__checkbox"
                        checked={selected}
                        onChange={onToggle}
                        onClick={(e) => e.stopPropagation()}
                    />
                </Tooltip>
                <div className="ao-pending-card__stock">
                    <span className="ao-pending-card__name">{order.stockName}</span>
                    <span className="ao-pending-card__code">{order.stockCode}</span>
                </div>
                <span className="ao-pending-card__time">
        {formatTimestamp(order.createdAt)}
    </span>
            </div>

            <div className="ao-pending-card__body">
                <div className="ao-pending-card__row">
                    <span className={`ao-pending-badge ${isAbove ? "ao-pending-badge--above" : "ao-pending-badge--below"}`}>
                        {isAbove ? "이상↑" : "이하↓"}
                    </span>
                    <LeverageBadge leverage={order.leverage} />
                </div>

                <div className="ao-pending-card__row">
                    <div className="ao-pending-card__field">
                        <span className="ao-pending-card__field-label">진입가</span>
                        <span className={`ao-pending-card__field-value ${isAbove ? "ao-pending-card__field-value--trail-buy" : "ao-pending-card__field-value--trail-sell"}`}>
                            {order.triggerPrice.toLocaleString()}원
                        </span>
                        <span className={`ao-pending-card__field-value ${isAbove ? "ao-pending-card__field-value--trail-buy" : "ao-pending-card__field-value--trail-sell"}`}>
                            {isAbove ? "이상" : "이하"}
                        </span>
                    </div>
                    <div className="ao-pending-card__field">
                        <span className="ao-pending-card__field-label">수량</span>
                        <span className="ao-pending-card__field-value">
                            {order.quantity.toLocaleString()}주
                        </span>
                    </div>
                    <span className={`ao-pending-badge ${order.entryFilled ? "ao-pending-badge--filled" : "ao-pending-badge--waiting"}`}>
                        {order.entryFilled ? "진입완료" : "진입예정"}
                    </span>
                </div>

                <div className="ao-pending-card__divider" />

                <div className="ao-pending-card__row" style={{ alignItems: "flex-start" }}>
                    <div className="ao-pending-card__exit-field">
                        <span className="ao-pending-card__field-label">익절</span>
                        <span className="ao-pending-card__field-value ao-pending-card__field-value--tp">
                            {tpMain}
                        </span>
                    </div>
                    <div className="ao-pending-card__exit-field">
                        <span className="ao-pending-card__field-label">손절</span>
                        <span className="ao-pending-card__field-value ao-pending-card__field-value--sl">
                            {slMain}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
}

interface PricePoint { t: number; p: number; }
interface StopPoint  { t: number; s: number; }

type Scenario = "trend" | "spike";

function makePricePoints(type: "sell" | "buy", scenario: Scenario): PricePoint[] {
    if (scenario === "spike") return makeSpikePricePoints(type);
    return makeTrendPricePoints(type);
}

function makeTrendPricePoints(type: "sell" | "buy"): PricePoint[] {
    if (type === "sell") {
        return [
            { t: 0, p: 100 },
            { t: 1, p: 108 },
            { t: 2, p: 104 },
            { t: 3, p: 106 },
            { t: 4, p: 103 },
            { t: 5, p: 105 },
            { t: 6, p: 118 },
            { t: 7, p: 114 },
            { t: 8, p: 116 },
            { t: 9, p: 113 },
            { t: 10, p: 115 },
            { t: 11, p: 130 },
            { t: 12, p: 126 },
            { t: 13, p: 128 },
            { t: 14, p: 124 },
            { t: 15, p: 127 },
            { t: 16, p: 142 },
            { t: 17, p: 138 },
            { t: 18, p: 140 },
            { t: 19, p: 135 },
            { t: 20, p: 139 },
            { t: 21, p: 156 },
            { t: 22, p: 150 },
            { t: 23, p: 152 },
            { t: 24, p: 147 },
            { t: 25, p: 151 },
            { t: 26, p: 143.52 },
        ];
    }
    return [
        { t: 0, p: 140 },
        { t: 1, p: 132 },
        { t: 2, p: 136 },
        { t: 3, p: 134 },
        { t: 4, p: 137 },
        { t: 5, p: 135 },
        { t: 6, p: 122 },
        { t: 7, p: 126 },
        { t: 8, p: 124 },
        { t: 9, p: 127 },
        { t: 10, p: 125 },
        { t: 11, p: 110 },
        { t: 12, p: 114 },
        { t: 13, p: 112 },
        { t: 14, p: 113 },
        { t: 15, p: 111 },
        { t: 16, p: 96 },
        { t: 17, p: 100 },
        { t: 18, p: 98 },
        { t: 19, p: 101 },
        { t: 20, p: 99 },
        { t: 21, p: 80 },
        { t: 22, p: 84 },
        { t: 23, p: 82 },
        { t: 24, p: 85 },
        { t: 25, p: 83 },
        { t: 26, p: 86.4 },
    ];
}

function makeSpikePricePoints(type: "sell" | "buy"): PricePoint[] {
    if (type === "sell") {
        return [
            { t: 0, p: 100 },
            { t: 1, p: 101 },
            { t: 2, p: 99 },
            { t: 3, p: 102 },
            { t: 4, p: 101 },
            { t: 5, p: 134 },
            { t: 6, p: 123.28 },
            { t: 7, p: 109 },
        ];
    }
    return [
        { t: 0, p: 140 },
        { t: 1, p: 139 },
        { t: 2, p: 141 },
        { t: 3, p: 138 },
        { t: 4, p: 139 },
        { t: 5, p: 106 },
        { t: 6, p: 114.48 },
        { t: 7, p: 128 },
    ];
}

function calcStopPoints(pts: PricePoint[], type: "sell" | "buy"): StopPoint[] {
    let extreme = pts[0].p;
    return pts.map((p) => {
        if (type === "sell") {
            if (p.p > extreme) extreme = p.p;
            return { t: p.t, s: extreme * (1 - GAP_PCT) };
        } else {
            if (p.p < extreme) extreme = p.p;
            return { t: p.t, s: extreme * (1 + GAP_PCT) };
        }
    });
}

function findTriggerIndex(
    pts: PricePoint[],
    stops: StopPoint[],
    type: "sell" | "buy"
): number | null {
    for (let i = 1; i < pts.length; i++) {
        if (type === "sell" && pts[i].p <= stops[i].s) return i;
        if (type === "buy"  && pts[i].p >= stops[i].s) return i;
    }
    return null;
}

function interpAtProgress(
    coords: { x: number; y: number }[],
    progress: number
): { visCoords: { x: number; y: number }[]; tipX: number; tipY: number } {
    const total = coords.length - 1;
    const clampedProgress = Math.max(0, Math.min(1, progress));
    const raw   = clampedProgress * total;
    const seg   = Math.max(0, Math.min(total, Math.floor(raw)));
    const frac  = raw - seg;

    const visCoords = coords.slice(0, seg + 1);

    let tipX: number, tipY: number;
    if (seg >= total) {
        tipX = coords[total].x;
        tipY = coords[total].y;
    } else {
        tipX = coords[seg].x + (coords[seg + 1].x - coords[seg].x) * frac;
        tipY = coords[seg].y + (coords[seg + 1].y - coords[seg].y) * frac;
    }

    return { visCoords, tipX, tipY };
}

function drawLinearPath(
    ctx: CanvasRenderingContext2D,
    coords: { x: number; y: number }[],
    tipX: number,
    tipY: number
) {
    if (coords.length < 1) return;
    ctx.moveTo(coords[0].x, coords[0].y);
    for (let i = 1; i < coords.length; i++) {
        ctx.lineTo(coords[i].x, coords[i].y);
    }
    ctx.lineTo(tipX, tipY);
}

function drawTrailingChart(
    canvas: HTMLCanvasElement,
    type: "sell" | "buy",
    scenario: Scenario,
    progress: number
) {
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const W = rect.width;
    const H = rect.height;
    if (W === 0 || H === 0) return;

    canvas.width  = W * dpr;
    canvas.height = H * dpr;
    const ctx = canvas.getContext("2d")!;
    ctx.scale(dpr, dpr);

    const COLOR_PRICE   = "#e8eaed";
    const COLOR_SELL    = "#4d8ef5";
    const COLOR_BUY     = "#f04a4a";
    const COLOR_GRID    = "rgba(255,255,255,0.05)";
    const COLOR_AXIS    = "#2e3138";
    const COLOR_TEXT = "#e8eaed";
    const COLOR_STOP    = type === "sell" ? COLOR_SELL : COLOR_BUY;
    const COLOR_TRIGGER = type === "sell" ? COLOR_SELL : COLOR_BUY;

    const pts   = makePricePoints(type, scenario);
    const stops = calcStopPoints(pts, type);
    const trigIdx = findTriggerIndex(pts, stops, type);

    const PAD = { top: 22, right: 16, bottom: 28, left: 42 };
    const cW = W - PAD.left - PAD.right;
    const cH = H - PAD.top  - PAD.bottom;

    const allPrices = [...pts.map(p => p.p), ...stops.map(s => s.s)];
    const minP = Math.min(...allPrices) - 4;
    const maxP = Math.max(...allPrices) + 4;

    const totalSteps = pts.length - 1;
    const xOf = (t: number) => PAD.left + (t / totalSteps) * cW;
    const yOf = (p: number) => PAD.top  + (1 - (p - minP) / (maxP - minP)) * cH;

    const priceCoords = pts.map(p => ({ x: xOf(p.t), y: yOf(p.p) }));
    const stopCoords  = stops.map(s => ({ x: xOf(s.t), y: yOf(s.s) }));

    const { visCoords: visPriceCoords, tipX: pTipX, tipY: pTipY } =
        interpAtProgress(priceCoords, progress);
    const { visCoords: visStopCoords, tipX: sTipX, tipY: sTipY } =
        interpAtProgress(stopCoords, progress);

    const trigProgress = trigIdx !== null ? trigIdx / totalSteps : Infinity;
    const trigVisible  = progress >= trigProgress && trigIdx !== null;
    const trigFade = trigIdx !== null
        ? Math.min(1, (progress - trigProgress) / 0.08)
        : 0;

    ctx.clearRect(0, 0, W, H);

    for (let i = 0; i <= 4; i++) {
        const y = PAD.top + (i / 4) * cH;
        ctx.beginPath();
        ctx.moveTo(PAD.left, y);
        ctx.lineTo(W - PAD.right, y);
        ctx.strokeStyle = COLOR_GRID;
        ctx.lineWidth = 0.5;
        ctx.stroke();
        const val = Math.round(maxP - (i / 4) * (maxP - minP));
        ctx.fillStyle = COLOR_TEXT;
        ctx.font = "10px system-ui, sans-serif";
        ctx.textAlign = "right";
        ctx.fillText(String(val), PAD.left - 5, y + 4);
    }

    ctx.beginPath();
    ctx.moveTo(PAD.left, PAD.top);
    ctx.lineTo(PAD.left, PAD.top + cH);
    ctx.lineTo(W - PAD.right, PAD.top + cH);
    ctx.strokeStyle = COLOR_AXIS;
    ctx.lineWidth = 1;
    ctx.stroke();

    const labelStep = totalSteps > 16 ? 4 : totalSteps > 8 ? 2 : 1;
    for (let i = 0; i < pts.length; i += labelStep) {
        ctx.fillStyle = COLOR_TEXT;
        ctx.font = "9px system-ui, sans-serif";
        ctx.textAlign = "center";
        ctx.fillText(`T${i}`, xOf(i), H - PAD.bottom + 13);
    }

    if (visPriceCoords.length >= 2) {
        ctx.beginPath();
        drawLinearPath(ctx, visPriceCoords, pTipX, pTipY);
        ctx.lineTo(sTipX, sTipY);
        const reversedStop = [...visStopCoords].reverse();
        for (const c of reversedStop) ctx.lineTo(c.x, c.y);
        ctx.closePath();
        ctx.fillStyle = type === "sell"
            ? "rgba(77,142,245,0.07)"
            : "rgba(240,74,74,0.07)";
        ctx.fill();
    }

    if (visStopCoords.length >= 2) {
        ctx.beginPath();
        ctx.setLineDash([5, 4]);
        ctx.lineWidth = 1.5;
        ctx.strokeStyle = COLOR_STOP;
        ctx.globalAlpha = 0.75;
        drawLinearPath(ctx, visStopCoords, sTipX, sTipY);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.globalAlpha = 1;
    }

    if (visPriceCoords.length >= 2) {
        ctx.beginPath();
        ctx.lineWidth = 2;
        ctx.strokeStyle = COLOR_PRICE;
        drawLinearPath(ctx, visPriceCoords, pTipX, pTipY);
        ctx.stroke();
    }

    if (type === "sell") {
        let peakIdx = 0;
        for (let i = 1; i < pts.length; i++) {
            if (pts[i].p > pts[peakIdx].p) peakIdx = i;
        }
        const peakProgress = peakIdx / totalSteps;
        if (progress >= peakProgress) {
            const fade = Math.min(1, (progress - peakProgress) / 0.06);
            const px = xOf(pts[peakIdx].t);
            const py = yOf(pts[peakIdx].p);
            ctx.globalAlpha = fade;
            ctx.beginPath();
            ctx.arc(px, py, 3.5, 0, Math.PI * 2);
            ctx.fillStyle = COLOR_PRICE;
            ctx.fill();
            ctx.fillStyle = COLOR_TEXT;
            ctx.font = "10px system-ui, sans-serif";
            ctx.textAlign = px > W - PAD.right - 18 ? "right" : "center";
            ctx.fillText("최고가", px > W - PAD.right - 18 ? px + 12 : px, py - 8);
            ctx.globalAlpha = 1;
        }
    } else {
        let troughIdx = 0;
        for (let i = 1; i < pts.length; i++) {
            if (pts[i].p < pts[troughIdx].p) troughIdx = i;
        }
        const troughProgress = troughIdx / totalSteps;
        if (progress >= troughProgress) {
            const fade = Math.min(1, (progress - troughProgress) / 0.06);
            const px = xOf(pts[troughIdx].t);
            const py = yOf(pts[troughIdx].p);
            ctx.globalAlpha = fade;
            ctx.beginPath();
            ctx.arc(px, py, 3.5, 0, Math.PI * 2);
            ctx.fillStyle = COLOR_PRICE;
            ctx.fill();
            ctx.fillStyle = COLOR_TEXT;
            ctx.font = "10px system-ui, sans-serif";
            ctx.textAlign = px > W - PAD.right - 18 ? "right" : "center";
            ctx.fillText("최저가", px > W - PAD.right - 18 ? px + 12 : px - 24, py);
            ctx.globalAlpha = 1;
        }
    }

    if (trigVisible && trigIdx !== null) {
        const tx = xOf(pts[trigIdx].t);
        const ty = yOf(pts[trigIdx].p);

        ctx.globalAlpha = trigFade * 0.35;
        ctx.beginPath();
        ctx.setLineDash([3, 3]);
        ctx.lineWidth = 0.5;
        ctx.strokeStyle = COLOR_TRIGGER;
        ctx.moveTo(tx, PAD.top);
        ctx.lineTo(tx, ty - 12);
        ctx.stroke();
        ctx.setLineDash([]);

        ctx.globalAlpha = trigFade * 0.18;
        ctx.beginPath();
        ctx.arc(tx, ty, 13, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_TRIGGER;
        ctx.fill();

        ctx.globalAlpha = trigFade;
        ctx.beginPath();
        ctx.arc(tx, ty, 8, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_PRICE;
        ctx.fill();

        ctx.beginPath();
        ctx.arc(tx, ty, 5.5, 0, Math.PI * 2);
        ctx.fillStyle = COLOR_TRIGGER;
        ctx.fill();

        ctx.beginPath();
        ctx.lineWidth = 1.3;
        ctx.strokeStyle = COLOR_PRICE;
        ctx.moveTo(tx - 4, ty);
        ctx.lineTo(tx + 4, ty);
        ctx.moveTo(tx, ty - 4);
        ctx.lineTo(tx, ty + 4);
        ctx.stroke();

        const label = type === "sell" ? "매도가" : "매수가";
        const priceLabel = `${Math.round(pts[trigIdx].p)}`;
        const isRight = tx > W * 0.65;
        const isTop   = ty < PAD.top + cH * 0.3;
        const lx  = tx + (isRight ? -11 : 11);
        const ly1 = isTop ? ty + 13 : ty - 17;
        const ly2 = isTop ? ty + 28 : ty - 2;

        ctx.fillStyle = COLOR_TRIGGER;
        ctx.font = "600 10px system-ui, sans-serif";
        ctx.textAlign = isRight ? "right" : "left";
        ctx.fillText(label, lx, ly1);

        ctx.fillStyle = COLOR_PRICE;
        ctx.font = "700 13px system-ui, sans-serif";
        ctx.fillText(priceLabel, lx, ly2);

        ctx.globalAlpha = 1;
    }

    ctx.beginPath();
    ctx.arc(pTipX, pTipY, 3, 0, Math.PI * 2);
    ctx.fillStyle = COLOR_PRICE;
    ctx.fill();
}

interface TrailingChartProps {
    type: "sell" | "buy";
    scenario: Scenario;
    replayKey: number;
}

function TrailingChart({ type, scenario, replayKey }: TrailingChartProps) {
    const canvasRef      = useRef<HTMLCanvasElement>(null);
    const rafRef         = useRef<number>(0);
    const isAnimatingRef = useRef<boolean>(false);

    const runAnimation = useCallback(() => {
        if (rafRef.current) cancelAnimationFrame(rafRef.current);
        const totalSteps = makePricePoints(type, scenario).length - 1;
        const DURATION = totalSteps * (scenario === "trend" ? 100 : 280);
        const FADE_IN_DURATION = DURATION * 0.08;
        const TOTAL_DURATION = DURATION + FADE_IN_DURATION;
        const start = performance.now();

        isAnimatingRef.current = true;

        const frame = (now: number) => {
            const elapsed = now - start;
            const raw = Math.max(0, elapsed / DURATION);
            if (canvasRef.current) drawTrailingChart(canvasRef.current, type, scenario, raw);
            if (elapsed < TOTAL_DURATION) {
                rafRef.current = requestAnimationFrame(frame);
            } else {
                if (canvasRef.current) drawTrailingChart(canvasRef.current, type, scenario, 1.1);
                isAnimatingRef.current = false;
            }
        };
        rafRef.current = requestAnimationFrame(frame);
    }, [type, scenario]);

    useEffect(() => {
        runAnimation();
        return () => {
            if (rafRef.current) cancelAnimationFrame(rafRef.current);
            isAnimatingRef.current = false;
        };
    }, [runAnimation, replayKey]);

    useEffect(() => {
        const canvas = canvasRef.current;
        if (!canvas) return;
        const ro = new ResizeObserver(() => {
            if (isAnimatingRef.current) return;
            if (canvasRef.current) drawTrailingChart(canvasRef.current, type, scenario, 1.1);
        });
        ro.observe(canvas);
        return () => ro.disconnect();
    }, [type, scenario]);

    return (
        <canvas
            ref={canvasRef}
            className="ao-trailing-canvas"
            aria-label={`${type === "sell" ? "매도" : "매수"} 트레일링 스탑 차트 (${scenario === "trend" ? "추세 반복형" : "급등락형"})`}
        />
    );
}

type InfoMode = "sell" | "buy";

function TrailingStopInfo() {
    const [mode, setMode] = useState<InfoMode>("sell");
    const [scenario, setScenario] = useState<Scenario>("trend");
    const [replayKey, setReplayKey] = useState(0);

    const handleTabChange = (next: InfoMode) => {
        setMode(next);
        setReplayKey(k => k + 1);
    };

    const isSell = mode === "sell";

    return (
        <div className="ao-info">
            <div className="ao-info__mode-tabs">
                <button
                    className={`ao-info__mode-tab ao-info__mode-tab--buy ${mode === "buy" ? "ao-info__mode-tab--active" : ""}`}
                    onClick={() => handleTabChange("buy")}
                >
                    매수 트레일링 스탑
                </button>
                <button
                    className={`ao-info__mode-tab ao-info__mode-tab--sell ${mode === "sell" ? "ao-info__mode-tab--active" : ""}`}
                    onClick={() => handleTabChange("sell")}
                >
                    매도 트레일링 스탑
                </button>
            </div>

            <div className="ao-diagram">
                <div className="ao-diagram__header">
                    <div className="ao-diagram__legend">
                        <div className="ao-diagram__legend-item">
                            <span className="ao-diagram__legend-line ao-diagram__legend-line--price" />
                            <span>현재 가격</span>
                        </div>
                        <div className="ao-diagram__legend-item">
                            <span className={`ao-diagram__legend-line ao-diagram__legend-line--stop ao-diagram__legend-line--stop-${mode}`} />
                            <span>{isSell ? "매도" : "매수"} 스탑가</span>
                        </div>
                    </div>
                    <ScenarioSelect<Scenario>
                        ariaLabel="시나리오 선택"
                        value={scenario}
                        onChange={(v) => { setScenario(v); setReplayKey(k => k + 1); }}
                        options={[
                            { value: "trend", label: "시나리오 1 · 추세 반복형" },
                            { value: "spike", label: "시나리오 2 · 급등/급락형"   },
                        ]}
                    />
                </div>

                <TrailingChart type={mode} scenario={scenario} replayKey={replayKey} />
            </div>

            <ul className="ao-info__points">
                {isSell ? (
                    <>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--sell" />
                            가격이 오를수록 매도 스탑도 함께 상승하며, 한 번 오른 스탑은 다시 내려가지 않습니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--sell" />
                            최고가 대비 설정 비율만큼 하락하는 순간 수익을 확정하며 자동 매도됩니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--sell" />
                            고정 지정가와 달리 상승 추세를 끝까지 따라가며 더 많은 수익을 확보할 수 있습니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--sell" />
                            변동성이 큰 구간에서는 일시적 하락에도 스탑이 조기에 체결될 수 있으므로 비율 설정에 유의하세요.
                        </li>
                    </>
                ) : (
                    <>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--buy" />
                            가격이 내릴수록 매수 스탑도 함께 하락하며, 한 번 내린 스탑은 다시 올라가지 않습니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--buy" />
                            최저가 대비 설정 비율만큼 반등하는 순간 바닥 근처에서 자동 매수됩니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--buy" />
                            정확한 바닥을 예측하지 않고도 반등 초입에 자동으로 진입할 수 있습니다.
                        </li>
                        <li className="ao-info__point">
                            <span className="ao-info__point-dot ao-info__point-dot--buy" />
                            횡보 구간에서는 작은 반등에도 스탑이 체결될 수 있으므로 비율을 충분히 여유 있게 설정하는 것을 권장합니다.
                        </li>
                    </>
                )}
            </ul>
        </div>
    );
}

function TrailingStopPendingList() {
    const { trailingOrders, connected } = useAdvancedOrders();

    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [canceling, setCanceling]     = useState<boolean>(false);

    const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);

    function toggleSelect(id: string) {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    }

    function toggleAll() {
        if (selectedIds.size === trailingOrders.length) {
            setSelectedIds(new Set());
        } else {
            setSelectedIds(new Set(trailingOrders.map((o) => o.orderId)));
        }
    }

    function openCancelConfirm() {
        if (selectedIds.size === 0 || canceling) return;
        setCancelConfirmOpen(true);
    }

    async function doCancel() {
        setCanceling(true);
        const targets = trailingOrders.filter((o) => selectedIds.has(o.orderId));
        try {
            await Promise.all(
                targets.map((o) =>
                    cancelTrailingStop({
                        trailingStopId: Number(o.orderId),
                        stockCode: o.stockCode,
                    }).catch((e) => console.error("트레일링 취소 실패:", e, o.orderId))
                )
            );
        } finally {
            setCanceling(false);
            setSelectedIds(new Set());
        }
    }

    const hasSelection = selectedIds.size > 0;

    if (!connected) {
        return (
            <div className="ao-pending__empty">
                <span className="ao-pending__empty-dot" />
                <p>연결 중…</p>
            </div>
        );
    }

    if (trailingOrders.length === 0) {
        return (
            <div className="ao-pending__empty">
                <span className="ao-pending__empty-icon">⌛</span>
                <p>미체결 트레일링 스탑 주문이 없습니다.</p>
            </div>
        );
    }

    return (
        <div className="ao-pending">
            <div className="ao-pending__header">
                <span className="ao-pending__count">미체결 {trailingOrders.length}건</span>
                <Tooltip
                    text={selectedIds.size === trailingOrders.length && trailingOrders.length > 0 ? "전체 취소 해제" : "전체 취소"}
                    placement="top"
                >
                    <label className="ao-pending__all-check">
                        <input
                            type="checkbox"
                            checked={selectedIds.size === trailingOrders.length && trailingOrders.length > 0}
                            onChange={toggleAll}
                        />
                        전체선택
                    </label>
                </Tooltip>
            </div>

            <div className="ao-pending__list">
                {[...trailingOrders]
                    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                    .map((order) => (
                        <TrailingOrderCard
                            key={order.orderId}
                            order={order}
                            selected={selectedIds.has(order.orderId)}
                            onToggle={() => toggleSelect(order.orderId)}
                        />
                    ))}
            </div>

            <div className="ao-pending__footer">
                <span
                    className={`ao-pending__selected-count ${
                        hasSelection ? "ao-pending__selected-count--active" : ""
                    }`}
                >
                    {hasSelection ? `${selectedIds.size}건 선택됨` : "항목을 선택하세요"}
                </span>
                <button
                    className={`ao-pending__cancel-btn ${
                        !hasSelection ? "ao-pending__cancel-btn--disabled" : ""
                    }`}
                    onClick={openCancelConfirm}
                    disabled={!hasSelection || canceling}
                >
                    {canceling ? "취소 중…" : "주문 취소"}
                </button>
            </div>

            <SimpleCancelConfirmModal
                open={cancelConfirmOpen}
                count={selectedIds.size}
                onClose={() => setCancelConfirmOpen(false)}
                onConfirm={doCancel}
            />
        </div>
    );
}

interface TrailingOrderCardProps {
    order: TrailingStopPendingOrder;
    selected: boolean;
    onToggle: () => void;
}

function TrailingOrderCard({ order, selected, onToggle }: TrailingOrderCardProps) {
    const isBuy = order.side === "BUY";

    const remaining = order.quantity;
    const total     = order.quantity;
    const fillPct   = total > 0
        ? Math.min(100, Math.max(0, (remaining / total) * 100))
        : 0;
    const isPartial = remaining < total;
    const sideClass = isBuy ? "bar--buy" : "bar--sell";

    return (
        <div
            className={`ao-pending-card ${selected ? "ao-pending-card--selected" : ""}`}
            onClick={onToggle}
        >
            <div className="ao-pending-card__head">
                <Tooltip
                    text={selected ? "취소 해제" : "취소"}
                    placement="top"
                >
                    <input
                        type="checkbox"
                        className="ao-pending-card__checkbox"
                        checked={selected}
                        onChange={onToggle}
                        onClick={(e) => e.stopPropagation()}
                    />
                </Tooltip>
                <div className="ao-pending-card__stock">
                    <span className="ao-pending-card__name">{order.stockName}</span>
                    <span className="ao-pending-card__code">{order.stockCode}</span>
                </div>
                <span className="ao-pending-card__time">
        {formatTimestamp(order.createdAt)}
    </span>
            </div>

            <div className="ao-pending-card__body">
                <div className="ao-pending-card__row">
                    <span className={`ao-pending-badge ${isBuy ? "ao-pending-badge--buy" : "ao-pending-badge--sell"}`}>
                        {isBuy ? "매수" : "매도"}
                    </span>
                    <LeverageBadge leverage={order.leverage} />
                </div>

                <div className="ao-pending-card__row">
                    <div className="ao-pending-card__field">
                        <span className="ao-pending-card__field-label">기준가</span>
                        <span className="ao-pending-card__field-value">
                            {order.basePrice.toLocaleString()}원
                        </span>
                    </div>
                    <div className="ao-pending-card__field">
                        <span className="ao-pending-card__field-label">
                            {isBuy ? "매수" : "매도"} 스탑
                        </span>
                        <span className={`ao-pending-card__field-value ${
                            isBuy
                                ? "ao-pending-card__field-value--trail-buy"
                                : "ao-pending-card__field-value--trail-sell"
                        }`}>
                            {order.stopPercent.toFixed(1)}%
                        </span>
                    </div>
                    <div className="ao-pending-card__field">
                        <span className="ao-pending-card__field-label">
                            예상 체결가
                        </span>
                        <span className={`ao-pending-card__field-value ${
                            isBuy
                                ? "ao-pending-card__field-value--trail-buy"
                                : "ao-pending-card__field-value--trail-sell"
                        }`}>
                            {order.triggerPrice.toLocaleString()}원
                        </span>
                    </div>
                </div>

                <div className="ao-pending-card__qty-row">
                    <span className="ao-pending-card__remaining">{remaining.toLocaleString()}</span>
                    <span className="ao-pending-card__unit">주</span>
                    <span className="ao-pending-card__qty-sep">/</span>
                    <span className="ao-pending-card__total">{total.toLocaleString()}</span>
                    <span className="ao-pending-card__unit">주</span>
                    <span className={`ao-pending-card__fill-badge ${isPartial ? "fill-badge--partial" : "fill-badge--none"}`}>
                        {isPartial ? "부분체결" : "미체결"}
                    </span>
                </div>

                <div className="ao-pending-card__bar-track">
                    <div
                        className={`ao-pending-card__bar-fill ${sideClass}`}
                        style={{ width: `${fillPct}%` }}
                    />
                </div>
            </div>
        </div>
    );
}
