import type { AccountResponse, MarginStatus, AccountStatus } from "../../types/account";
import { stockNameMap } from "../data/stocks";
import { calculateBreakevenAmount, calculateSellCost } from "../../utils/fee";
import "./MyAccountModal.css";

export const fmt = (n: number) => n.toLocaleString("ko-KR");
export const fmtSigned = (n: number) => (n > 0 ? `+${fmt(n)}` : `${fmt(n)}`);
export const fmtRate = (n: number) => (n > 0 ? `+${n.toFixed(2)}%` : `${n.toFixed(2)}%`);

const MARGIN_STATUS_LABEL: Record<MarginStatus, string> = {
    NORMAL: "정상",
    MARGIN_CALL: "마진콜",
    LIQUIDATION_PENDING: "청산 대기",
};

const MARGIN_STATUS_CLASS: Record<MarginStatus, string> = {
    NORMAL: "mam-margin-badge--normal",
    MARGIN_CALL: "mam-margin-badge--call",
    LIQUIDATION_PENDING: "mam-margin-badge--risk",
};

const ACCOUNT_STATUS_LABEL: Record<AccountStatus, string> = {
    NORMAL: "정상",
    NEGATIVE: "마이너스",
    SUSPENDED: "거래 정지",
};

const ACCOUNT_STATUS_CLASS: Record<AccountStatus, string> = {
    NORMAL: "mam-margin-badge--normal",
    NEGATIVE: "mam-margin-badge--call",
    SUSPENDED: "mam-margin-badge--risk",
};

const LEVERAGE_LABEL: Record<string, string> = {
    SPOT: "현금",
    X1_5: "1.5x",
    X2: "2x",
    X2_5: "2.5x",
};

const LEVERAGE_BADGE_CLASS: Record<string, string> = {
    SPOT: "mam-leverage-badge--cash",
    X1_5: "mam-leverage-badge--lev2",
    X2: "mam-leverage-badge--lev3",
    X2_5: "mam-leverage-badge--lev5",
};

const signClass = (n: number, zero = "zero") => (n > 0 ? "positive" : n < 0 ? "negative" : zero);
const cardClass = (n: number) =>
    n > 0 ? "mam-cumulative--positive" : n < 0 ? "mam-cumulative--negative" : "mam-cumulative--neutral";

function StockIcon({ code }: { code: string }) {
    return (
        <img
            className="mam-holding-icon"
            src={`https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${code}.svg`}
            alt=""
            onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.visibility = "hidden";
            }}
        />
    );
}

interface Props {
    account: AccountResponse;
}

