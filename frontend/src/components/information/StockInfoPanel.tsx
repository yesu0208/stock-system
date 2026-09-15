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

type TabType = "summary" | "info" | "price" | "opinion" | "broker" | "trend";

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
                불러오는 중입니다
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
        if (isNaN(v) || isNaN(b)) return "white";
        if (v > b) return "#ff6347";
        if (v < b) return "#4f9dff";
        return "white";
    };

    const getForeignColor = (type: "sell" | "buy" | "diff", value: string): string => {
        if (type === "sell") return "#4f9dff";
        if (type === "buy") return "#ff6347";
        const n = parseFloat(value.replace(/,/g, ""));
        if (isNaN(n) || n === 0) return "white";
        return n > 0 ? "#ff6347" : "#4f9dff";
    };

    const getDiffColor = (diff: string): string => {
        if (diff.startsWith("▲") || diff.startsWith("⬆")) return "#ff6347";
        if (diff.startsWith("▼") || diff.startsWith("⬇")) return "#4f9dff";
        return "white";
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
                    className={`tab ${tab === "opinion" ? "active" : ""}`}
                    onClick={() => setTab("opinion")}
                >
                    투자의견
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
                        <p>{detail.companySummary}</p>
                    </div>
                )}

                {tab === "info" && (
                    <table className="info-table">
                        <tbody>
                        <Row2
                            a="시가총액" av={`${info.marketCap}억원`}
                        />

                        <Row3
                            a="주식수" av={info.listedShares}
                            b="액면가" bv={info.parValue}
                            c="매매단위" cv={info.tradingUnit}
                        />

                        <Row3
                            a="외인보유" av={info.foreignOwned}
                            b="외인비율" bv={info.foreignRate}
                            c="외인한도" cv={info.foreignLimit}
                        />

                        <Row3
                            a="PER" av={info.per}
                            b="추정 PER" bv={info.estimatedPer}
                            c="PBR" cv={info.pbr}
                        />

                        <Row3
                            a="EPS" av={info.eps}
                            b="추정 EPS" bv={info.estimatedEps}
                            c="BPS" cv={info.bps}
                        />

                        <Row3
                            a={"배당\n수익률"} av={info.dividendYield}
                            b="동일업종 PER" bv={info.sameIndustryPer}
                            c="동일업종 등락률" cv={info.sameIndustryRate}
                        />
                        </tbody>
                    </table>
                )}

                {tab === "price" && (
                    <table className="info-table">
                        <tbody>
                        <tr>
                            <td className="label">현재가</td>
                            <td className="value" style={{ color: getPriceColor(detail.currentPrice, detail.prevPrice) }}>
                                {detail.currentPrice ?? "-"}
                            </td>

                            <td className="label">전일 종가</td>
                            <td className="value">{detail.prevPrice ?? "-"}</td>

                            <td className="label"></td>
                            <td className="value"></td>
                        </tr>
                        <tr>
                            <td className="label">고가</td>
                            <td className="value" style={{ color: getPriceColor(detail.highPrice, detail.prevPrice) }}>
                                {detail.highPrice ?? "-"}
                            </td>

                            <td className="label">저가</td>
                            <td className="value" style={{ color: getPriceColor(detail.lowPrice, detail.prevPrice) }}>
                                {detail.lowPrice ?? "-"}
                            </td>

                            <td className="label">시가</td>
                            <td className="value" style={{ color: getPriceColor(detail.openPrice, detail.prevPrice) }}>
                                {detail.openPrice ?? "-"}
                            </td>
                        </tr>
                        <tr>
                            <td className="label">상한가</td>
                            <td className="value" style={{ color: getPriceColor(detail.upperLimit, detail.prevPrice) }}>
                                {detail.upperLimit ?? "-"}
                            </td>

                            <td className="label">하한가</td>
                            <td className="value" style={{ color: getPriceColor(detail.lowerLimit, detail.prevPrice) }}>
                                {detail.lowerLimit ?? "-"}
                            </td>

                            <td className="label"></td>
                            <td className="value"></td>
                        </tr>
                        <tr>
                            <td className="label">52주 최고</td>
                            <td className="value">{info.high52}</td>

                            <td className="label">52주 최저</td>
                            <td className="value">{info.low52}</td>

                            <td className="label"></td>
                            <td className="value"></td>
                        </tr>
                        </tbody>
                    </table>
                )}

                {tab === "opinion" && (
                    <table className="info-table">
                        <tbody>
                        <Row3
                            a="투자의견" av={info.opinion}
                            b="목표주가" bv={info.targetPrice}
                            c="컨센서스 기준일" cv={info.consensusDate}
                        />
                        </tbody>
                    </table>
                )}

                {tab === "broker" && (
                    <div>
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
                    </div>
                )}

                {tab === "trend" && (
                    <div>
                        <div className="broker-desc">
                            일별 외국인/기관 순매매 동향
                        </div>

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
                            <tbody>
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

                            {trendItems.map((t, i) => (
                                <tr key={`${t.date}-${i}`}>
                                    <td>{t.date}</td>
                                    <td>{t.closePrice}</td>
                                    <td style={{ color: getDiffColor(t.diff) }}>
                                        {formatDiff(t.diff)}
                                    </td>
                                    <td style={{ color: getDiffColor(t.diff) }}>
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
                            ))}
                            </tbody>
                        </table>

                        <div ref={observerRef} className="trend-sentinel">
                            {trendLoading && "불러오는 중..."}
                            {!trendHasNext && trendItems.length > 0 && "마지막 데이터입니다."}
                        </div>
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

function Row2({ a, av }: { a: string; av: string }) {
    return (
        <tr>
            <td className="label">{a}</td>
            <td className="value" colSpan={5}>{av}</td>
        </tr>
    );
}
