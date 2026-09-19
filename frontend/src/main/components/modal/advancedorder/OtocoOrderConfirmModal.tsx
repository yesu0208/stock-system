import { useState } from "react";
import ModalV2 from "../../../../components/ModalV2";
import "./OtocoOrderConfirmModal.css";
import { useMsg } from "../../../context/MsgContext";

export interface OtocoConfirmData {
    stockName:      string;
    stockCode:      string;
    entryDirection: "ABOVE" | "BELOW";
    triggerPrice:   number;
    quantity:       number;
    tpMode:         "PRICE" | "PCT";
    tpPrice:        number | null;
    tpPct:          number | null;
    slMode:         "PRICE" | "PCT";
    slPrice:        number | null;
    slPct:          number | null;
    credit:         boolean;
    leverage:       number;
    tpDerivedPrice: number;
    slDerivedPrice: number;
}

interface OtocoOrderConfirmModalProps {
    open:      boolean;
    data:      OtocoConfirmData | null;
    onClose:   () => void;
    onConfirm: () => void | Promise<void>;
}

function fmt(n: number): string {
    return n.toLocaleString();
}

export default function OtocoOrderConfirmModal({
                                                   open, data, onClose, onConfirm,
                                               }: OtocoOrderConfirmModalProps) {
    const [loading, setLoading] = useState(false);
    const { error } = useMsg();

    if (!data) return null;

    const isAbove = data.entryDirection === "ABOVE";

    const leverageLabel =
        !data.credit || data.leverage === 1 ? "현금" : `${data.leverage}x`;
    const leverageClass =
        !data.credit || data.leverage === 1 ? "cash"  :
            data.leverage === 1.5 ? "lev-2" :
                data.leverage === 2   ? "lev-3" :
                    data.leverage === 2.5 ? "lev-5" :
                        "lev-other";

    const tpMain = data.tpMode === "PRICE"
        ? `${fmt(data.tpPrice ?? 0)}원`
        : `+${(data.tpPct ?? 0).toFixed(1)}%`;

    const slMain = data.slMode === "PRICE"
        ? `${fmt(data.slPrice ?? 0)}원`
        : `-${(data.slPct ?? 0).toFixed(1)}%`;

    const rawAmount   = data.triggerPrice * data.quantity;
    const afterAmount = data.credit
        ? Math.floor(rawAmount / data.leverage)
        : rawAmount;
    const showBeforeAfter = data.credit;

    async function handleConfirm() {
        setLoading(true);
        try {
            await onConfirm();
            onClose();
        } catch (e: any) {
            error(e.response?.data?.message ?? e.message ?? "OTOCO 주문 등록 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <ModalV2 open={open} title="주문 확인" onClose={onClose} extraClass="occ-modal">
            <div className="occ">

                <div className="occ__header">
                    <span className="occ__side-badge">
                        {data.credit ? "신용" : "현금"}매수
                    </span>
                    <div className="occ__stock-info">
                        <span className="occ__stock-name">{data.stockName}</span>
                        <span className="occ__stock-code">{data.stockCode}</span>
                    </div>
                    <div className="occ__tag-row">
                        <span className={`occ__tag occ__tag--leverage ${leverageClass}`}>
                            {leverageLabel}
                        </span>
                        <span className="occ__tag-sep">/</span>
                        <span className="occ__tag occ__tag--type">OTOCO</span>
                        <span className="occ__tag-sep">/</span>
                        <span className={`occ__tag ${isAbove ? "occ__tag--above" : "occ__tag--below"}`}>
                            {isAbove ? "이상↑" : "이하↓"} 진입
                        </span>
                    </div>
                </div>

                <div className="occ__section">
                    <span className="occ__section-title">진입 조건</span>
                    <div className="occ__table">

                        <div className="occ__row">
                            <span className="occ__label">트리거가</span>
                            <span className={`occ__value ${isAbove ? "occ__value--above" : "occ__value--below"}`}>
                                {fmt(data.triggerPrice)}
                                <span className="occ__unit">원</span>
                                <span className={`occ__trigger-dir ${isAbove ? "occ__value--above" : "occ__value--below"}`}>
                                    {isAbove ? "이상" : "이하"}
                                </span>
                            </span>
                        </div>

                        <div className="occ__row">
                            <span className="occ__label">수량</span>
                            <span className="occ__value">
                                {fmt(data.quantity)}
                                <span className="occ__unit">주</span>
                            </span>
                        </div>

                        <div className="occ__row">
                            <span className="occ__label">
                                진입 금액
                            </span>
                            <span className="occ__summary-value">
                                {showBeforeAfter ? (
                                    <>
                                        <span className="occ__value--after">{fmt(afterAmount)}원</span>
                                        <span className="occ__value--before">{fmt(rawAmount)}원</span>
                                    </>
                                ) : (
                                    <span className="occ__value">{fmt(afterAmount)}<span className="occ__unit">원</span></span>
                                )}
                            </span>
                        </div>

                    </div>
                </div>

                <div className="occ__section">
                    <span className="occ__section-title">청산 조건</span>
                    <div className="occ__table">

                        <div className="occ__row">
                            <span className="occ__label occ__label--tp">익절</span>
                            <div className="occ__exit-value">
                                <span className="occ__value occ__value--tp">{tpMain}</span>
                                {data.tpMode === "PCT" && (
                                    <span className="occ__derived occ__derived--tp">≈ {fmt(data.tpDerivedPrice)}원</span>
                                )}
                            </div>
                        </div>

                        <div className="occ__row">
                            <span className="occ__label occ__label--sl">손절</span>
                            <div className="occ__exit-value">
                                <span className="occ__value occ__value--sl">{slMain}</span>
                                {data.slMode === "PCT" && (
                                    <span className="occ__derived occ__derived--sl">≈ {fmt(data.slDerivedPrice)}원</span>
                                )}
                            </div>
                        </div>

                    </div>
                </div>

                <p className="occ__notice">
                    {isAbove
                        ? "현재가가 트리거가 이상이 되면 매수 진입하고,"
                        : "현재가가 트리거가 이하가 되면 매수 진입하고,"
                    }
                    <br />진입 즉시 익절·손절 OCO 주문이 자동 등록됩니다.
                    <br />익절·손절 중 하나가 체결되면 나머지는 자동 취소됩니다.
                </p>

                <div className="occ__btn-row">
                    <button
                        className="occ__btn occ__btn--confirm"
                        onClick={handleConfirm}
                        disabled={loading}
                    >
                        {loading ? "처리 중..." : "매수 확정"}
                    </button>
                    <button className="occ__btn occ__btn--cancel" onClick={onClose} disabled={loading}>
                        취소
                    </button>
                </div>

            </div>
        </ModalV2>
    );
}
