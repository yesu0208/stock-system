import { useEffect, useRef, useState } from "react";
import ModalV2 from "../../components/ModalV2";
import { useAccount } from "../context/AccountContext";
import { getDailyReturnHistory } from "../../api/dailyReturn";
import type { DailyReturnHistoryItem } from "../../types/dailyReturn";
import type { MarginStatus, AccountStatus } from "../../types/account";
import { stockNameMap } from "../../constants/stocks";
import "./MyAccountModal.css";
import Spinner from "../../components/Spinner";

interface Props {
    open: boolean;
    onClose: () => void;
}

type Tab = "account" | "holdings" | "leverage" | "profit";

const fmt = (n: number) => n.toLocaleString("ko-KR");
const fmtSigned = (n: number) => (n > 0 ? `+${fmt(n)}` : `${fmt(n)}`);
const fmtRate = (n: number) => (n > 0 ? `+${n.toFixed(2)}%` : `${n.toFixed(2)}%`);

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

export default function MyAccountModal({ open, onClose }: Props) {
    const [tab, setTab] = useState<Tab>("account");
    const { account } = useAccount();

    // 수익률 탭
    const [profitItems, setProfitItems] = useState<DailyReturnHistoryItem[]>([]);
    const [profitHasNext, setProfitHasNext] = useState(true);
    const [profitLoading, setProfitLoading] = useState(false);
    const profitPageRef = useRef(0);
    const loadingRef = useRef(false);
    const scrollRef = useRef<HTMLDivElement>(null);

    const fetchProfitPage = (page: number) => {
        if (loadingRef.current) return;
        loadingRef.current = true;
        setProfitLoading(true);
        getDailyReturnHistory(page, 20)
            .then((data) => {
                setProfitItems((prev) => (page === 0 ? data.items : [...prev, ...data.items]));
                setProfitHasNext(data.hasNext);
                profitPageRef.current = page;
            })
            .catch(console.error)
            .finally(() => {
                loadingRef.current = false;
                setProfitLoading(false);
            });
    };

    useEffect(() => {
        if (!open) return;
        setTab("account");
        setProfitItems([]);
        setProfitHasNext(true);
        profitPageRef.current = 0;
    }, [open]);

    useEffect(() => {
        if (tab === "profit" && profitItems.length === 0) {
            fetchProfitPage(0);
        }
    }, [tab]);

    useEffect(() => {
        if (tab !== "profit") return;
        const el = scrollRef.current;
        if (!el) return;

        const handleScroll = () => {
            if (
                el.scrollHeight - el.scrollTop - el.clientHeight < 80 &&
                profitHasNext &&
                !loadingRef.current
            ) {
                fetchProfitPage(profitPageRef.current + 1);
            }
        };

        el.addEventListener("scroll", handleScroll);
        return () => el.removeEventListener("scroll", handleScroll);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tab, profitHasNext]);

    const holdings = account ? Object.entries(account.stocks) : [];
    const leveragePositions = account?.leveragePositions ?? [];

    return (
        <ModalV2 open={open} title="내 계좌" onClose={onClose}>
            <div className="mam-tabs">
                <button className={`mam-tab ${tab === "account" ? "mam-tab--active" : ""}`} onClick={() => setTab("account")}>계좌</button>
                <button className={`mam-tab ${tab === "holdings" ? "mam-tab--active" : ""}`} onClick={() => setTab("holdings")}>보유주식</button>
                <button className={`mam-tab ${tab === "leverage" ? "mam-tab--active" : ""}`} onClick={() => setTab("leverage")}>레버리지</button>
                <button className={`mam-tab ${tab === "profit" ? "mam-tab--active" : ""}`} onClick={() => setTab("profit")}>수익률</button>
            </div>

            {/* ════ 계좌 탭 ════ */}
            {tab === "account" && (
                <div className="mam-account mam-tab-content" key="account">
                    {account ? (
                        <>
                            <div className="mam-top-row">
                                <div className="mam-summary-cards">
                                    <div className={`mam-cumulative ${
                                        account.accumulatedProfit > 0 ? "mam-cumulative--positive" :
                                            account.accumulatedProfit < 0 ? "mam-cumulative--negative" : "mam-cumulative--neutral"
                                    }`}>
                                        <span className="mam-cumulative__label">누적 손익</span>
                                        <span className={`mam-cumulative__value ${
                                            account.accumulatedProfit > 0 ? "positive" :
                                                account.accumulatedProfit < 0 ? "negative" : "neutral"
                                        }`}>
                                            {fmtSigned(account.accumulatedProfit)} 원
                                        </span>
                                        <span className={`mam-cumulative__rate ${
                                            account.accumulatedProfitRate > 0 ? "positive" :
                                                account.accumulatedProfitRate < 0 ? "negative" : "neutral"
                                        }`}>
                                            {fmtRate(account.accumulatedProfitRate)}
                                        </span>
                                    </div>

                                    <div className={`mam-cumulative ${
                                        account.totalProfit > 0 ? "mam-cumulative--positive" :
                                            account.totalProfit < 0 ? "mam-cumulative--negative" : "mam-cumulative--neutral"
                                    }`}>
                                        <span className="mam-cumulative__label">평가 손익</span>
                                        <span className={`mam-cumulative__value ${
                                            account.totalProfit > 0 ? "positive" :
                                                account.totalProfit < 0 ? "negative" : "neutral"
                                        }`}>
                                            {fmtSigned(account.totalProfit)} 원
                                        </span>
                                        <span className={`mam-cumulative__rate ${
                                            account.totalProfitRate > 0 ? "positive" :
                                                account.totalProfitRate < 0 ? "negative" : "neutral"
                                        }`}>
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
                                    <div className="mam-cell mam-cell--pending">
                                        <span className="mam-cell__label">예약(미체결)</span>
                                        <span className="mam-cell__value">{fmt(account.reservedCash)} 원</span>
                                    </div>
                                    <div className="mam-cell">
                                        <span className="mam-cell__label">총 매입</span>
                                        <span className="mam-cell__value">{fmt(account.buyValue)} 원</span>
                                    </div>
                                    <div className="mam-cell">
                                        <span className="mam-cell__label">총 평가</span>
                                        <span className="mam-cell__value">{fmt(account.stockValue)} 원</span>
                                    </div>
                                </div>
                            </div>

                            {/* ── 계좌/마진 상태 패널 (실제 존재하는 필드만) ── */}
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
                                        <span className="mam-cell__label">레버리지 순자산</span>
                                        <span className="mam-cell__value">{fmt(account.leverageNetValue ?? 0)} 원</span>
                                    </div>
                                    <div className="mam-cell">
                                        <span className="mam-cell__label">레버리지 대출금</span>
                                        <span className="mam-cell__value">{fmt(account.leverageLoanTotal ?? 0)} 원</span>
                                    </div>
                                </div>
                            </div>
                        </>
                    ) : (
                        <div className="mam-loading"><Spinner /></div>
                    )}
                </div>
            )}

            {/* ════ 보유주식 탭 (현물 전용) ════ */}
            {tab === "holdings" && (
                <div className="mam-holdings mam-holdings--tab mam-tab-content" key="holdings">
                    {account ? (
                        holdings.length > 0 ? (
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
                                </div>

                                {holdings.map(([code, info]) => {
                                    const curPrice = account.currentPrices[code];
                                    const profitAmount = account.profitAmounts[code] ?? 0;
                                    const profitRate = account.profitRates[code] ?? 0;
                                    const avgBuyPrice = info.quantity > 0 ? Math.round(info.totalAmount / info.quantity) : 0;
                                    const evalAmount = curPrice != null ? curPrice * info.quantity : undefined;

                                    return (
                                        <div key={code} className="mam-holding-row">
                                            <div className="mam-holding-row__main mam-holding-row__main--spot">
                                                <div className="mam-holding-name">
                                                    <img
                                                        className="mam-holding-icon"
                                                        src={`https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${code}.svg`}
                                                        alt=""
                                                        onError={(e) => {
                                                            (e.currentTarget as HTMLImageElement).style.visibility = "hidden";
                                                        }}
                                                    />
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
                                                    <span className={`mam-holding-cell__top ${profitAmount >= 0 ? "positive" : "negative"}`}>
                                                        {fmtSigned(profitAmount)}
                                                    </span>
                                                    <span className={`mam-holding-cell__bottom ${profitRate >= 0 ? "positive" : "negative"}`}>
                                                        {fmtRate(profitRate)}
                                                    </span>
                                                </div>
                                                <div className="mam-holding-cell">
                                                    <span className="mam-holding-cell__top">{fmt(info.totalAmount)}</span>
                                                    <span className="mam-holding-cell__bottom">{evalAmount != null ? fmt(evalAmount) : "-"}</span>
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <div className="mam-empty">보유 중인 주식이 없습니다</div>
                        )
                    ) : (
                        <div className="mam-loading"><Spinner /></div>
                    )}
                </div>
            )}

            {/* ════ 레버리지 탭 (별도 분리) ════ */}
            {tab === "leverage" && (
                <div className="mam-holdings mam-holdings--tab mam-tab-content" key="leverage">
                    {account ? (
                        leveragePositions.length > 0 ? (
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

                                    return (
                                        <div key={`${pos.stockCode}-${pos.leverageRatio}`} className="mam-holding-row">
                                            <div className="mam-holding-row__main mam-holding-row__main--leverage">
                                                <div className="mam-holding-name">
                                                    <img
                                                        className="mam-holding-icon"
                                                        src={`https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${pos.stockCode}.svg`}
                                                        alt=""
                                                        onError={(e) => {
                                                            (e.currentTarget as HTMLImageElement).style.visibility = "hidden";
                                                        }}
                                                    />
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
                                                    <span className={`mam-holding-cell__top ${pos.profitAmount >= 0 ? "positive" : "negative"}`}>
                                                        {fmtSigned(pos.profitAmount)}
                                                    </span>
                                                    <span className={`mam-holding-cell__bottom ${pos.profitRate >= 0 ? "positive" : "negative"}`}>
                                                        {fmtRate(pos.profitRate)}
                                                    </span>
                                                </div>

                                                <div className="mam-holding-cell">
                                                    <span className="mam-holding-cell__top">{fmt(pos.purchaseAmount)}</span>
                                                    <span className="mam-holding-cell__bottom">{fmt(pos.evaluationAmount)}</span>
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
                        ) : (
                            <div className="mam-empty">보유 중인 레버리지 포지션이 없습니다</div>
                        )
                    ) : (
                        <div className="mam-loading"><Spinner /></div>
                    )}
                </div>
            )}

            {/* ════ 수익률 탭 ════ */}
            {tab === "profit" && (
                <div className="mam-profit mam-tab-content" ref={scrollRef} key="profit">
                    {profitItems.length > 0 && (
                        <div className="mam-profit-header">
                            <span>일자</span>
                            <span>당일 손익</span>
                            <span>당일 수익률</span>
                            <span>누적손익</span>
                            <span>누적수익률</span>
                            <span>거래대금</span>
                        </div>
                    )}
                    {profitItems.map((p) => (
                        <div key={p.date} className="mam-profit-row">
                            <span>{p.date}</span>
                            <span className={p.dailyProfitAmount >= 0 ? "positive" : "negative"}>{fmtSigned(p.dailyProfitAmount)}</span>
                            <span className={p.dailyProfitRate >= 0 ? "positive" : "negative"}>{fmtRate(p.dailyProfitRate)}</span>
                            <span className={p.cumulativeProfitAmount >= 0 ? "positive" : "negative"}>{fmtSigned(p.cumulativeProfitAmount)}</span>
                            <span className={p.cumulativeProfitRate >= 0 ? "positive" : "negative"}>{fmtRate(p.cumulativeProfitRate)}</span>
                            <span>{fmt(p.dailyTradeAmount)}</span>
                        </div>
                    ))}

                    {profitLoading && <div className="mam-loading"><Spinner /></div>}
                    {!profitLoading && !profitHasNext && profitItems.length > 0 && (
                        <div className="mam-end-of-list">마지막 데이터입니다</div>
                    )}
                    {!profitLoading && profitItems.length === 0 && (
                        <div className="mam-empty">수익 기록이 없습니다</div>
                    )}
                </div>
            )}
        </ModalV2>
    );
}
