import { useState } from "react";
import ModalV2 from "../../../../components/ModalV2";
import "./TrailingOrderConfirmModal.css";
import { useMsg } from "../../../context/MsgContext";
import Tooltip from "../../../../tooltip/Tooltip";

export interface TrailingConfirmData {
    stockName:   string;
    stockCode:   string;
    side:        "BUY" | "SELL";
    credit:      boolean;
    leverage:    number;
    quantity:    number;
    stopPercent: number;
    basePrice:   number;
    expectedFillPrice: number;
    buyFee?:          number;
    sellCost?:        number;
    sellAmountAfter?: number;
    minProfitAmount?: number;
    minProfitRate?:   number;
}

interface TrailingOrderConfirmModalProps {
    open:      boolean;
    data:      TrailingConfirmData | null;
    onClose:   () => void;
    onConfirm: () => void | Promise<void>;
}

function fmt(n: number): string {
    return n.toLocaleString();
}

function LeverageTag({ leverage, small = false }: { leverage: number; small?: boolean }) {
    const isCash = leverage === 1;
    const levClass =
        isCash           ? "cash"  :
            leverage === 1.5 ? "lev-2" :
                leverage === 2   ? "lev-3" :
                    leverage === 2.5 ? "lev-5" :
                        "lev-other";
    return (
        <span className={`toc__leverage-tag ${levClass}${small ? " toc__leverage-tag--sm" : ""}`}>
            {isCash ? "현금" : `${leverage}x`}
        </span>
    );
}

