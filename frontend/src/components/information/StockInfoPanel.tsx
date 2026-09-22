import "./StockInfoPanel.css";

import { useState, useEffect, useRef, useCallback } from "react";
import { useStock } from "../../main/context/StockContext";
import { useRealtime } from "../../main/context/RealtimeContext";
import {
    getStockInfo,
    getStockDetailExtra,
    getForeignTrades,
} from "../../api/stockInfo";
import type {
    StockInfo,
    StockDetailExtraResponse,
    ForeignInstitutionTrade,
} from "../../api/stockInfo";
import type { StockDetailTickMessage } from "../../types/stockDetail";
import Spinner from "../../components/Spinner";

type TabType = "summary" | "info" | "price" | "broker" | "trend";

type MergedDetail = StockDetailExtraResponse & Partial<StockDetailTickMessage>;

export default function StockInfoPanel() {
    const { selectedStock } = useStock();
    const { subscribeStock } = useRealtime();
    const code = selectedStock.code;

    const [tab, setTab] = useState<TabType>("summary");

    const [info, setInfo] = useState<StockInfo | null>(null);
    const [detailExtra, setDetailExtra] = useState<StockDetailExtraResponse | null>(null);
    const [detailTick, setDetailTick] = useState<StockDetailTickMessage | null>(null);
    const [loading, setLoading] = useState(true);

    const detail: MergedDetail | null = detailExtra
        ? { ...detailExtra, ...(detailTick ?? {}) }
        : null;

    useEffect(() => {
        setTab("summary");
    }, [code]);

    useEffect(() => {
        setLoading(true);
        setInfo(null);
        setDetailExtra(null);
        setDetailTick(null);

        Promise.all([getStockInfo(code), getStockDetailExtra(code)])
            .then(([infoRes, detailRes]) => {
                setInfo(infoRes);
                setDetailExtra(detailRes);
            })
            .catch((e) => console.error(e))
            .finally(() => setLoading(false));
    }, [code]);

    useEffect(() => {
        return subscribeStock(code, (tick: any) => {
            if (tick.tickMessageType === "DETAIL") {
                setDetailTick(tick as StockDetailTickMessage);
            }
        });
    }, [code, subscribeStock]);

    const [trendItems, setTrendItems] = useState<ForeignInstitutionTrade[]>([]);
    const [trendPage, setTrendPage] = useState(1);
    const [trendHasNext, setTrendHasNext] = useState(true);
    const [trendLoading, setTrendLoading] = useState(false);

    const observerRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        setTrendItems([]);
        setTrendPage(1);
        setTrendHasNext(true);
    }, [code]);

    const fetchTrendPage = useCallback(async () => {
        if (!code || trendLoading || !trendHasNext) return;

        setTrendLoading(true);

        try {
            const data = await getForeignTrades(code, trendPage);

            setTrendItems((prev) =>
                trendPage === 1 ? data.items : [...prev, ...data.items]
            );
            setTrendHasNext(data.hasNext);
            setTrendPage((prev) => prev + 1);
        } catch (e) {
            console.error(e);
            setTrendHasNext(false);
        } finally {
            setTrendLoading(false);
        }
    }, [code, trendPage, trendHasNext]);

    useEffect(() => {
        if (tab === "trend" && trendItems.length === 0 && trendHasNext) {
            fetchTrendPage();
        }
    }, [tab]);

    useEffect(() => {
        if (tab !== "trend") return;

        const target = observerRef.current;
        if (!target) return;

        const observer = new IntersectionObserver(
            (entries) => {
                if (entries[0].isIntersecting) {
                    fetchTrendPage();
                }
            },
            { threshold: 0.1 }
        );

        observer.observe(target);

        return () => observer.disconnect();
    }, [tab, trendHasNext, trendLoading, trendPage]);

    if (loading || !info || !detail) {
        return (
            <div className="stock-info-panel stock-info-panel--loading">
                <Spinner />
            </div>
        );
    }

    const getTrendColor = (cls?: string) => {
        if (cls === "UP") return "#ff6347";
        if (cls === "DOWN") return "#4f9dff";
        return "white";
    };

    const getPriceColor = (value?: string, base?: string): string => {
        if (!value || !base) return "white";
        const v = parseFloat(value.replace(/,/g, ""));
        const b = parseFloat(base.replace(/,/g, ""));
        if (isNaN(v) || isNaN(b) || v === 0) return "white";
        if (v > b) return "#ff6347";
        if (v < b) return "#4f9dff";
        return "white";
    };

    const formatPriceValue = (value?: string): string => {
        if (!value) return "-";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n) || n === 0) return "-";
        return value;
    };

    const getRateColor = (value?: string): string => {
        if (!value) return "white";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n) || n === 0) return "white";
        return n > 0 ? "#ff6347" : "#4f9dff";
    };

    const getOpinionIndex = (score?: string): number => {
        if (!score) return 2;
        const n = parseFloat(score);
        if (isNaN(n)) return 2;
        return Math.min(4, Math.max(0, Math.round(n) - 1));
    };

    const isOpinionEmpty = (score?: string): boolean => {
        if (!score) return true;
        const n = parseFloat(score);
        return isNaN(n) || n === 0;
    };

    const getPricePosition = (current?: string, low?: string, high?: string): number => {
        if (!current || !low || !high) return 50;
        const c = parseFloat(current.replace(/,/g, ""));
        const l = parseFloat(low.replace(/,/g, ""));
        const h = parseFloat(high.replace(/,/g, ""));
        if (isNaN(c) || isNaN(l) || isNaN(h) || h === l) return 50;
        const pct = ((c - l) / (h - l)) * 100;
        return Math.min(100, Math.max(0, pct));
    };

    const getLabelPosition = (current?: string, low?: string, high?: string): number => {
        const raw = getPricePosition(current, low, high);
        return Math.min(903, Math.max(7, raw));
    };

    const getForeignColor = (type: "sell" | "buy" | "diff", value: string): string => {
        if (type === "sell") return "#4f9dff";
        if (type === "buy") return "#ff6347";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n) || n === 0) return "white";
        return n > 0 ? "#ff6347" : "#4f9dff";
    };

    const getRowColorByRate = (rate: string): string => {
        const n = parseFloat(rate.replace(/,/g, "").replace(/[^0-9.-]/g, ""));
        if (isNaN(n) || n === 0) return "white";
        return n > 0 ? "#ff6347" : "#4f9dff";
    };

    const formatDiffWithSign = (diff: string, rate: string): string => {
        const formatted = formatDiff(diff);
        const n = parseFloat(rate.replace(/,/g, "").replace(/[^0-9.-]/g, ""));
        if (isNaN(n) || n === 0) return formatted;
        if (n > 0 && !formatted.startsWith("+")) return `+${formatted}`;
        if (n < 0 && !formatted.startsWith("-")) return `-${formatted}`;
        return formatted;
    };

    const formatDiff = (diff: string): string => {
        const match = diff.match(/^([^\d-]*)([\d,.-]+)(.*)$/);
        if (!match) return diff;

        const [, prefix, numPart, suffix] = match;
        const num = parseFloat(numPart.replace(/,/g, ""));
        if (isNaN(num)) return diff;

        return `${prefix}${num.toLocaleString()}${suffix}`;
    };

    const getNetBuyColor = (value: string): string => {
        const n = parseFloat(value.replace(/,/g, "").replace(/[^0-9.-]/g, ""));
        if (isNaN(n) || n === 0) return "white";
        return n > 0 ? "#ff6347" : "#4f9dff";
    };

    const formatMarketCap = (value?: string): string => {
        if (!value) return "-";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n)) return value;

        const eok = Math.round(n / 100_000_000);
        const jo = Math.floor(eok / 10000);
        const remainder = eok % 10000;

        if (jo > 0) {
            return remainder > 0 ? `${jo}조 ${remainder.toLocaleString()}억` : `${jo}조`;
        }
        return `${remainder.toLocaleString()}억`;
    };

    const withUnit = (value?: string, unit = ""): string => {
        if (!value) return "-";
        return `${value}${unit}`;
    };

    const formatWon = (value?: string): string => {
        if (!value) return "-";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n)) return value;
        return `${Math.round(n).toLocaleString()}원`;
    };

    const withMultiplier = (value?: string): string => {
        if (!value) return "-";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n)) return `${value}배`;
        return `${n.toFixed(2)}배`;
    };

    return (
        <div className="stock-info-panel stock-info-panel--loaded">
            <div className="tabs">
                <button
                    className={`tab ${tab === "summary" ? "active" : ""}`}
                    onClick={() => setTab("summary")}
                >
                    기업 개요
                </button>

                <button
                    className={`tab ${tab === "info" ? "active" : ""}`}
                    onClick={() => setTab("info")}
                >
                    종목 정보
                </button>

                <button
                    className={`tab ${tab === "price" ? "active" : ""}`}
                    onClick={() => setTab("price")}
                >
                    종목 시세
                </button>

                <button
                    className={`tab ${tab === "broker" ? "active" : ""}`}
                    onClick={() => setTab("broker")}
                >
                    거래원 정보
                </button>

                <button
                    className={`tab ${tab === "trend" ? "active" : ""}`}
                    onClick={() => setTab("trend")}
                >
                    매매 동향
                </button>
            </div>

            <div className="tab-divider" />

            <div className="tab-content">
                {tab === "summary" && (
                    <div className="company-summary">
                        {detail.companySummary?.split("\n").map((line, i) => (
                            <p key={i}>{line}</p>
                        ))}
                    </div>
                )}

                {tab === "info" && (
                    <table className="info-table">
                        <tbody>
                        <Row3
                            a="시가총액" av={formatMarketCap(info.marketCap)}
                            b="액면가" bv={formatWon(info.parValue)}
                            c="매매단위" cv={withUnit(info.tradingUnit, "주")}
                        />

                        <Row2
                            a="주식수" av={withUnit(info.listedShares, "주")}
                            b="외인한도" bv={withUnit(info.foreignLimit, "주")}
                        />

                        <Row2
                            a="외인보유" av={withUnit(info.foreignOwned, "주")}
                            b="외인비율" bv={info.foreignRate}
                        />

                        <Row3
                            a="PER" av={withMultiplier(info.per)}
                            b="추정 PER" bv={withMultiplier(info.estimatedPer)}
                            c="PBR" cv={withMultiplier(info.pbr)}
                        />

                        <Row3
                            a="EPS" av={formatWon(info.eps)}
                            b="추정 EPS" bv={formatWon(info.estimatedEps)}
                            c="BPS" cv={formatWon(info.bps)}
                        />

                        <tr>
                            <td className="label">{"배당\n수익률"}</td>
                            <td className="value">{info.dividendYield}</td>

                            <td className="label">동일업종 PER</td>
                            <td className="value">{withMultiplier(info.sameIndustryPer)}</td>

                            <td className="label">동일업종 등락률</td>
                            <td className="value" style={{ color: getRateColor(info.sameIndustryRate) }}>
                                {info.sameIndustryRate}
                            </td>
                        </tr>
                        </tbody>
                    </table>
                )}

                {tab === "price" && (
                    <div className="price-tab">
                        <div className="price-left">
                            <div className="price-range-bar">
                                <div className="price-range-current-wrapper">
                                    <div
                                        className="price-range-current"
                                        style={{
                                            left: `${getLabelPosition(detail.currentPrice, info.low52, info.high52)}%`
                                        }}
                                    >
                                        <span className="price-range-label-title">현재가</span>
                                        <span className="price-range-label-value">
                                            {formatPriceValue(detail.currentPrice)}
                                        </span>
                                    </div>
                                </div>

                                <div className="price-range-track">
                                    <div
                                        className="price-range-marker"
                                        style={{
                                            left: `${getPricePosition(detail.currentPrice, info.low52, info.high52)}%`
                                        }}
                                    />
                                </div>

                                <div className="price-range-labels">
                                    <div className="price-range-label">
                                        <span className="price-range-label-title">52주 최저</span>
                                        <span className="price-range-label-value">{formatPriceValue(info.low52)}</span>
                                    </div>

                                    <div className="price-range-label price-range-label--right">
                                        <span className="price-range-label-title">52주 최고</span>
                                        <span className="price-range-label-value">{formatPriceValue(info.high52)}</span>
                                    </div>
                                </div>
                            </div>

                            <table className="info-table price-detail-table">
                                <tbody>
                                <tr>
                                    <td className="label">고가</td>
                                    <td className="value" style={{ color: getPriceColor(detail.highPrice, detail.prevPrice) }}>
                                        {formatPriceValue(detail.highPrice)}
                                    </td>

                                    <td className="label">저가</td>
                                    <td className="value" style={{ color: getPriceColor(detail.lowPrice, detail.prevPrice) }}>
                                        {formatPriceValue(detail.lowPrice)}
                                    </td>
                                </tr>
                                <tr>
                                    <td className="label">상한가</td>
                                    <td className="value" style={{ color: getPriceColor(detail.upperLimit, detail.prevPrice) }}>
                                        {formatPriceValue(detail.upperLimit)}
                                    </td>

                                    <td className="label">하한가</td>
                                    <td className="value" style={{ color: getPriceColor(detail.lowerLimit, detail.prevPrice) }}>
                                        {formatPriceValue(detail.lowerLimit)}
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </div>

                        <div className="price-right">
                            <div className="consensus-header">
                                <span>컨센서스</span>
                            </div>

                            <div className="consensus-rating-bar">
                                {["적극매도", "매도", "중립", "매수", "적극매수"].map((label, i) => {
                                    const empty = isOpinionEmpty(info.opinion);
                                    const isActive = !empty && i === getOpinionIndex(info.opinion);
                                    return (
                                        <div key={label} className="consensus-rating-item">
                                            <div className={`consensus-rating-badge ${isActive ? "consensus-rating-badge--active" : ""}`}>
                                                {isActive ? Number(info.opinion).toFixed(2) : ""}
                                            </div>
                                            <span className="consensus-rating-label">{label}</span>
                                        </div>
                                    );
                                })}
                            </div>

                            <div className="consensus-target">
                                <div className="consensus-target-title">목표주가</div>
                                <div className="consensus-target-value">{formatWon(info.targetPrice)}</div>
                            </div>

                            {!isOpinionEmpty(info.opinion) && (
                                <>
                                    <div className="consensus-desc">
                                        최근 3개월간 증권사에서 발표한 전망치의 평균값입니다.
                                    </div>

                                    <div className="consensus-date">
                                        {info.consensusDate} 기준 · 에프앤가이드 제공
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                )}

                {tab === "broker" && (
                    <div className="broker-tab-content">
                        {detail.brokerTrades.length === 0 ? (
                            <div className="broker-empty">거래원 정보가 없습니다</div>
                        ) : (
                            <>
                                <div className="broker-desc">
                                    일별 상위 5위 거래원의 누적 정보 기준 (20분 지연)
                                </div>

                                <table className="broker-table">
                                    <tbody>
                                    <tr className="broker-header-row">
                                        <td>매도상위</td>
                                        <td>거래량</td>
                                        <td>매수상위</td>
                                        <td>거래량</td>
                                    </tr>

                                    {detail.brokerTrades.map((t, i) => (
                                        <tr key={i}>
                                            <td style={{ color: getTrendColor(t.sellBrokerClass) }}>
                                                {t.sellBroker}
                                            </td>
                                            <td style={{ color: getTrendColor(t.sellVolumeClass) }}>
                                                {t.sellVolume}
                                            </td>
                                            <td style={{ color: getTrendColor(t.buyBrokerClass) }}>
                                                {t.buyBroker}
                                            </td>
                                            <td style={{ color: getTrendColor(t.buyVolumeClass) }}>
                                                {t.buyVolume}
                                            </td>
                                        </tr>
                                    ))}

                                    {detail.foreignBrokerSummary && (
                                        <tr className="foreign-total-row">
                                            <td>외국계추정합</td>
                                            <td style={{ color: getForeignColor("sell", detail.foreignBrokerSummary.sellVolume) }}>
                                                매도 {detail.foreignBrokerSummary.sellVolume}
                                            </td>
                                            <td style={{ color: getForeignColor("diff", detail.foreignBrokerSummary.buyDiff) }}>
                                                순매수 {detail.foreignBrokerSummary.buyDiff}
                                            </td>
                                            <td style={{ color: getForeignColor("buy", detail.foreignBrokerSummary.buyVolume) }}>
                                                매수 {detail.foreignBrokerSummary.buyVolume}
                                            </td>
                                        </tr>
                                    )}
                                    </tbody>
                                </table>
                            </>
                        )}
                    </div>
                )}

                {tab === "trend" && (
                    <div style={trendItems.length === 0 && trendLoading ? { height: "100%" } : undefined}>
                        {trendItems.length === 0 && trendLoading ? (
                            <div style={{ height: "100%" }}>
                                <Spinner />
                            </div>
                        ) : (
                            <>
                                <div className="trend-sticky-spacer" />
                                <table className="trend-table">
                                    <colgroup>
                                        <col style={{ width: "12%" }} />
                                        <col style={{ width: "11%" }} />
                                        <col style={{ width: "12%" }} />
                                        <col style={{ width: "11%" }} />
                                        <col style={{ width: "14%" }} />
                                        <col style={{ width: "12%" }} />
                                        <col style={{ width: "12%" }} />
                                        <col style={{ width: "16%" }} />
                                    </colgroup>
                                    <thead>
                                    <tr className="broker-header-row">
                                        <td rowSpan={2}>날짜</td>
                                        <td rowSpan={2}>종가</td>
                                        <td rowSpan={2}>등락</td>
                                        <td rowSpan={2}>등락률</td>
                                        <td rowSpan={2}>거래량</td>
                                        <td>기관</td>
                                        <td colSpan={2}>외국인</td>
                                    </tr>
                                    <tr className="broker-header-row broker-header-row--sub">
                                        <td>순매매</td>
                                        <td>순매매</td>
                                        <td>보유주수(비율)</td>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {trendItems.map((t, i) => {
                                        const rowColor = getRowColorByRate(t.rate);
                                        return (
                                            <tr key={`${t.date}-${i}`}>
                                                <td>{t.date}</td>
                                                <td style={{ color: rowColor }}>{t.closePrice}</td>
                                                <td style={{ color: rowColor }}>
                                                    {formatDiffWithSign(t.diff, t.rate)}
                                                </td>
                                                <td style={{ color: rowColor }}>
                                                    {t.rate}
                                                </td>
                                                <td>{t.volume}</td>
                                                <td style={{ color: getNetBuyColor(t.institutionNetBuy) }}>
                                                    {t.institutionNetBuy}
                                                </td>
                                                <td style={{ color: getNetBuyColor(t.foreignNetBuy) }}>
                                                    {t.foreignNetBuy}
                                                </td>
                                                <td className="trend-cell-stack">
                                                    <div>{t.foreignHoldings}</div>
                                                    <div className="trend-rate-sub">({t.foreignRate})</div>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                    </tbody>
                                </table>

                                <div ref={observerRef} className="trend-sentinel">
                                    {trendLoading && <Spinner size={16} thickness={2} center={false} />}
                                    {!trendHasNext && trendItems.length > 0 && "마지막 데이터입니다."}
                                </div>
                            </>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
}

function Row3({
                  a, av,
                  b, bv,
                  c, cv,
              }: {
    a: string; av: string;
    b: string; bv: string;
    c: string; cv: string;
}) {
    return (
        <tr>
            <td className="label">{a}</td>
            <td className="value">{av}</td>

            <td className="label">{b}</td>
            <td className="value">{bv}</td>

            <td className="label">{c}</td>
            <td className="value">{cv}</td>
        </tr>
    );
}

function Row2({
                  a, av,
                  b, bv,
              }: {
    a: string; av: string;
    b: string; bv: string;
}) {
    return (
        <tr>
            <td className="label">{a}</td>
            <td className="value">{av}</td>

            <td className="label">{b}</td>
            <td className="value">{bv}</td>

            <td className="label"></td>
            <td className="value"></td>
        </tr>
    );
}
