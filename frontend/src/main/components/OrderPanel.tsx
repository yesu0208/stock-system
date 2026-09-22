import { useState, useEffect } from "react";
import "./OrderPanel.css";
import { usePendingOrders, type PendingOrder } from "../context/PendingOrderContext";
import { useOrderPrice } from "../context/OrderPriceContext";
import { useStock } from "../context/StockContext";
import { useStockRealtime } from "../context/StockRealtimeContext";
import { useAccount } from "../context/AccountContext";
import { useRealtime } from "../context/RealtimeContext";
import { useMsg } from "../context/MsgContext";
import { useToast } from "../context/ToastContext";
import { useUser } from "../context/UserContext";
import { stockNameMap } from "../data/stocks";
import { FaSlidersH } from "react-icons/fa";
import AdvancedOrder from "./modal/AdvancedOrder";
import OrderConfirmModal from "./modal/order/OrderConfirmModal";
import CancelConfirmModal from "./modal/cancel/CancelConfirmModal";
import Tooltip from "../../tooltip/Tooltip";
import type { RankTier } from "../../types/rank";
import type { LeverageRatio, OrderResultResponse } from "../../types/order";
import type { CancelResultResponse } from "../../types/cancel";
import type { AutoOrderResultResponse } from "../../types/autoOrder";
import type { AutoCancelResultResponse } from "../../types/autoCancel";
import type { TradeResponse } from "../../types/trade";
import type { TrailingStopResultResponse, TrailingStopCancelResultResponse } from "../../types/trailingStop";
import type { OtocoResultResponse, OtocoCancelResultResponse } from "../../types/otoco";
import { calculateFee, calculateSellCost } from "../../utils/fee";

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
    { leverage: 1.5, label: "1.5x", marginRate: 66.7, apiValue: "X1_5" },
    { leverage: 2,   label: "2x",   marginRate: 50,   apiValue: "X2" },
    { leverage: 2.5, label: "2.5x", marginRate: 40,   apiValue: "X2_5" },
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

function getLeverageColorClass(leverage: number): string {
    return leverage === 1.5 ? "lev-2" :
        leverage === 2   ? "lev-3" :
            leverage === 2.5 ? "lev-5" :
                "lev-other";
}

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

const ORIGIN_LABEL: Record<string, string> = {
    OTOCO_ENTRY: "OTOCO 진입",
    OTOCO_TAKE_PROFIT: "OTOCO 익절",
    OTOCO_STOP_LOSS: "OTOCO 손절",
    TRAILING_STOP: "트레일링",
    AUTO_ORDER: "자동주문",
};

const ORIGIN_CLASS: Record<string, string> = {
    OTOCO_ENTRY: "origin-otoco",
    OTOCO_TAKE_PROFIT: "origin-otoco-tp",
    OTOCO_STOP_LOSS: "origin-otoco-sl",
    TRAILING_STOP: "origin-trailing",
    AUTO_ORDER: "origin-auto",
};

function OriginBadge({ origin }: { origin?: string }) {
    if (!origin || origin === "MANUAL") return null;
    return (
        <span className={`pending-item__origin-badge ${ORIGIN_CLASS[origin] ?? ""}`}>
            {ORIGIN_LABEL[origin] ?? origin}
        </span>
    );
}

