// frontend/src/main/components/OrderPanel.tsx
import { useState, useEffect } from "react";
import "./OrderPanel.css";
import { usePendingOrders, type PendingOrder } from "../context/PendingOrderContext";
// 호가창(AskList/BidList)에서 클릭한 가격을 구독하기 위한 Context
import { useOrderPrice } from "../context/OrderPriceContext";
// 종목명/종목코드 등을 제공하는 전역 Context
import { useStock } from "../context/StockContext";
// [수정] 실시간 호가창(asks/bids) 데이터는 useRealtime이 아니라 StockRealtimeContext에 있음
import { useStockRealtime } from "../context/StockRealtimeContext";
// 계좌 요약(주문가능금액) 및 보유 종목(availableQuantity, totalAmount 등)을 제공하는 Context
import { useAccount } from "../context/AccountContext";
import { FaSlidersH } from "react-icons/fa";
import AdvancedOrder from "./modal/AdvancedOrder";
import OrderConfirmModal from "./modal/order/OrderConfirmModal";
import CancelConfirmModal from "./modal/cancel/CancelConfirmModal";
import Tooltip from "../../tooltip/Tooltip";
// [신규] 백엔드 LeverageRatio(SPOT/X1_5/X2/X2_5)와 매핑하기 위한 타입
import type { LeverageRatio } from "../../types/order";

type MainTab = "buy" | "sell" | "cancel";
type OrderType = "market" | "limit" | "conditional";

/* 수량 / 가격 / 감시가 입력 가능한 최대값 (8자리) */
const MAX_VALUE = 99_999_999;

/* ─── 비율 버튼(10% / 25% / 50% / 75% / 전체)에 사용할 비율 값 목록 ───
 * label: 버튼에 표시할 텍스트, ratio: 0~1 사이의 소수 비율 */
const RATIO_OPTIONS: { label: string; ratio: number }[] = [
    { label: "10%", ratio: 0.1 },
    { label: "25%", ratio: 0.25 },
    { label: "50%", ratio: 0.5 },
    { label: "75%", ratio: 0.75 },
    { label: "전체", ratio: 1 },
];

/* [수정] 신용 배수(레버리지) 선택 옵션 — 백엔드 LeverageRatio(SPOT/X1_5/X2/X2_5)에 맞춤
 * leverage: 화면 표시/계산용 배수(1.5/2/2.5), label: 버튼 텍스트,
 * marginRate: 유지증거금률(%) 안내용 값, apiValue: 백엔드 전송 시 사용할 enum 코드값
 *   - 백엔드 LeverageRatio: SPOT(1배, 현금) / X1_5(1.5배) / X2(2배) / X2_5(2.5배)
 *     (backend/bff-server/.../leverage/dto/LeverageRatio.java 기준)
 * ⚠️⚠️⚠️ marginRate(25/20/15%) 수치는 임시 표시값입니다. 실제 유지증거금률 정책과
 * 대조 후, 필요하다면 서버 응답(maintenanceMarginRate) 기반 값으로 교체해주세요. ⚠️⚠️⚠️ */
const LEVERAGE_OPTIONS: { leverage: number; label: string; marginRate: number; apiValue: LeverageRatio }[] = [
    { leverage: 1.5, label: "1.5배", marginRate: 25, apiValue: "X1_5" },
    { leverage: 2,   label: "2배",   marginRate: 20, apiValue: "X2" },
    { leverage: 2.5, label: "2.5배", marginRate: 15, apiValue: "X2_5" },
];

/* ─── 숫자 ,(콤마) 포맷 유틸 ───
 * formatNumber : 숫자를 "1,234" 형태의 문자열로 변환 (input value 표시용)
 * parseNumber  : 사용자가 입력한 문자열에서 콤마/숫자 이외 문자를 제거하고 number로 변환
 *   - 빈 문자열 입력 시 0으로 처리 (NaN 방지)
 * 두 함수 모두 number ↔ string(콤마 포함) 변환 지점에서 공통으로 재사용 */
function formatNumber(value: number): string {
    return value.toLocaleString();
}

function parseNumber(value: string): number {
    const onlyDigits = value.replace(/[^0-9]/g, "");
    return onlyDigits === "" ? 0 : Number(onlyDigits);
}

/* 수량 입력값 클램프: 0 ~ MAX_VALUE 범위로 제한 (수량은 호가 단위 적용 대상이 아님) */
function clampQuantity(value: number): number {
    return Math.min(MAX_VALUE, Math.max(0, value));
}

