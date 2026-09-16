import { useState, useEffect } from "react";
import "./OrderPanel.css";
import { usePendingOrders, type PendingOrder } from "../context/PendingOrderContext";
import { useOrderPrice } from "../context/OrderPriceContext";
import { useStock } from "../context/StockContext";
import { useStockRealtime } from "../context/StockRealtimeContext";
import { useAccount } from "../context/AccountContext";
import { FaSlidersH } from "react-icons/fa";
import AdvancedOrder from "./modal/AdvancedOrder";
import OrderConfirmModal from "./modal/order/OrderConfirmModal";
import CancelConfirmModal from "./modal/cancel/CancelConfirmModal";
import Tooltip from "../../tooltip/Tooltip";
import type { LeverageRatio } from "../../types/order";

type MainTab = "buy" | "sell" | "cancel";
type OrderType = "market" | "limit" | "conditional";

const MAX_VALUE = 99_999_999;

const RATIO_OPTIONS: { label: string; ratio: number }[] = [
    { label: "10%", ratio: 0.1 },
    { label: "25%", ratio: 0.25 },
    { label: "50%", ratio: 0.5 },
    { label: "75%", ratio: 0.75 },
    { label: "전체", ratio: 1 },
];

const LEVERAGE_OPTIONS: { leverage: number; label: string; marginRate: number; apiValue: LeverageRatio }[] = [
    { leverage: 1.5, label: "1.5배", marginRate: 66.7, apiValue: "X1_5" },
    { leverage: 2,   label: "2배",   marginRate: 50,   apiValue: "X2" },
    { leverage: 2.5, label: "2.5배", marginRate: 40,   apiValue: "X2_5" },
];

function formatNumber(value: number): string {
    return value.toLocaleString();
}

function parseNumber(value: string): number {
    const onlyDigits = value.replace(/[^0-9]/g, "");
    return onlyDigits === "" ? 0 : Number(onlyDigits);
}

function clampQuantity(value: number): number {
    return Math.min(MAX_VALUE, Math.max(0, value));
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
    const clamped = Math.min(MAX_VALUE, Math.max(0, price));
    const tick = getTickSize(clamped);
    const baseline = getTickBaseline(clamped);
    const offset = Math.round((clamped - baseline) / tick) * tick;
    return Math.min(MAX_VALUE, Math.max(0, baseline + offset));
}

function stepPrice(current: number, direction: 1 | -1): number {
    const tick = getTickSize(current);
    const next = current + direction * tick;
    return snapToTick(next);
}

function LeverageTag({ leverage }: { leverage: number }) {
    const isCash = leverage === 1;

    const levClass =
        isCash           ? "cash" :
            leverage === 1.5 ? "lev-2" :
                leverage === 2   ? "lev-3" :
                    leverage === 2.5 ? "lev-5" :
                        "lev-other";

    return (
        <span className={`pending-item__leverage-badge ${levClass}`}>
            {isCash ? "현금" : `${leverage}x`}
        </span>
    );
}

function LeverageBadge({ leverage }: { leverage: number }) {
    const isCash = leverage === 1;

    const levClass =
        isCash           ? "cash" :
            leverage === 1.5 ? "lev-2" :
                leverage === 2   ? "lev-3" :
                    leverage === 2.5 ? "lev-5" :
                        "lev-other";

    return (
        <span className={`pending-item__leverage-badge ${levClass}`}>
            {isCash ? "현금" : `${leverage}x`}
        </span>
    );
}

