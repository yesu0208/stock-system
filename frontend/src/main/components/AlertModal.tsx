import "./AlertModal.css";

import { useState, useRef, useEffect } from "react";

import ModalV2 from "../../components/ModalV2";
import { useStock } from "../context/StockContext";
import { useAlert } from "../context/AlertContext";
import type { AlertDirection } from "../../types/alert";
import { STOCKS } from "../data/stocks";

type AlertTab = "current" | "all";

interface AlertModalProps {
    open: boolean;
    onClose: () => void;
}

function getStockName(code: string): string {
    return STOCKS.find((s) => s.code === code)?.name ?? code;
}

export default function AlertModal({ open, onClose }: AlertModalProps) {
    const { selectedStock } = useStock();
    const { alerts, registerAlert, cancelAlert, getAlertsBySymbol } = useAlert();

    const [tab, setTab] = useState<AlertTab>("current");
    const [price, setPrice] = useState<string>("");
    const [direction, setDirection] = useState<AlertDirection>("ABOVE");
    const [loading, setLoading] = useState(false);
    const [priceError, setPriceError] = useState<string | null>(null);

    const errorInnerRef = useRef<HTMLParagraphElement>(null);
    const [errorHeight, setErrorHeight] = useState(0);

    useEffect(() => {
        if (priceError && errorInnerRef.current) {
            setErrorHeight(errorInnerRef.current.scrollHeight);
        } else {
            setErrorHeight(0);
        }
    }, [priceError]);

    const handleRegister = async () => {
        const priceNum = parseInt(price.replace(/,/g, ""), 10);

        if (!price || isNaN(priceNum) || priceNum <= 0) {
            setPriceError("유효한 가격을 입력해 주세요.");
            return;
        }

        setPriceError(null);
        setLoading(true);

        const result = await registerAlert(selectedStock.code, selectedStock.name, priceNum, direction); // [수정]

        if (result.success) {
            setPrice("");
        }

        setLoading(false);
    };

    const handleCancel = async (alertId: number, stockCode: string, stockName: string) => { // [수정]
        await cancelAlert(alertId, stockCode, stockName); // [수정]
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") handleRegister();
    };

    const handlePriceChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const raw = e.target.value.replace(/[^0-9]/g, "");
        if (priceError) setPriceError(null);
        if (raw === "") {
            setPrice("");
            return;
        }
        setPrice(Number(raw).toLocaleString("ko-KR"));
    };

    const currentAlerts = getAlertsBySymbol(selectedStock.code);

    return (
        <ModalV2 open={open} title="알림" onClose={onClose}>
            <div className="alert-modal-body">
                <div className="alert-tabs">
                    <button
                        className={`alert-tab-btn ${tab === "current" ? "active" : ""}`}
                        onClick={() => setTab("current")}
                    >
                        현재 종목
                    </button>
                    <button
                        className={`alert-tab-btn ${tab === "all" ? "active" : ""}`}
                        onClick={() => setTab("all")}
                    >
                        전체 종목
                    </button>
                </div>

                {tab === "current" ? (
                    <div className="alert-current-wrap">
                        <section className="alert-form-section">
                            <div className="alert-symbol-badge">
                                {selectedStock.name}
                                <span className="badge-code">{selectedStock.code}</span>
                            </div>

                            <div className="alert-form">
                                <div className="alert-direction-toggle">
                                    <button
                                        className={`alert-direction-btn ${direction === "ABOVE" ? "selected-above" : ""}`}
                                        onClick={() => setDirection("ABOVE")}
                                    >
                                        ▲ 이상 (ABOVE)
                                    </button>

                                    <button
                                        className={`alert-direction-btn ${direction === "BELOW" ? "selected-below" : ""}`}
                                        onClick={() => setDirection("BELOW")}
                                    >
                                        ▼ 이하 (BELOW)
                                    </button>
                                </div>

                                <div className="alert-price-row">
                                    <input
                                        className={`alert-price-input${priceError ? " is-invalid" : ""}`}
                                        type="text"
                                        inputMode="numeric"
                                        placeholder="감시 가격 (원)"
                                        value={price}
                                        onChange={handlePriceChange}
                                        onKeyDown={handleKeyDown}
                                    />
                                    <button
                                        className="alert-submit-btn"
                                        onClick={handleRegister}
                                        disabled={loading}
                                    >
                                        {loading ? "등록 중" : "등록"}
                                    </button>
                                </div>

                                <div className="alert-price-error-wrap" style={{ height: errorHeight }}>
                                    <p className="alert-price-error" ref={errorInnerRef}>{priceError}</p>
                                </div>
                            </div>
                        </section>

                        <section className="alert-list-section">
                            <div className="alert-section-title">
                                {selectedStock.name} 알림 ({currentAlerts.length})
                            </div>

                            <div className="alert-list current">
                                {currentAlerts.length === 0 ? (
                                    <div className="alert-empty">등록된 알림이 없습니다</div>
                                ) : (
                                    currentAlerts.map((item) => (
                                        <div key={item.alertId} className="alert-item">
                                            <div className="alert-item-left">
                                                <div className="alert-item-top">
                                                    <span className={`alert-dir-chip ${item.direction === "ABOVE" ? "above" : "below"}`}>
                                                        {item.direction === "ABOVE" ? "▲ 이상" : "▼ 이하"}
                                                    </span>
                                                </div>

                                                <div className="alert-item-price">
                                                    <strong>{item.triggerPrice.toLocaleString("ko-KR")}</strong> 원
                                                </div>
                                            </div>

                                            <button
                                                className="alert-cancel-btn"
                                                onClick={() => handleCancel(item.alertId, item.stockCode, selectedStock.name)}
                                            >
                                                해제
                                            </button>
                                        </div>
                                    ))
                                )}
                            </div>
                        </section>
                    </div>
                ) : (
                    <section>
                        <div className="alert-section-title">전체 알림 ({alerts.length})</div>

                        <div className="alert-list all">
                            {alerts.length === 0 ? (
                                <div className="alert-empty">등록된 알림이 없습니다</div>
                            ) : (
                                alerts.map((item) => (
                                    <div key={item.alertId} className="alert-item">
                                        <div className="alert-item-left">
                                            <div className="alert-item-top">
                                                <span className="alert-item-symbol">{getStockName(item.stockCode)}</span>
                                                <span className="alert-item-code">{item.stockCode}</span>
                                                <span className={`alert-dir-chip ${item.direction === "ABOVE" ? "above" : "below"}`}>
                                                    {item.direction === "ABOVE" ? "▲ 이상" : "▼ 이하"}
                                                </span>
                                            </div>

                                            <div className="alert-item-price">
                                                <strong>{item.triggerPrice.toLocaleString("ko-KR")}</strong> 원
                                            </div>
                                        </div>

                                        <button
                                            className="alert-cancel-btn"
                                            onClick={() => handleCancel(item.alertId, item.stockCode, getStockName(item.stockCode))}
                                        >
                                            해제
                                        </button>
                                    </div>
                                ))
                            )}
                        </div>
                    </section>
                )}
            </div>
        </ModalV2>
    );
}