/* ─── 호가(가격) 단위 정책 ───
 * 가격 구간에 따라 +/- 시 증가/감소하는 단위가 달라짐:
 *   1       ~ 1,999   : 1원 단위
 *   2,000   ~ 4,999   : 5원 단위
 *   5,000   ~ 9,999   : 10원 단위
 *   10,000  ~ 19,999  : 10원 단위 (5,000~20,000 구간을 10원 단위로 통합)
 *   20,000  ~ 49,999  : 50원 단위
 *   50,000  ~ 199,999 : 100원 단위
 *   200,000 ~ 499,999 : 500원 단위
 *   500,000 이상       : 1,000원 단위
 *
 * getTickSize(price) : 현재 가격이 속한 구간의 호가 단위를 반환
 * 경계값(2000, 5000, 10000, 20000, 50000, 200000, 500000)은 그 다음 구간(더 큰 구간)의
 * 시작점으로 취급한다 — 예: 정확히 5000원은 "5,000~9,999" 구간(10원 단위)에 속함.
 */
function getTickSize(price: number): number {
    if (price < 2_000) return 1;
    if (price < 5_000) return 5;
    if (price < 20_000) return 10;
    if (price < 50_000) return 50;
    if (price < 200_000) return 100;
    if (price < 500_000) return 500;
    return 1_000;
}

/* 호가 구간의 "시작값"을 반환 (snapToTick에서 구간 기준점으로 사용)
 * 예: price=12,345 → getTickSize=10 구간의 시작값은 10,000 */
function getTickBaseline(price: number): number {
    if (price < 2_000) return 0;
    if (price < 5_000) return 2_000;
    if (price < 20_000) return 5_000;
    if (price < 50_000) return 20_000;
    if (price < 200_000) return 50_000;
    if (price < 500_000) return 200_000;
    return 500_000;
}

/* ─── 가격/감시가 값을 해당 구간의 유효한 호가로 스냅(정렬) ───
 * 구간의 시작점(baseline)을 기준으로 tick 배수만큼 떨어진 값으로 보정한다. */
function snapToTick(price: number): number {
    const clamped = Math.min(MAX_VALUE, Math.max(0, price));
    const tick = getTickSize(clamped);
    const baseline = getTickBaseline(clamped);
    const offset = Math.round((clamped - baseline) / tick) * tick;
    return Math.min(MAX_VALUE, Math.max(0, baseline + offset));
}

/* 가격을 한 tick만큼 증가/감소시킨 뒤 새 구간 기준으로 다시 스냅
 * direction: +1(증가) / -1(감소) */
function stepPrice(current: number, direction: 1 | -1): number {
    const tick = getTickSize(current);
    const next = current + direction * tick;
    return snapToTick(next);
}

/* ─── 신용 배수 태그 컴포넌트 ───
 * 주문가능·예상금액·예상손익 라벨 오른쪽에 상시 표시되는 인라인 레이블.
 *   - leverage === 1 (신용 미체크 = 현금 거래) → "현금", 중립 회색
 *   - 1.5 / 2 / 2.5배 → "×N" 형태, 배율별 색상 구분
 *   - 그 외(이론상 발생하지 않는 배수) → 마젠타 경고색 */
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

/* ─── 레버리지 배지 컴포넌트 (취소 탭 미체결 목록 전용) ───
 * pending-item__top(1행) 안에서 매수/매도 뱃지 바로 옆에 배치된다. */
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

    /* ── 미체결 주문 목록: PendingOrderContext에서 전역으로 관리 ── */
    const { pendingOrders } = usePendingOrders();
    const { selectedStock } = useStock();

    // [수정] StockInfo엔 group 필드가 없음 → realtimeSupported로 모의투자 지원 여부 판단
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

            {/* 메인 탭 */}
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

            {/* 탭 콘텐츠 — 높이 고정을 위해 order-panel__body가 flex 컨테이너 역할 */}
            <div className="order-panel__body">
                {mainTab === "buy"    && <TradeForm mode="buy" />}
                {mainTab === "sell"   && <TradeForm mode="sell" />}
                {mainTab === "cancel" && <CancelTab orders={pendingOrders} />}
            </div>

            <AdvancedOrder open={advancedOpen} onClose={() => setAdvancedOpen(false)} />
        </div>
    );
}