export default function OrderPanel() {
    const [mainTab, setMainTab] = useState<MainTab>("buy");
    const [advancedOpen, setAdvancedOpen] = useState(false);

    const { pendingOrders } = usePendingOrders();
    const { selectedStock } = useStock();

    const isUnsupported = !selectedStock.realtimeSupported;

    return (
        <div className="order-panel">
            <div className="order-panel__title-row">
                <h2 className="order-panel__title">주문</h2>
                {!isUnsupported && (
                    <button
                        type="button"
                        className="advanced-order-btn"
                        onClick={() => setAdvancedOpen(true)}
                    >
                        <FaSlidersH size={11} />
                        고급 주문
                    </button>
                )}
            </div>

            <div className="order-panel__tabs">
                <button
                    className={`tab-btn tab-btn--buy ${mainTab === "buy" ? "tab-btn--active" : ""}`}
                    onClick={() => setMainTab("buy")}
                >
                    매수
                </button>
                <button
                    className={`tab-btn tab-btn--sell ${mainTab === "sell" ? "tab-btn--active" : ""}`}
                    onClick={() => setMainTab("sell")}
                >
                    매도
                </button>
                <button
                    className={`tab-btn tab-btn--cancel ${mainTab === "cancel" ? "tab-btn--active" : ""}`}
                    onClick={() => setMainTab("cancel")}
                >
                    취소
                </button>
            </div>

            <div className="order-panel__body">
                {mainTab === "buy"    && <TradeForm mode="buy" />}
                {mainTab === "sell"   && <TradeForm mode="sell" />}
                {mainTab === "cancel" && <CancelTab orders={pendingOrders} />}
            </div>

            <AdvancedOrder open={advancedOpen} onClose={() => setAdvancedOpen(false)} />
        </div>
    );
}

type ConfirmInfo = {
    side: "buy" | "sell";
    orderType: OrderType;
    stockName: string;
    stockCode: string;
    quantity: number;
    price: number;
    watchPrice?: number;
    estimatedAmount: number;
    isCredit: boolean;
    leverage: number;
    profitAmount?: number;
    profitRate?: number;
};

