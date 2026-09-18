import ModalV2 from "../../../../components/ModalV2";
import "./TrailingOrderConfirmModal.css";

export interface TrailingConfirmData {
    stockName:   string;
    stockCode:   string;
    side:        "BUY" | "SELL";
    credit:      boolean;
    leverage:    number;
    quantity:    number;
    stopPercent: number;
    basePrice:   number;
}

interface TrailingOrderConfirmModalProps {
    open:      boolean;
    data:      TrailingConfirmData | null;
    onClose:   () => void;
    onConfirm: () => void;
}

function fmt(n: number): string {
    return n.toLocaleString();
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
        <span className={`toc__leverage-tag ${levClass}`}>
            {isCash ? "현금" : `${leverage}x`}
        </span>
    );
}

export default function TrailingOrderConfirmModal({
                                                      open, data, onClose, onConfirm,
                                                  }: TrailingOrderConfirmModalProps) {
    if (!data) return null;

    const isBuy       = data.side === "BUY";
    const sideLabel   = isBuy ? "매수" : "매도";
    const creditLabel = data.credit ? "신용" : "현금";

    const expectedFill = isBuy
        ? Math.round(data.basePrice * (1 + data.stopPercent / 100))
        : Math.round(data.basePrice * (1 - data.stopPercent / 100));

    const rawAmount   = expectedFill * data.quantity;
    const afterAmount = (isBuy && data.credit)
        ? Math.floor(rawAmount / data.leverage)
        : rawAmount;

    const showBeforeAfter = isBuy && data.credit;

    const handleConfirm = () => { onConfirm(); onClose(); };

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
                        <LeverageTag leverage={data.credit ? data.leverage : 1} />
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
                        <span className="toc__row-label">기준가</span>
                        <span className="toc__row-value">{fmt(data.basePrice)} 원</span>
                    </div>

                    <div className="toc__row">
                        <span className="toc__row-label">{sideLabel} 스탑</span>
                        <span className={`toc__row-value toc__stop--${isBuy ? "buy" : "sell"}`}>
                            {data.stopPercent.toFixed(1)}%
                        </span>
                    </div>

                    <div className="toc__divider" />

                    <div className="toc__row">
                        <span className="toc__row-label">
                            {isBuy ? "예상 최대 체결가" : "예상 최소 체결가"}
                            <span className="toc__row-label-hint">(기준가 기준)</span>
                        </span>
                        <span className={`toc__row-value toc__stop--${isBuy ? "buy" : "sell"}`}>
                            {fmt(expectedFill)} 원
                        </span>
                    </div>

                    <div className="toc__row">
                        <span className="toc__row-label">
                            {isBuy ? "예상 최대 금액" : "예상 최소 금액"}
                        </span>
                        <span className={`toc__row-value toc__row-value--amount ${isBuy ? "buy" : "sell"}`}>
                            {showBeforeAfter ? (
                                <>
                                    <span className="toc__value--after">{fmt(afterAmount)}원</span>
                                    <span className="toc__value--before">{fmt(rawAmount)}원</span>
                                </>
                            ) : (
                                `${fmt(afterAmount)} 원`
                            )}
                        </span>
                    </div>

                </div>

                <p className="toc__notice">
                    {isBuy
                        ? `최저가 대비 ${data.stopPercent.toFixed(1)}% 반등 시 자동 매수됩니다.`
                        : `최고가 대비 ${data.stopPercent.toFixed(1)}% 하락 시 자동 매도됩니다.`
                    }
                    <br />체결가는 실제 체결 시점에 따라 달라질 수 있습니다.
                </p>

                <div className="toc__footer">
                    <button
                        className={`toc__btn toc__btn--confirm-${isBuy ? "buy" : "sell"}`}
                        onClick={handleConfirm}
                    >
                        {sideLabel} 확정
                    </button>
                    <button className="toc__btn toc__btn--cancel" onClick={onClose}>
                        취소
                    </button>
                </div>

            </div>
        </ModalV2>
    );
}