/* ─── 주문 확인 모달에 넘길 주문 정보 타입 ─── */
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

/* ─── 매수/매도 공통 폼 ─── */
function TradeForm({ mode }: { mode: "buy" | "sell" }) {
    const [orderType, setOrderType]   = useState<OrderType>("limit");
    const [quantity,  setQuantity]    = useState<number>(0);
    // [수정] 초기값 0 — 아래 useEffect에서 호가창(orderbook) 기준가로 최초 1회 세팅
    const [price,     setPrice]       = useState<number>(0);
    const [watchPrice, setWatchPrice] = useState<number>(0);

    /* ─── 신용 거래 토글 ───
     * 매수/매도 탭 각각 독립적인 TradeForm 인스턴스이므로 신용 여부도 탭별로 따로 관리 */
    const [isCredit, setIsCredit] = useState<boolean>(false);

    /* ─── 신용 배수(레버리지) 선택 ───
     * [수정] 기본값 1.5배 (백엔드 최소 배수) */
    const [leverage, setLeverage] = useState<number>(1.5);

    const selectedLeverageOption =
        LEVERAGE_OPTIONS.find((opt) => opt.leverage === leverage) ?? LEVERAGE_OPTIONS[0];

    /* ─── 주문 확인 모달 상태 ─── */
    const [confirmOpen, setConfirmOpen]   = useState<boolean>(false);
    const [confirmInfo, setConfirmInfo]   = useState<ConfirmInfo | null>(null);

    // 호가창에서 가격 셀을 클릭하면 selectedPrice가 갱신됨
    const { selectedPrice } = useOrderPrice();

    useEffect(() => {
        if (selectedPrice !== null) {
            setPrice(snapToTick(selectedPrice));
        }
    }, [selectedPrice]);

    /* 종목명/종목코드 — StockContext에서 구독
       [수정] StockContext엔 selectedStock만 존재 (detail 없음 — 그건 StockInfoPanel 전용 로컬 상태) */
    const { selectedStock } = useStock();

    /* 실시간 호가창(asks/bids) — 시장가 주문의 기준가 계산에 사용
       [수정] useRealtime()이 아니라 useStockRealtime()에서 제공됨 */
    const { orderbook } = useStockRealtime();

    /* 계좌 정보 — AccountContext에서 구독
       [수정] summary가 아니라 account(AccountResponse)를 제공 */
    const { account } = useAccount();

    /* [수정] 현재 종목의 보유 정보 계산
     * account.stocks: Record<종목코드, { quantity, availableQuantity, totalAmount }>
     * avgBuyPrice는 서버가 별도로 내려주지 않아 totalAmount/quantity로 직접 계산
     * (MyAccountModal.tsx의 계산 방식과 동일) */
    const stockInfo = account?.stocks[selectedStock.code];
    const holding = stockInfo
        ? {
            availableQty: stockInfo.availableQuantity,
            avgBuyPrice: stockInfo.quantity > 0 ? Math.round(stockInfo.totalAmount / stockInfo.quantity) : 0,
        }
        : undefined;

    /* [수정] 종목이 바뀐 직후, 가격(price)/감시가(watchPrice)를 "딱 한 번"만 세팅한다.
     * 기존 디자인은 StockContext.detail.currentPrice(실제로는 존재하지 않는 필드)를 사용했으나,
     * 실제 StockContext엔 현재가 정보가 없으므로 호가창(orderbook) 최초 수신 시점의
     * 기준가(매수: asks[0], 매도: bids[0])로 대체한다.
     * ⚠️⚠️⚠️ 임시 로직입니다 — 향후 종목 상세 조회 API(getStockDetailExtra 등)로 현재가를
     * 직접 조회해 세팅하는 방식으로 교체하는 것을 권장합니다. ⚠️⚠️⚠️ */
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

    /* 시장가 기준가(marketPrice) — 실시간 호가창(orderbook)에서 계산 */
    const marketPrice = isBuy
        ? (orderbook?.asks && orderbook.asks.length > 0
            ? orderbook.asks[0].price
            : 0)
        : (orderbook?.bids && orderbook.bids.length > 0
            ? orderbook.bids[0].price
            : 0);

    const effectivePrice = orderType === "market" ? marketPrice : price;
    const estimatedAmount = quantity * effectivePrice;

    /* ─── 매도 전용: 예상손익 계산 ─── */
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

    // [수정] StockInfo엔 group 필드가 없음 → realtimeSupported로 모의투자 지원 여부 판단
    if (!selectedStock.realtimeSupported) {
        return (
            <div className="trade-form trade-form--unsupported">
                <p className="trade-form__unsupported-msg">
                    모의투자를 지원하지 않는 종목입니다
                </p>
            </div>
        );
    }

    /* ─── 비율 버튼(10% / 25% / 50% / 75% / 전체) 클릭 핸들러 ─── */
    function handleRatioClick(ratio: number) {
        if (isBuy) {
            if (effectivePrice <= 0) {
                setQuantity(0);
                return;
            }
            /* [수정] summary?.orderableAmount → account?.availableCash */
            const budgetAmount = (account?.availableCash ?? 0) * (isCredit ? leverage : 1);
            const affordableQty = Math.floor((budgetAmount * ratio) / effectivePrice);
            setQuantity(clampQuantity(affordableQty));
        } else {
            const availableQty = holding?.availableQty ?? 0;
            setQuantity(clampQuantity(Math.floor(availableQty * ratio)));
        }
    }

    /* ─── handleOrder — 버튼 클릭 시점의 값을 confirmInfo 스냅샷으로 고정한 뒤 모달을 연다 ─── */
    function handleOrder() {
        const orderPrice = orderType === "market" ? marketPrice : price;

        const displayAmount = (isBuy && isCredit)
            ? Math.floor(estimatedAmount / leverage)
            : estimatedAmount;

        setConfirmInfo({
            side:            mode,
            orderType,
            // [수정] detail.stockName(존재하지 않음) → selectedStock.name (StockInfo.name은 필수 필드로 확인됨)
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

    /* [수정] handleConfirm 삭제 — 실제 주문 API 호출은 OrderConfirmModal 내부에서 처리.
     * 여기서는 모달이 성공적으로 처리를 마쳤을 때 폼 상태만 정리한다. */
    function handleConfirmSuccess() {
        setQuantity(0);
        setConfirmInfo(null);
    }

    /* ─── 신용 배수 적용 표시값 계산 ─── */
    const orderableAmountRaw = account?.availableCash ?? 0; // [수정]
    const estimatedAmountRaw = estimatedAmount;
    const estimatedAmountAfter = (isBuy && isCredit)
        ? Math.floor(estimatedAmountRaw / leverage)
        : estimatedAmountRaw;
    const showEstimatedBeforeAfter = isBuy && isCredit;

    return (
        <div className="trade-form">
            {/* 주문구분 */}
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

            {/* 비율 버튼 */}
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

            {/* 수량 */}
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

            {/* 가격 — 지정가/조건부 */}
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

            {/* 가격 — 시장가: 호가창 기준가 읽기 전용 표시 */}
            <div className={`conditional-wrapper ${orderType === "market" ? "conditional-wrapper--visible" : ""}`}>
                <div className="form-row">
                    <span className="form-label">예상 가격</span>
                    <div className="stepper stepper--readonly">
                        <button className="stepper__btn" disabled aria-hidden="true">−</button>   {/* [수정] ghost span → 실제 button(disabled)으로 교체 — 수량 스테퍼와 렌더링 폭을 완전히 동일하게 맞춰 정렬 어긋남 해결 */}
                        <input
                            className="stepper__input stepper__input--wide stepper__input--readonly"
                            type="text"
                            readOnly
                            tabIndex={-1}
                            value={marketPrice > 0 ? formatNumber(marketPrice) : "—"}
                        />
                        <button className="stepper__btn" disabled aria-hidden="true">+</button>   {/* [수정] ghost span → 실제 button(disabled)으로 교체 */}
                        <span className="stepper__unit">원</span>
                    </div>
                </div>
            </div>

            {/* 감시가 — 조건부일 때만 표시 */}
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

            {/* 주문 가능 / 예상금액 / (매도) 예상손익 */}
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

            {/* 주문 버튼 행 */}
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
                        <span className="credit-area__margin-label">유지증거금률</span>
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

            {/* 주문 확인 모달 */}
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

/* ─── 취소 탭 ─── */
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

    /* [수정] handleConfirm 삭제 — 실제 취소 API 호출은 CancelConfirmModal 내부에서 처리 */
    function handleConfirmSuccess() {
        setSelectedIds(new Set());
    }

    const hasSelection = selectedIds.size > 0;
    // [신규] 선택된 id에 해당하는 실제 주문 객체 목록 — CancelConfirmModal에 그대로 전달
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