export default function OrderPanel() {
    const [mainTab, setMainTab] = useState<MainTab>("buy");
    const [advancedOpen, setAdvancedOpen] = useState(false);

    const { pendingOrders } = usePendingOrders();
    const { selectedStock } = useStock();

    const isUnsupported = !selectedStock.realtimeSupported;

    const { subscribeDestination } = useRealtime();
    const { success, error } = useMsg();
    const { showToast } = useToast();

    useEffect(() => {
        const unsubOrder = subscribeDestination(
            "/user/sub/order/result",
            (data: OrderResultResponse) => {
                const sideLabel = data.orderType === "BUY" ? "매수" : "매도";
                const stockName = stockNameMap[data.stockCode] ?? data.stockCode;

                if (data.responseType === "SUCCESS") {
                    success(`[${stockName}] ${sideLabel} 주문이 접수되었습니다.`);
                } else {
                    error(`${sideLabel} 주문 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        const unsubCancel = subscribeDestination(
            "/user/sub/cancel",
            (data: CancelResultResponse) => {
                if (data.responseType === "SUCCESS") {
                    const stockName = stockNameMap[data.stockCode] ?? data.stockCode;
                    success(`[${stockName}] 주문이 취소되었습니다.`);
                } else {
                    error(`주문 취소 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        const unsubAutoOrder = subscribeDestination(
            "/user/sub/auto/order/result",
            (data: AutoOrderResultResponse) => {
                const sideLabel = data.autoOrderType === "BUY" ? "자동 매수" : "자동 매도";

                if (data.responseType === "SUCCESS") {
                    const stockName = stockNameMap[data.stockCode] ?? data.stockCode;
                    success(`[${stockName}] ${sideLabel} 주문이 등록되었습니다.`);
                } else {
                    error(`${sideLabel} 등록 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        const unsubAutoCancel = subscribeDestination(
            "/user/sub/auto/cancel",
            (data: AutoCancelResultResponse) => {
                if (data.responseType === "SUCCESS") {
                    const stockName = stockNameMap[data.stockCode] ?? data.stockCode;
                    success(`[${stockName}] 자동주문이 취소되었습니다.`);
                } else {
                    error(`자동주문 취소 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        const unsubTrade = subscribeDestination(
            "/user/sub/trade",
            (data: TradeResponse) => {
                const sideLabel = data.tradeType === "BUY" ? "매수" : "매도";
                const stockName = stockNameMap[data.stockCode] ?? data.stockCode;

                showToast(
                    `[${sideLabel}] ${stockName} ${data.tradePrice.toLocaleString()}원 ${data.tradeQuantity}주`,
                    data.tradeType === "BUY" ? "buy" : "sell"
                );
            }
        );

        const unsubTrailingResult = subscribeDestination(
            "/user/sub/trailing-stop/result",
            (data: TrailingStopResultResponse) => {
                const sideLabel = data.trailingStopType === "BUY" ? "매수" : "매도";

                if (data.responseType === "SUCCESS") {
                    const stockName = stockNameMap[data.stockCode] ?? data.stockCode;
                    success(`[${stockName}] 트레일링 ${sideLabel} 주문이 등록되었습니다.`);
                } else {
                    error(`트레일링 ${sideLabel} 등록 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        const unsubTrailingCancel = subscribeDestination(
            "/user/sub/trailing-stop/cancel",
            (data: TrailingStopCancelResultResponse) => {
                if (data.responseType === "SUCCESS") {
                    const stockName = stockNameMap[data.stockCode] ?? data.stockCode;
                    success(`[${stockName}] 트레일링 주문이 취소되었습니다.`);
                } else {
                    error(`트레일링 취소 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        const unsubOtocoResult = subscribeDestination(
            "/user/sub/otoco/result",
            (data: OtocoResultResponse) => {
                if (data.responseType === "ERROR") {
                    error(`OTOCO 등록 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                    return;
                }

                const stockName = stockNameMap[data.stockCode] ?? data.stockCode;

                switch (data.otocoStatus) {
                    case "WAITING_ENTRY":
                        success(`[${stockName}] OTOCO 주문이 등록되었습니다.`);
                        break;
                    case "ENTRY_ORDER_PLACED":
                        success(`[${stockName}] 진입 조건이 충족되어 주문이 접수되었습니다.`);
                        break;
                    case "WAITING_EXIT":
                        success(`[${stockName}] 진입 주문이 체결되어 익절·손절 감시를 시작합니다.`);
                        break;
                    case "COMPLETED":
                        success(`[${stockName}] 익절 또는 손절 조건이 충족되어 청산되었습니다.`);
                        break;
                    default:
                        success(`[${stockName}] OTOCO 주문 상태가 갱신되었습니다.`);
                }
            }
        );

        const unsubOtocoCancel = subscribeDestination(
            "/user/sub/otoco/cancel",
            (data: OtocoCancelResultResponse) => {
                if (data.responseType === "SUCCESS") {
                    const stockName = stockNameMap[data.stockCode] ?? data.stockCode;
                    success(`[${stockName}] OTOCO 주문이 취소되었습니다.`);
                } else {
                    error(`OTOCO 취소 실패: ${data.errorMessage ?? "알 수 없는 오류"}`);
                }
            }
        );

        return () => {
            unsubOrder();
            unsubCancel();
            unsubAutoOrder();
            unsubAutoCancel();
            unsubTrade();
            unsubTrailingResult();
            unsubTrailingCancel();
            unsubOtocoResult();
            unsubOtocoCancel();
        };
    }, [subscribeDestination, success, error, showToast]);

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
    estimatedAmountRaw: number;
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

    const { user } = useUser();

    const stockInfo = account?.stocks[selectedStock.code];
    const holding = stockInfo
        ? {
            availableQty: stockInfo.availableQuantity,
            avgBuyPrice: stockInfo.quantity > 0 ? Math.round(stockInfo.totalCostAmount / stockInfo.quantity) : 0, // [수정]
        }
        : undefined;

    const leveragePosition = account?.leveragePositions?.find(
        (p) => p.stockCode === selectedStock.code && p.leverageRatio === selectedLeverageOption.apiValue
    );

    const sellAvailableQty = isCredit
        ? (leveragePosition?.availableQuantity ?? 0)
        : (holding?.availableQty ?? 0);

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

    const requiredTier = isCredit ? LEVERAGE_REQUIRED_TIER[leverage] : null;
    const isLeverageAllowed = !requiredTier || isRankAtLeast(user?.rank?.tier, requiredTier);

    const marketPrice = isBuy
        ? (orderbook?.asks && orderbook.asks.length > 0
            ? orderbook.asks[0].price
            : 0)
        : (orderbook?.bids && orderbook.bids.length > 0
            ? orderbook.bids[0].price
            : 0);

    const effectivePrice = orderType === "market" ? marketPrice : price;
    const estimatedAmount = quantity * effectivePrice;

    const sellAvgBuyPrice = isCredit
        ? (leveragePosition && leveragePosition.quantity > 0
            ? Math.round(leveragePosition.costAmount / leveragePosition.quantity) // [수정] purchaseAmount → costAmount
            : 0)
        : (holding?.avgBuyPrice ?? 0);

    const sellCost = !isBuy ? calculateSellCost(estimatedAmount) : 0;

    const profitAmount = !isBuy ? (effectivePrice - sellAvgBuyPrice) * quantity - sellCost : 0;

    const marginPerShare = isCredit && leveragePosition && leveragePosition.quantity > 0
        ? (leveragePosition.costAmount - leveragePosition.loanAmount) / leveragePosition.quantity // [수정] initialMargin → costAmount-loanAmount(투입원금액)
        : sellAvgBuyPrice;

    const profitRate = profitAmount === 0
        ? 0
        : marginPerShare !== 0
            ? (profitAmount / (marginPerShare * quantity)) * 100
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

    function clampSellQuantity(value: number): number {
        return isBuy ? clampQuantity(value) : Math.min(sellAvailableQty, clampQuantity(value));
    }

    function handleRatioClick(ratio: number) {
        if (isBuy) {
            if (effectivePrice <= 0) {
                setQuantity(0);
                return;
            }

            const divisor = isCredit ? (1 / leverage + 0.00015) : (1 + 0.00015);
            const budget = (account?.availableCash ?? 0) * ratio;
            const affordableQty = Math.floor(budget / (effectivePrice * divisor));
            setQuantity(clampQuantity(affordableQty));
        } else {
            setQuantity(clampQuantity(Math.floor(sellAvailableQty * ratio)));
        }
    }

    function handleOrder() {
        const orderPrice = orderType === "market" ? marketPrice : price;

        const displayAmount = (isBuy && isCredit)
            ? Math.floor(estimatedAmount / leverage)
            : !isBuy
                ? estimatedAmountAfter
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
            estimatedAmountRaw: estimatedAmount,
        });
        setConfirmOpen(true);
    }

    function handleConfirmSuccess() {
        setQuantity(0);
        setConfirmInfo(null);
    }

    const orderableAmountRaw = account?.availableCash ?? 0;
    const estimatedAmountRaw = estimatedAmount;

    const loanPerShare = isCredit && leveragePosition && leveragePosition.quantity > 0
        ? leveragePosition.loanAmount / leveragePosition.quantity
        : 0;

    const estimatedAmountAfter =
        isBuy && isCredit ? Math.floor(estimatedAmountRaw / leverage) :
            !isBuy && isCredit ? Math.max(0, Math.round(estimatedAmountRaw - loanPerShare * quantity - sellCost)) :
                !isBuy ? Math.max(0, Math.round(estimatedAmountRaw - sellCost)) :
                    estimatedAmountRaw;

    const showEstimatedBeforeAfter = isCredit;

    const leverageDisabledReason =
        isCredit && !isLeverageAllowed ? `${requiredTier} 등급 이상 가능` : null;
    const quantityDisabledReason =
        quantity <= 0 ? "수량 입력 필요" : null;

    const disabledTooltipText = leverageDisabledReason ?? quantityDisabledReason ?? "";
    const isOrderDisabled = leverageDisabledReason !== null || quantityDisabledReason !== null;

    const orderButton = (
        <button
            className={`order-btn ${isBuy ? "order-btn--buy" : "order-btn--sell"}`}
            onClick={handleOrder}
            disabled={isOrderDisabled}
        >
            {isCredit ? "신용" : "현금"} {isBuy ? "매수" : "매도"}
        </button>
    );

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
                    <button className="stepper__btn" onClick={() => setQuantity((q) => clampSellQuantity(q - 1))}>−</button>
                    <input
                        className="stepper__input"
                        type="text"
                        inputMode="numeric"
                        value={formatNumber(quantity)}
                        onChange={(e) => setQuantity(clampSellQuantity(parseNumber(e.target.value)))}
                    />
                    <button className="stepper__btn" onClick={() => setQuantity((q) => clampSellQuantity(q + 1))}>+</button>
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
                        {!isBuy && <LeverageTag leverage={isCredit ? leverage : 1} />}
                    </span>
                    <span className="summary__value">
                        {isBuy ? (
                            `${orderableAmountRaw.toLocaleString()} 원`
                        ) : (
                            `${sellAvailableQty.toLocaleString()} 주`
                        )}
                    </span>
                </div>

                <div className={`summary__row ${!isBuy ? "summary__row--stacked" : ""}`}>
                    <span className="summary__label">
                        {isBuy ? "주문금액" : "예상금액"}
                        <LeverageTag leverage={isCredit ? leverage : 1} />
                        {!isBuy && (
                            <Tooltip
                                text={`매도 수수료·세금(0.015%, 0.2%) ${sellCost.toLocaleString()}원 차감`}
                                placement="top"
                            >
                                <span className="fee-tag">수수료·세금</span>
                            </Tooltip>
                        )}
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

                {isBuy && (
                    <div className="summary__row summary__row--stacked">
                        <span className="summary__label">
                            필요금액
                            <LeverageTag leverage={isCredit ? leverage : 1} />
                            <Tooltip
                                text={`매수 수수료 0.015% (${calculateFee(estimatedAmountRaw).toLocaleString()}원) 포함`}
                                placement="top"
                            >
                                <span className="fee-tag">수수료</span>
                            </Tooltip>
                        </span>
                        <span className="summary__value">
                            {showEstimatedBeforeAfter ? (
                                <>
                                    <span className="summary__value--after">
                                        {(estimatedAmountAfter + calculateFee(estimatedAmountRaw)).toLocaleString()} 원
                                    </span>
                                    <span className="summary__value--before">
                                        {(estimatedAmountRaw + calculateFee(estimatedAmountRaw)).toLocaleString()} 원
                                    </span>
                                </>
                            ) : (
                                `${(estimatedAmountAfter + calculateFee(estimatedAmountRaw)).toLocaleString()} 원`
                            )}
                        </span>
                    </div>
                )}

                {!isBuy && (
                    <div className="summary__row">
                        <span className="summary__label">
                            예상손익
                            <LeverageTag leverage={isCredit ? leverage : 1} />
                            <Tooltip
                                text={`매도 수수료·세금(0.015%, 0.2%) ${sellCost.toLocaleString()}원 차감`}
                                placement="top"
                            >
                                <span className="fee-tag">수수료·세금</span>
                            </Tooltip>
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
                                    className={`leverage-btn ${getLeverageColorClass(lv)} ${
                                        isCredit && leverage === lv ? "leverage-btn--active" : ""
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
                        <span className={`credit-area__margin-value ${getLeverageColorClass(leverage)}`}>{selectedLeverageOption.marginRate}%</span>
                    </div>
                </div>

                <Tooltip text={disabledTooltipText} placement="top">
                    {orderButton}
                </Tooltip>
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
                    estimatedAmountRaw={confirmInfo.estimatedAmountRaw}
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
                                        {new Date(order.time).toLocaleTimeString("ko-KR", {
                                            hour: "2-digit",
                                            minute: "2-digit",
                                            second: "2-digit",
                                            hour12: false,
                                        })}
                                    </span>
                                </div>

                                <div className="pending-item__top">
                                    <OriginBadge origin={order.origin} />
                                    <span className={`pending-item__badge ${order.side === "BUY" ? "badge--buy" : "badge--sell"}`}>
                                        {order.side === "BUY" ? "매수" : "매도"}
                                    </span>
                                    <LeverageBadge leverage={order.leverage} />
                                    <span className="pending-item__order-type">
                                        {order.orderType === "MARKET" ? "시장가" : order.orderType === "LIMIT" ? "지정가" : "조건부"}
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