/** 계좌 요약 (누적/평가 손익, 자산 그리드, 계좌·마진 상태) */
export function AccountSummary({ account }: Props) {
    return (
        <>
            <div className="mam-top-row">
                <div className="mam-summary-cards">
                    <div className={`mam-cumulative ${cardClass(account.accumulatedProfit)}`}>
                        <span className="mam-cumulative__label">누적 손익</span>
                        <span className={`mam-cumulative__value ${signClass(account.accumulatedProfit, "neutral")}`}>
                            {fmtSigned(account.accumulatedProfit)} 원
                        </span>
                        <span className={`mam-cumulative__rate ${signClass(account.accumulatedProfitRate, "neutral")}`}>
                            {fmtRate(account.accumulatedProfitRate)}
                        </span>
                    </div>

                    <div className={`mam-cumulative ${cardClass(account.totalProfit)}`}>
                        <span className="mam-cumulative__label">평가 손익</span>
                        <span className={`mam-cumulative__value ${signClass(account.totalProfit, "neutral")}`}>
                            {fmtSigned(account.totalProfit)} 원
                        </span>
                        <span className={`mam-cumulative__rate ${signClass(account.totalProfitRate, "neutral")}`}>
                            {fmtRate(account.totalProfitRate)}
                        </span>
                    </div>
                </div>

                <div className="mam-grid">
                    <div className="mam-cell">
                        <span className="mam-cell__label">총 자산</span>
                        <span className="mam-cell__value">{fmt(account.totalValue)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">예수금</span>
                        <span className="mam-cell__value">{fmt(account.totalCash)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">주문 가능</span>
                        <span className="mam-cell__value">{fmt(account.availableCash)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">예약 (미체결)</span>
                        <span className="mam-cell__value">{fmt(account.reservedCash)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">총 매입 (현물)</span>
                        <span className="mam-cell__value">{fmt(account.buyValue)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">총 평가 (현물)</span>
                        <span className="mam-cell__value">{fmt(account.stockValue)} 원</span>
                    </div>
                </div>
            </div>

            <div className="mam-margin-panel">
                <div className="mam-margin-panel__head">
                    <p className="mam-section-title mam-section-title--inline" style={{ opacity: 0.5, color: "#fff" }}>
                        계좌 상태
                    </p>
                </div>
                <div className="mam-margin-grid">
                    <div className="mam-cell">
                        <span className="mam-cell__label">계좌 상태</span>
                        <span className={`mam-margin-badge ${ACCOUNT_STATUS_CLASS[account.accountStatus]}`}>
                            {ACCOUNT_STATUS_LABEL[account.accountStatus]}
                        </span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">마진 상태</span>
                        <span className={`mam-margin-badge ${MARGIN_STATUS_CLASS[account.marginStatus]}`}>
                            {MARGIN_STATUS_LABEL[account.marginStatus]}
                        </span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">레버리지 매입</span>
                        <span className="mam-cell__value">{fmt(account.leveragePurchaseTotal ?? 0)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">레버리지 평가</span>
                        <span className="mam-cell__value">
                            {fmt((account.leverageNetValue ?? 0) + (account.leverageLoanTotal ?? 0))} 원
                        </span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">레버리지 대출</span>
                        <span className="mam-cell__value">{fmt(account.leverageLoanTotal ?? 0)} 원</span>
                    </div>
                    <div className="mam-cell">
                        <span className="mam-cell__label">레버리지 순자산</span>
                        <span className="mam-cell__value">{fmt(account.leverageNetValue ?? 0)} 원</span>
                    </div>
                </div>
            </div>
        </>
    );
}

/** 보유주식 (현물) 테이블 */
export function HoldingsTable({ account }: Props) {
    const holdings = Object.entries(account.stocks);
    if (holdings.length === 0) return <div className="mam-empty">보유 중인 주식이 없습니다</div>;

    return (
        <div className="mam-holdings-body">
            <div className="mam-holding-header">
                <span>종목</span>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">매입가</span>
                    <span className="mam-holding-cell__bottom">현재가</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">보유수량</span>
                    <span className="mam-holding-cell__bottom">가능수량</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">평가손익</span>
                    <span className="mam-holding-cell__bottom">수익률</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">매입금액</span>
                    <span className="mam-holding-cell__bottom">평가금액</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">매입원금액</span>
                    <span className="mam-holding-cell__bottom">손익분기금액</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">손익분기매입가</span>
                    <span className="mam-holding-cell__bottom">수수료+세금</span>
                </div>
            </div>

            {holdings.map(([code, info]) => {
                const curPrice = account.currentPrices[code];
                const profitAmount = account.profitAmounts[code] ?? 0;
                const profitRate = account.profitRates[code] ?? 0;
                const avgBuyPrice = info.quantity > 0 ? Math.round(info.totalAmount / info.quantity) : 0;
                const evalAmount = curPrice != null ? curPrice * info.quantity : undefined;

                const breakevenAmount = calculateBreakevenAmount(info.totalCostAmount);
                const breakevenPrice = info.quantity > 0 ? Math.round(breakevenAmount / info.quantity) : 0;
                const sellCostNow = evalAmount != null ? calculateSellCost(evalAmount) : undefined;

                return (
                    <div key={code} className="mam-holding-row">
                        <div className="mam-holding-row__main mam-holding-row__main--spot">
                            <div className="mam-holding-name">
                                <StockIcon code={code} />
                                <span>{stockNameMap[code] ?? code}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(avgBuyPrice)}</span>
                                <span className="mam-holding-cell__bottom">{curPrice != null ? fmt(curPrice) : "-"}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{info.quantity}</span>
                                <span className="mam-holding-cell__bottom">{info.availableQuantity}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className={`mam-holding-cell__top ${signClass(profitAmount)}`}>
                                    {fmtSigned(profitAmount)}
                                </span>
                                <span className={`mam-holding-cell__bottom ${signClass(profitAmount)}`}>
                                    {fmtRate(profitRate)}
                                </span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(info.totalAmount)}</span>
                                <span className="mam-holding-cell__bottom">{evalAmount != null ? fmt(evalAmount) : "-"}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(info.totalCostAmount)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(breakevenAmount)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(breakevenPrice)}</span>
                                <span className="mam-holding-cell__bottom">{sellCostNow != null ? fmt(sellCostNow) : "-"}</span>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}

/** 레버리지 포지션 테이블 */
export function LeverageTable({ account }: Props) {
    const leveragePositions = account.leveragePositions ?? [];
    if (leveragePositions.length === 0) return <div className="mam-empty">보유 중인 레버리지 포지션이 없습니다</div>;

    return (
        <div className="mam-holdings-body">
            <div className="mam-holding-header mam-holding-header--leverage">
                <span>종목</span>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">매입가</span>
                    <span className="mam-holding-cell__bottom">현재가</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">보유수량</span>
                    <span className="mam-holding-cell__bottom">가능수량</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">평가손익</span>
                    <span className="mam-holding-cell__bottom">수익률</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">매입금액</span>
                    <span className="mam-holding-cell__bottom">평가금액</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">매입원금액</span>
                    <span className="mam-holding-cell__bottom">손익분기금액</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">손익분기매입가</span>
                    <span className="mam-holding-cell__bottom">수수료+세금</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">순자산</span>
                    <span className="mam-holding-cell__bottom">대출금</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">개시증거금</span>
                    <span className="mam-holding-cell__bottom">유지증거금</span>
                </div>
                <div className="mam-holding-cell">
                    <span className="mam-holding-cell__top">마진상태</span>
                    <span className="mam-holding-cell__bottom">유지증거금 기준가</span>
                </div>
            </div>

            {leveragePositions.map((pos) => {
                const avgBuyPrice = pos.quantity > 0 ? Math.round(pos.purchaseAmount / pos.quantity) : 0;
                const breakevenAmount = calculateBreakevenAmount(pos.costAmount);
                const breakevenPrice = pos.quantity > 0 ? Math.round(breakevenAmount / pos.quantity) : 0;
                const sellCostNow = calculateSellCost(pos.evaluationAmount);

                return (
                    <div key={`${pos.stockCode}-${pos.leverageRatio}`} className="mam-holding-row">
                        <div className="mam-holding-row__main mam-holding-row__main--leverage">
                            <div className="mam-holding-name">
                                <StockIcon code={pos.stockCode} />
                                <span>{stockNameMap[pos.stockCode] ?? pos.stockCode}</span>
                                <span className={`mam-leverage-badge ${LEVERAGE_BADGE_CLASS[pos.leverageRatio] ?? "mam-leverage-badge--levother"}`}>
                                    {LEVERAGE_LABEL[pos.leverageRatio] ?? pos.leverageRatio}
                                </span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(avgBuyPrice)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(pos.currentPrice)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{pos.quantity}</span>
                                <span className="mam-holding-cell__bottom">{pos.availableQuantity}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className={`mam-holding-cell__top ${signClass(pos.profitAmount)}`}>
                                    {fmtSigned(pos.profitAmount)}
                                </span>
                                <span className={`mam-holding-cell__bottom ${signClass(pos.profitAmount)}`}>
                                    {fmtRate(pos.profitRate)}
                                </span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(pos.purchaseAmount)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(pos.evaluationAmount)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(pos.costAmount)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(breakevenAmount)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(breakevenPrice)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(sellCostNow)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(pos.netValue)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(pos.loanAmount)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className="mam-holding-cell__top">{fmt(pos.initialMargin)}</span>
                                <span className="mam-holding-cell__bottom">{fmt(pos.maintenanceMargin)}</span>
                            </div>
                            <div className="mam-holding-cell">
                                <span className={`mam-margin-badge mam-margin-badge--sm ${MARGIN_STATUS_CLASS[pos.marginStatus]}`}>
                                    {MARGIN_STATUS_LABEL[pos.marginStatus]}
                                </span>
                                <span className="mam-holding-cell__bottom">{fmt(pos.maintenancePrice)}</span>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
}
