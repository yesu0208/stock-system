import "./InvestorModal.css";
import ModalV2 from "../../components/ModalV2";
import Spinner from "../../components/Spinner";
import { useEffect, useRef, useState } from "react";
import { getInvestorTrend } from "../../api/marketInfo";
import type { InvestorTrendMarket } from "../../api/marketInfo";
import type { InvestorTrendDto } from "../../types/marketWidgets";

interface Props {
    open: boolean;
    onClose: () => void;
}

type TrendType = "time" | "day";

const MARKET_TABS: { value: InvestorTrendMarket; label: string }[] = [
    { value: "KOSPI", label: "KOSPI" },
    { value: "KOSDAQ", label: "KOSDAQ" },
    { value: "FUTURES", label: "선물" },
];

interface TrendState {
    data: InvestorTrendDto[];
    page: number;
    hasNext: boolean;
    loading: boolean;
}

const INITIAL_TREND_STATE: TrendState = {
    data: [],
    page: 1,
    hasNext: false,
    loading: false,
};

export default function InvestorModal({ open, onClose }: Props) {
    const [market, setMarket] = useState<InvestorTrendMarket>("KOSPI");
    const [trendType, setTrendType] = useState<TrendType>("day");
    const [animating, setAnimating] = useState(false);

    const [dayState, setDayState] = useState<TrendState>(INITIAL_TREND_STATE);
    const [timeState, setTimeState] = useState<TrendState>(INITIAL_TREND_STATE);

    const current = trendType === "day" ? dayState : timeState;

    const sentinelRef = useRef<HTMLDivElement>(null);
    const tableWrapRef = useRef<HTMLDivElement>(null);
    const observerLocked = useRef(false);

    const fetchPage = (
        m: InvestorTrendMarket,
        type: TrendType,
        page: number,
        append: boolean
    ) => {
        const setter = type === "day" ? setDayState : setTimeState;
        setter((prev) => ({ ...prev, loading: true }));

        getInvestorTrend(m, type, page)
            .then((res) => {
                setter((prev) => ({
                    data: append ? [...prev.data, ...res.data] : res.data,
                    page,
                    hasNext: res.hasNext,
                    loading: false,
                }));
            })
            .catch(() => {
                setter((prev) => ({
                    ...prev,
                    loading: false,
                    ...(append ? {} : { data: [], hasNext: false }),
                }));
            });
    };

    useEffect(() => {
        if (!open) return;
        setDayState(INITIAL_TREND_STATE);
        setTimeState(INITIAL_TREND_STATE);
        fetchPage(market, "day", 1, false);
        fetchPage(market, "time", 1, false);
    }, [open, market]);

    useEffect(() => {
        if (current.data.length > 0) {
            observerLocked.current = false;
        }
    }, [current.data]);

    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel || !open) return;

        const observer = new IntersectionObserver(
            (entries) => {
                const entry = entries[0];
                if (observerLocked.current) return;

                if (entry.isIntersecting && current.hasNext && !current.loading) {
                    fetchPage(market, trendType, current.page + 1, true);
                }
            },
            {
                rootMargin: "0px 0px 80px 0px",
                threshold: 0.1,
            }
        );

        observer.observe(sentinel);
        return () => observer.disconnect();
    }, [open, market, trendType, current.hasNext, current.loading, current.page]);

    const lockAndScrollTop = () => {
        observerLocked.current = true;
        if (tableWrapRef.current) {
            tableWrapRef.current.scrollTop = 0;
        }
    };

    const changeTrendType = (next: TrendType) => {
        if (trendType === next) return;
        lockAndScrollTop();
        setAnimating(true);
        setTimeout(() => {
            setTrendType(next);
            setAnimating(false);
        }, 120);
    };

    const changeMarket = (m: InvestorTrendMarket) => {
        if (market === m) return;
        lockAndScrollTop();
        setAnimating(true);
        setTimeout(() => {
            setMarket(m);
            setAnimating(false);
        }, 120);
    };

    const fmt = (v: number) => {
        if (market === "FUTURES") {
            return v > 0 ? `+${v.toLocaleString()}` : v.toLocaleString();
        }
        const eok = Math.round(v / 100_000_000);
        return eok > 0 ? `+${eok.toLocaleString()}` : eok.toLocaleString();
    };

    const color = (v: number) => (v > 0 ? "#ff4d4f" : v < 0 ? "#3b82f6" : "#94a3b8");

    const formatDateOrTime = (value: string): string => {
        if (value.length === 8) {
            return `${value.slice(0, 4)}.${value.slice(4, 6)}.${value.slice(6, 8)}`;
        }
        if (value.length === 6) {
            return `${value.slice(0, 2)}:${value.slice(2, 4)}:${value.slice(4, 6)}`;
        }
        return value;
    };

    return (
        <ModalV2 open={open} title="투자자별 매매동향" onClose={onClose}>
            <div className="investor-wrap">
                <div className="market-tabs">
                    {MARKET_TABS.map((m) => (
                        <button
                            key={m.value}
                            className={market === m.value ? "active" : ""}
                            onClick={() => changeMarket(m.value)}
                        >
                            {m.label}
                        </button>
                    ))}
                </div>

                <div className="sub-tabs">
                    <button
                        className={trendType === "day" ? "active" : ""}
                        onClick={() => changeTrendType("day")}
                    >
                        일자별
                    </button>
                    <button
                        className={trendType === "time" ? "active" : ""}
                        onClick={() => changeTrendType("time")}
                    >
                        시간별
                    </button>
                </div>

                <div className={`content-area ${animating ? "animating" : ""}`}>
                    <div className="table-wrap" ref={tableWrapRef}>
                        {current.loading && current.data.length === 0 ? (
                            <Spinner />
                        ) : (
                            <>
                                <table>
                                    <thead>
                                    <tr>
                                        <th rowSpan={2}>시간</th>
                                        <th rowSpan={2}>개인</th>
                                        <th rowSpan={2}>외국인</th>
                                        <th colSpan={7}>기관계</th>
                                        <th rowSpan={2}>기타법인</th>
                                    </tr>
                                    <tr>
                                        <th>기관</th>
                                        <th>금융투자</th>
                                        <th>보험</th>
                                        <th>투신(사모)</th>
                                        <th>은행</th>
                                        <th>기타금융</th>
                                        <th>연기금등</th>
                                    </tr>
                                    </thead>

                                    <tbody>
                                    {current.data.map((row, i) => (
                                        <tr key={i}>
                                            <td className="time">{formatDateOrTime(row.dateOrTime)}</td>
                                            <td style={{ color: color(row.individual) }}>
                                                {fmt(row.individual)}
                                            </td>
                                            <td style={{ color: color(row.foreigner) }}>
                                                {fmt(row.foreigner)}
                                            </td>
                                            <td style={{ color: color(row.institution) }}>
                                                {fmt(row.institution)}
                                            </td>
                                            <td style={{ color: color(row.financeInvestment) }}>
                                                {fmt(row.financeInvestment)}
                                            </td>
                                            <td style={{ color: color(row.insurance) }}>
                                                {fmt(row.insurance)}
                                            </td>
                                            <td style={{ color: color(row.fund) }}>
                                                {fmt(row.fund)}
                                            </td>
                                            <td style={{ color: color(row.bank) }}>
                                                {fmt(row.bank)}
                                            </td>
                                            <td style={{ color: color(row.etcFinance) }}>
                                                {fmt(row.etcFinance)}
                                            </td>
                                            <td style={{ color: color(row.pension) }}>
                                                {fmt(row.pension)}
                                            </td>
                                            <td style={{ color: color(row.corporation) }}>
                                                {fmt(row.corporation)}
                                            </td>
                                        </tr>
                                    ))}

                                    {!current.loading && current.data.length === 0 && (
                                        <tr>
                                            <td colSpan={10} className="empty-state">
                                                데이터가 없습니다.
                                            </td>
                                        </tr>
                                    )}
                                    </tbody>
                                </table>

                                <div ref={sentinelRef} className="scroll-sentinel">
                                    {current.loading && (
                                        <div className="loading-indicator">
                                            <Spinner size={14} thickness={2} center={false} />
                                        </div>
                                    )}
                                    {!current.hasNext && current.data.length > 0 && (
                                        <div className="end-of-list">마지막 데이터</div>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </ModalV2>
    );
}