function TradeForm({ mode }: { mode: "buy" | "sell" }) {
    const [orderType, setOrderType]   = useState<OrderType>("limit");
    const [quantity,  setQuantity]    = useState<number>(0);
    const [price,     setPrice]       = useState<number>(0);
    const [watchPrice, setWatchPrice] = useState<number>(0);

    const [isCredit, setIsCredit] = useState<boolean>(false);

    const [leverage, setLeverage] = useState<number>(1.5);

    const selectedLeverageOption =
        LEVERAGE_OPTIONS.find((opt) => opt.leverage === leverage) ?? LEVERAGE_OPTIONS[0];

    const [confirmOpen, setConfirmOpen]   = useState<boolean>(false);
    const [confirmInfo, setConfirmInfo]   = useState<ConfirmInfo | null>(null);

    const { selectedPrice } = useOrderPrice();

    useEffect(() => {
        if (selectedPrice !== null) {
            setPrice(snapToTick(selectedPrice));
        }
    }, [selectedPrice]);

    const { selectedStock } = useStock();

    const { orderbook } = useStockRealtime();

    const { account } = useAccount();

    const stockInfo = account?.stocks[selectedStock.code];
    const holding = stockInfo
        ? {
            availableQty: stockInfo.availableQuantity,
            avgBuyPrice: stockInfo.quantity > 0 ? Math.round(stockInfo.totalAmount / stockInfo.quantity) : 0,
        }
        : undefined;

    const [appliedStockCode, setAppliedStockCode] = useState<string | null>(null);

    useEffect(() => {
        if (appliedStockCode === selectedStock.code) return;

        const baseAsk = orderbook?.asks?.[0]?.price;
        const baseBid = orderbook?.bids?.[0]?.price;
        const base = baseAsk ?? baseBid;
        if (!base) return;

        const snapped = snapToTick(base);
        setPrice(snapped);
        setWatchPrice(snapped);
        setAppliedStockCode(selectedStock.code);
    }, [selectedStock.code, orderbook, appliedStockCode]);

    const isBuy           = mode === "buy";
    const accentClass     = isBuy ? "accent--buy" : "accent--sell";

    const marketPrice = isBuy
        ? (orderbook?.asks && orderbook.asks.length > 0
            ? orderbook.asks[0].price
            : 0)
        : (orderbook?.bids && orderbook.bids.length > 0
            ? orderbook.bids[0].price
            : 0);

    const effectivePrice = orderType === "market" ? marketPrice : price;
    const estimatedAmount = quantity * effectivePrice;

    const profitAmount = holding ? (effectivePrice - holding.avgBuyPrice) * quantity : 0;
    const profitRate = profitAmount === 0
        ? 0
        : holding && holding.avgBuyPrice !== 0
            ? ((effectivePrice - holding.avgBuyPrice) / holding.avgBuyPrice) * 100
            : 0;

    const profitSign  = profitAmount > 0 ? "+" : profitAmount < 0 ? "−" : "";
    const profitText  = `${profitSign}${Math.abs(profitAmount).toLocaleString()}원`;
    const rateSign    = profitRate > 0 ? "+" : profitRate < 0 ? "−" : "";
    const rateText    = `${rateSign}${Math.abs(profitRate).toFixed(2)}%`;
    const profitColorClass =
        profitAmount > 0 ? "summary__value--profit" :
            profitAmount < 0 ? "summary__value--loss" : "";

    if (!selectedStock.realtimeSupported) {
        return (
            <div className="trade-form trade-form--unsupported">
                <p className="trade-form__unsupported-msg">
                    모의투자를 지원하지 않는 종목입니다
                </p>
            </div>
        );
    }

    function handleRatioClick(ratio: number) {
        if (isBuy) {
            if (effectivePrice <= 0) {
                setQuantity(0);
                return;
            }
            const budgetAmount = (account?.availableCash ?? 0) * (isCredit ? leverage : 1);
            const affordableQty = Math.floor((budgetAmount * ratio) / effectivePrice);
            setQuantity(clampQuantity(affordableQty));
        } else {
            const availableQty = holding?.availableQty ?? 0;
            setQuantity(clampQuantity(Math.floor(availableQty * ratio)));
        }
    }

    function handleOrder() {
        const orderPrice = orderType === "market" ? marketPrice : price;

        const displayAmount = (isBuy && isCredit)
            ? Math.floor(estimatedAmount / leverage)
            : estimatedAmount;

        setConfirmInfo({
            side:            mode,
            orderType,
            stockName:       selectedStock.name,
            stockCode:       selectedStock.code,
            quantity,
            price:           orderPrice,
            watchPrice:      orderType === "conditional" ? watchPrice : undefined,
            estimatedAmount: displayAmount,
            isCredit,
            leverage:        isCredit ? leverage : 1,
            profitAmount: !isBuy ? profitAmount : undefined,
            profitRate:   !isBuy ? profitRate   : undefined,
        });
        setConfirmOpen(true);
    }

    function handleConfirmSuccess() {
        setQuantity(0);
        setConfirmInfo(null);
    }

    const orderableAmountRaw = account?.availableCash ?? 0;
    const estimatedAmountRaw = estimatedAmount;
    const estimatedAmountAfter = (isBuy && isCredit)
        ? Math.floor(estimatedAmountRaw / leverage)
        : estimatedAmountRaw;
    const showEstimatedBeforeAfter = isBuy && isCredit;

    return (
        <div className="trade-form">
            <div className="form-row form-row--type">
                <span className="form-label">주문구분</span>
                <div className="type-group">
                    <button
                        className={`type-btn ${orderType === "market" ? `type-btn--active ${accentClass}` : ""}`}
                        onClick={() => setOrderType("market")}
                    >
                        시장가
                    </button>
                    <button
                        className={`type-btn ${orderType === "limit" ? `type-btn--active ${accentClass}` : ""}`}
                        onClick={() => setOrderType("limit")}
                    >
                        지정가
                    </button>
                    <button
                        className={`type-btn ${orderType === "conditional" ? `type-btn--active ${accentClass}` : ""}`}
                        onClick={() => setOrderType("conditional")}
                    >
                        조건부
                    </button>
                </div>
            </div>

            <div className="divider" />

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
                    <button className="stepper__btn" onClick={() => setQuantity((q) => clampQuantity(q - 1))}>−</button>
                    <input
                        className="stepper__input"
                        type="text"
                        inputMode="numeric"
                        value={formatNumber(quantity)}
                        onChange={(e) => setQuantity(clampQuantity(parseNumber(e.target.value)))}
                    />
                    <button className="stepper__btn" onClick={() => setQuantity((q) => clampQuantity(q + 1))}>+</button>
                    <span className="stepper__unit">주</span>
                </div>
            </div>

            <div className={`conditional-wrapper ${orderType !== "market" ? "conditional-wrapper--visible" : ""}`}>
                <div className="form-row">
                    <span className="form-label">가격</span>
                    <div className="stepper">
                        <button className="stepper__btn" onClick={() => setPrice((p) => stepPrice(p, -1))}>−</button>
                        <input
                            className="stepper__input stepper__input--wide"
                            type="text"
                            inputMode="numeric"
                            value={formatNumber(price)}
                            onChange={(e) =>
                                setPrice(snapToTick(Math.min(MAX_VALUE, Math.max(0, parseNumber(e.target.value)))))
                            }
                        />
                        <button className="stepper__btn" onClick={() => setPrice((p) => stepPrice(p, 1))}>+</button>
                        <span className="stepper__unit">원</span>
                    </div>
                </div>
            </div>

            <div className={`conditional-wrapper ${orderType === "market" ? "conditional-wrapper--visible" : ""}`}>
                <div className="form-row">
                    <span className="form-label">예상 가격</span>
                    <div className="stepper stepper--readonly">
                        <button className="stepper__btn" disabled aria-hidden="true">−</button>
                        <input
                            className="stepper__input stepper__input--wide stepper__input--readonly"
                            type="text"
                            readOnly
                            tabIndex={-1}
                            value={marketPrice > 0 ? formatNumber(marketPrice) : "—"}
                        />
                        <button className="stepper__btn" disabled aria-hidden="true">+</button>
                        <span className="stepper__unit">원</span>
                    </div>
                </div>
            </div>

            <div className={`conditional-wrapper ${orderType === "conditional" ? "conditional-wrapper--visible" : ""}`}>
                <div className="form-row">
                    <span className="form-label">감시가</span>
                    <div className="stepper">
                        <button className="stepper__btn" onClick={() => setWatchPrice((p) => stepPrice(p, -1))}>−</button>
                        <input
                            className="stepper__input stepper__input--wide"
                            type="text"
                            inputMode="numeric"
                            value={formatNumber(watchPrice)}
                            onChange={(e) =>
                                setWatchPrice(snapToTick(Math.min(MAX_VALUE, Math.max(0, parseNumber(e.target.value)))))
                            }
                        />
                        <button className="stepper__btn" onClick={() => setWatchPrice((p) => stepPrice(p, 1))}>+</button>
                        <span className="stepper__unit">원</span>
                    </div>
                </div>
            </div>

            <div className="divider" />

            <div className="summary">
                <div className="summary__row">
                    <span className="summary__label">
                        주문가능
                        <LeverageTag leverage={isCredit ? leverage : 1} />
                    </span>
                    <span className="summary__value">
                        {isBuy ? (
                            `${orderableAmountRaw.toLocaleString()} 원`
                        ) : (
                            `${(holding?.availableQty ?? 0).toLocaleString()} 주`
                        )}
                    </span>
                </div>

                <div className="summary__row">
                    <span className="summary__label">
                        예상금액
                        <LeverageTag leverage={isCredit ? leverage : 1} />
                    </span>
                    <span className="summary__value">
                        {showEstimatedBeforeAfter ? (
                            <>
                                <span className="summary__value--after">
                                    {estimatedAmountAfter.toLocaleString()} 원
                                </span>
                                <span className="summary__value--before">
                                    {estimatedAmountRaw.toLocaleString()} 원
                                </span>
                            </>
                        ) : (
                            `${estimatedAmountAfter.toLocaleString()} 원`
                        )}
                    </span>
                </div>

                {!isBuy && (
                    <div className="summary__row">
                        <span className="summary__label">
                            예상손익
                            <LeverageTag leverage={isCredit ? leverage : 1} />
                        </span>
                        <span className={`summary__value ${profitColorClass}`}>
                            {profitText} ({rateText})
                        </span>
                    </div>
                )}
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
                            {LEVERAGE_OPTIONS.map(({ leverage: lv, label }) => (
                                <button
                                    key={lv}
                                    type="button"
                                    disabled={!isCredit}
                                    className={`leverage-btn ${
                                        isCredit && leverage === lv
                                            ? `leverage-btn--active leverage-btn--${lv}`
                                            : ""
                                    }`}
                                    onClick={() => setLeverage(lv)}
                                >
                                    {label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className={`credit-area__row2 ${!isCredit ? "credit-area__row2--disabled" : ""}`}>
                        <span className="credit-area__margin-label">개시증거금률</span>
                        <span className="credit-area__margin-value">{selectedLeverageOption.marginRate}%</span>
                    </div>
                </div>

                <button
                    className={`order-btn ${isBuy ? "order-btn--buy" : "order-btn--sell"}`}
                    onClick={handleOrder}
                >
                    {isCredit ? "신용" : "현금"} {isBuy ? "매수" : "매도"}
                </button>
            </div>

            {confirmInfo && (
                <OrderConfirmModal
                    open={confirmOpen}
                    onClose={() => setConfirmOpen(false)}
                    onConfirm={handleConfirmSuccess}
                    side={confirmInfo.side}
                    orderType={confirmInfo.orderType}
                    stockName={confirmInfo.stockName}
                    stockCode={confirmInfo.stockCode}
                    quantity={confirmInfo.quantity}
                    price={confirmInfo.price}
                    watchPrice={confirmInfo.watchPrice}
                    estimatedAmount={confirmInfo.estimatedAmount}
                    isCredit={confirmInfo.isCredit}
                    leverage={confirmInfo.leverage}
                    profitAmount={confirmInfo.profitAmount}
                    profitRate={confirmInfo.profitRate}
                />
            )}
        </div>
    );
}

function CancelTab({ orders }: { orders: PendingOrder[] }) {
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
    const [confirmOpen, setConfirmOpen] = useState(false);

    function toggleSelect(id: string) {
        setSelectedIds((prev) => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    }

    function toggleAll() {
        if (selectedIds.size === orders.length) {
            setSelectedIds(new Set());
        } else {
            setSelectedIds(new Set(orders.map((o) => o.orderId)));
        }
    }

    function handleCancel() {
        if (selectedIds.size === 0) return;
        setConfirmOpen(true);
    }

    function handleConfirmSuccess() {
        setSelectedIds(new Set());
    }

    const hasSelection = selectedIds.size > 0;
    const selectedOrders = orders.filter((o) => selectedIds.has(o.orderId));

    return (
        <div className="cancel-tab">
            <div className="cancel-tab__header">
                <span className="cancel-tab__count">미체결 주문 {orders.length}건</span>
                <Tooltip
                    text={selectedIds.size === orders.length && orders.length > 0 ? "전체 취소 해제" : "전체 취소"}
                    placement="top"
                >
                    <label className="all-check">
                        <input
                            type="checkbox"
                            checked={selectedIds.size === orders.length && orders.length > 0}
                            onChange={toggleAll}
                        />
                        전체 선택
                    </label>
                </Tooltip>
            </div>

            <ul className="pending-list">
                {orders.map((order) => {
                    const remaining = Number(order.remainingQty) || 0;
                    const total     = Number(order.orderQty) || 0;
                    const fillPct   = total > 0
                        ? Math.min(100, Math.max(0, (remaining / total) * 100))
                        : 0;
                    const sideClass = order.side === "BUY" ? "bar--buy" : "bar--sell";

                    return (
                        <li
                            key={order.orderId}
                            className={`pending-item ${selectedIds.has(order.orderId) ? "pending-item--selected" : ""}`}
                            onClick={() => toggleSelect(order.orderId)}
                        >
                            <Tooltip
                                text={selectedIds.has(order.orderId) ? "취소 해제" : "취소"}
                                placement="top"
                            >
                                <input
                                    type="checkbox"
                                    checked={selectedIds.has(order.orderId)}
                                    onChange={() => toggleSelect(order.orderId)}
                                    onClick={(e) => e.stopPropagation()}
                                />
                            </Tooltip>

                            <div className="pending-item__info">
                                <div className="pending-item__stock-name-row">
                                    <span className="pending-item__stock-name">{order.stockName}</span>
                                    <span className="pending-item__stock-code">{order.stockCode}</span>
                                    <span className="pending-item__time">
                                        {order.time.slice(11, 19)}
                                    </span>
                                </div>

                                <div className="pending-item__top">
                                    <span className={`pending-item__badge ${order.side === "BUY" ? "badge--buy" : "badge--sell"}`}>
                                        {order.side === "BUY" ? "매수" : "매도"}
                                    </span>
                                    <LeverageBadge leverage={order.leverage} />
                                    <span className="pending-item__order-type">
                                        {order.orderType === "LIMIT" ? "지정가" : "조건부"}
                                    </span>
                                </div>

                                <div className="pending-item__price-row">
                                    <span className="pending-item__price-label">주문가</span>
                                    <span className="pending-item__price">{order.price.toLocaleString()}원</span>
                                    {order.orderType === "CONDITIONAL" && order.triggerPrice != null && (
                                        <>
                                            <span className="pending-item__trigger-label">감시가</span>
                                            <span className="pending-item__trigger-value">
                                                {order.triggerPrice.toLocaleString()}원
                                            </span>
                                        </>
                                    )}
                                </div>

                                <div className="pending-item__qty-row">
                                    <span className="pending-item__remaining">{remaining.toLocaleString()}</span>
                                    <span className="pending-item__unit">주</span>
                                    <span className="pending-item__qty-sep">/</span>
                                    <span className="pending-item__total"> {total.toLocaleString()}</span>
                                    <span className="pending-item__unit">주</span>
                                    <span className={`pending-item__fill-badge ${remaining === total ? "fill-badge--none" : "fill-badge--partial"}`}>
                                        {remaining === total ? "미체결" : "부분체결"}
                                    </span>
                                </div>

                                <div className="pending-item__bar-track">
                                    <div
                                        className={`pending-item__bar-fill ${sideClass}`}
                                        style={{ width: `${fillPct}%` }}
                                    />
                                </div>
                            </div>
                        </li>
                    );
                })}
            </ul>

            <div className="cancel-tab__footer">
                <span className={`cancel-tab__selected-count ${hasSelection ? "cancel-tab__selected-count--active" : ""}`}>
                    {hasSelection ? `${selectedIds.size}건 선택됨` : "항목을 선택하세요"}
                </span>
                <button
                    className={`cancel-btn ${!hasSelection ? "cancel-btn--disabled" : ""}`}
                    onClick={handleCancel}
                    disabled={!hasSelection}
                >
                    주문 취소
                </button>
            </div>

            <CancelConfirmModal
                open={confirmOpen}
                onClose={() => setConfirmOpen(false)}
                onConfirm={handleConfirmSuccess}
                count={selectedIds.size}
                selectedOrders={selectedOrders}
            />
        </div>
    );
}