export default function TrailingOrderConfirmModal({
                                                      open, data, onClose, onConfirm,
                                                  }: TrailingOrderConfirmModalProps) {
    const [loading, setLoading] = useState(false);
    const { error } = useMsg();

    if (!data) return null;

    const isBuy       = data.side === "BUY";
    const sideLabel   = isBuy ? "매수" : "매도";
    const creditLabel = data.credit ? "신용" : "현금";
    const tagLeverage = data.credit ? data.leverage : 1;

    const rawAmount   = Math.round(data.expectedFillPrice * data.quantity);
    const afterAmount = (isBuy && data.credit)
        ? Math.floor(rawAmount / data.leverage)
        : rawAmount;

    const showBeforeAfter = isBuy && data.credit;

    const buyFee              = data.buyFee ?? 0;
    const requiredAmountAfter = afterAmount + buyFee;
    const requiredAmountRaw   = rawAmount + buyFee;

    const sellCost        = data.sellCost ?? 0;
    const sellAmountAfter = data.sellAmountAfter ?? 0;

    const showProfit  = !isBuy && data.minProfitAmount !== undefined;
    const profitAmt   = data.minProfitAmount ?? 0;
    const profitRate  = data.minProfitRate ?? 0;
    const profitSign  = profitAmt > 0 ? "+" : profitAmt < 0 ? "−" : "";
    const rateSign    = profitRate > 0 ? "+" : profitRate < 0 ? "−" : "";
    const profitClass = profitAmt > 0 ? "profit" : profitAmt < 0 ? "loss" : "";

    async function handleConfirm() {
        setLoading(true);
        try {
            await onConfirm();
            onClose();
        } catch (e: any) {
            error(e.response?.data?.message ?? e.message ?? "트레일링 스탑 등록 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <ModalV2 open={open} title="주문 확인" onClose={onClose} extraClass="toc-modal">
            <div className="toc">

                <div className="toc__header">
                    <span className={`toc__side-badge toc__side-badge--${isBuy ? "buy" : "sell"}`}>
                        {creditLabel} {sideLabel}
                    </span>

                    <div className="toc__stock-info">
                        <span className="toc__stock-name">{data.stockName}</span>
                        <span className="toc__stock-code">{data.stockCode}</span>
                    </div>

                    <div className="toc__tag-row">
                        <LeverageTag leverage={tagLeverage} />
                        <span className="toc__tag-sep">/</span>
                        <span className="toc__tag">트레일링 스탑</span>
                    </div>
                </div>

                <div className="toc__rows">

                    <div className="toc__row">
                        <span className="toc__row-label">수량</span>
                        <span className="toc__row-value">{fmt(data.quantity)} 주</span>
                    </div>

                    <div className="toc__row">
                        <span className="toc__row-label">진입가</span>
                        <span className="toc__row-value">{fmt(data.basePrice)} 원</span>
                    </div>

                    <div className="toc__row">
                        <span className="toc__row-label">{sideLabel} 스탑</span>
                        <span className={`toc__row-value toc__stop--${isBuy ? "buy" : "sell"}`}>
                            {data.stopPercent.toFixed(1)}%
                        </span>
                    </div>

                    <div className="toc__divider" />

                    <div className="toc__row toc__row--fill">
                        <span className="toc__row-label">
                            {isBuy ? "최대 체결가" : "최소 체결가"}
                            <span className="toc__row-label-hint">(진입가 기준)</span>
                        </span>
                        <span className={`toc__row-value toc__stop--${isBuy ? "buy" : "sell"}`}>
                            {fmt(data.expectedFillPrice)} 원
                        </span>
                    </div>

                    <div className="toc__row toc__row--fill toc__row--stacked">
                        <span className="toc__row-label">
                            {isBuy ? "최대 체결금액" : "최소 체결금액"}
                            <LeverageTag leverage={tagLeverage} small />
                            {!isBuy && (
                                <Tooltip
                                    text={`매도 수수료·세금(0.015%, 0.2%) ${sellCost.toLocaleString()}원 차감`}
                                    placement="top"
                                >
                                    <span className="toc__fee-tag">수수료·세금</span>
                                </Tooltip>
                            )}
                        </span>
                        <span className={`toc__row-value toc__row-value--amount ${isBuy ? "buy" : "sell"}`}>
                            {isBuy ? (
                                showBeforeAfter ? (
                                    <>
                                        <span className="toc__value--after">{fmt(afterAmount)}원</span>
                                        <span className="toc__value--before">{fmt(rawAmount)}원</span>
                                    </>
                                ) : (
                                    `${fmt(afterAmount)} 원`
                                )
                            ) : data.credit ? (
                                <>
                                    <span className="toc__value--after">{fmt(sellAmountAfter)}원</span>
                                    <span className="toc__value--before">{fmt(rawAmount)}원</span>
                                </>
                            ) : (
                                `${fmt(sellAmountAfter)} 원`
                            )}
                        </span>
                    </div>

                    {isBuy && (
                        <div className="toc__row toc__row--fill toc__row--stacked">
                            <span className="toc__row-label">
                                필요금액
                                <LeverageTag leverage={tagLeverage} small />
                                <Tooltip
                                    text={`매수 수수료(0.015%) ${buyFee.toLocaleString()}원 포함`}
                                    placement="top"
                                >
                                    <span className="toc__fee-tag">수수료</span>
                                </Tooltip>
                            </span>
                            <span className="toc__row-value toc__row-value--amount buy">
                                {showBeforeAfter ? (
                                    <>
                                        <span className="toc__value--after">{fmt(requiredAmountAfter)}원</span>
                                        <span className="toc__value--before">{fmt(requiredAmountRaw)}원</span>
                                    </>
                                ) : (
                                    `${fmt(requiredAmountAfter)} 원`
                                )}
                            </span>
                        </div>
                    )}

                    {showProfit && (
                        <div className="toc__row toc__row--stacked">
                            <span className="toc__row-label">
                                최소 예상손익
                                <LeverageTag leverage={tagLeverage} small />
                                <Tooltip
                                    text={`매도 수수료·세금(0.015%, 0.2%) ${sellCost.toLocaleString()}원 차감`}
                                    placement="top"
                                >
                                    <span className="toc__fee-tag">수수료·세금</span>
                                </Tooltip>
                            </span>
                            <span className={`toc__row-value toc__profit ${profitClass}`}>
                                {profitSign}{Math.abs(profitAmt).toLocaleString()}원
                                ({rateSign}{Math.abs(profitRate).toFixed(2)}%)
                            </span>
                        </div>
                    )}

                </div>

                <p className="toc__notice">
                    <span className={`toc__notice-main toc__stop--${isBuy ? "buy" : "sell"}`}>
                        {isBuy
                            ? `최저가 대비 ${data.stopPercent.toFixed(1)}% 반등 시 자동 매수됩니다.`
                            : `최고가 대비 ${data.stopPercent.toFixed(1)}% 하락 시 자동 매도됩니다.`
                        }
                    </span>
                    <br />체결가는 실제 체결 시점에 따라 달라질 수 있습니다.
                </p>

                <div className="toc__footer">
                    <button
                        className={`toc__btn toc__btn--confirm-${isBuy ? "buy" : "sell"}`}
                        onClick={handleConfirm}
                        disabled={loading}
                    >
                        {loading ? "처리 중..." : `${sideLabel} 확정`}
                    </button>
                    <button className="toc__btn toc__btn--cancel" onClick={onClose} disabled={loading}>
                        취소
                    </button>
                </div>

            </div>
        </ModalV2>
    );
}
