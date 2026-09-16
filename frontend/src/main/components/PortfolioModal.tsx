import { Fragment, useEffect, useRef, useState } from "react";
import ModalV2 from "../../components/ModalV2";
import { FiPieChart, FiBarChart2, FiActivity } from "react-icons/fi";
import { useAccount } from "../context/AccountContext";
import { usePortfolio } from "../context/PortfolioContext";
import { stockNameMap } from "../../constants/stocks";
import "./PortfolioModal.css";

const SECTOR_PALETTE = [
    "#6366f1", "#22d3ee", "#f59e0b", "#34d399", "#fb923c",
    "#ec4899", "#a78bfa", "#facc15", "#38bdf8", "#14b8a6",
    "#94a3b8", "#f472b6", "#818cf8",
];

// [추가] 현금 항목 라벨/색상 상수 (클릭 불가 섹터로 사용)
const CASH_LABEL = "현금";
const CASH_COLOR = "#64748b";

function hexToHsl(hex: string): [number, number, number] {
    const r = parseInt(hex.slice(1, 3), 16) / 255;
    const g = parseInt(hex.slice(3, 5), 16) / 255;
    const b = parseInt(hex.slice(5, 7), 16) / 255;
    const max = Math.max(r, g, b), min = Math.min(r, g, b);
    let h = 0, s = 0;
    const l = (max + min) / 2;
    if (max !== min) {
        const d = max - min;
        s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
        switch (max) {
            case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break;
            case g: h = ((b - r) / d + 2) / 6;                break;
            case b: h = ((r - g) / d + 4) / 6;                break;
        }
    }
    return [h * 360, s * 100, l * 100];
}

function hslToHex(h: number, s: number, l: number): string {
    h /= 360; s /= 100; l /= 100;
    let r: number, g: number, b: number;
    if (s === 0) {
        r = g = b = l;
    } else {
        const hue2rgb = (p: number, q: number, t: number) => {
            if (t < 0) t += 1; if (t > 1) t -= 1;
            if (t < 1 / 6) return p + (q - p) * 6 * t;
            if (t < 1 / 2) return q;
            if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
            return p;
        };
        const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        const p = 2 * l - q;
        r = hue2rgb(p, q, h + 1 / 3);
        g = hue2rgb(p, q, h);
        b = hue2rgb(p, q, h - 1 / 3);
    }
    return `#${[r, g, b].map(x => Math.round(x * 255).toString(16).padStart(2, "0")).join("")}`;
}

function generateStockColors(baseHex: string, count: number): string[] {
    const [h, s, l] = hexToHsl(baseHex);
    return Array.from({ length: count }, (_, i) => {
        const center = (count - 1) / 2;
        const hueShift = (i - center) * 20;
        const lightShift = (i - center) * 14;
        return hslToHex(
            (h + hueShift + 360) % 360,
            Math.min(95, Math.max(35, s)),
            Math.min(78, Math.max(28, l + lightShift)),
        );
    });
}

type SectorData = { name: string; pct: number; color: string };

function DonutChart({
                        sectors,
                        open,
                        selectedSector,
                        onSectorClick,
                        size = 90,
                    }: {
    sectors: SectorData[];
    open: boolean;
    selectedSector: string | null;
    onSectorClick: (name: string) => void;
    size?: number;
}) {
    const [progress, setProgress] = useState(0);
    const rafRef = useRef<number | undefined>(undefined);
    const startRef = useRef<number | undefined>(undefined);
    const DURATION = 1000;

    const [animatedPcts, setAnimatedPcts] = useState<number[]>(() => sectors.map(s => s.pct));
    const prevPctsRef = useRef<number[]>(sectors.map(s => s.pct));
    const pctRafRef = useRef<number | undefined>(undefined);
    const PCT_DURATION = 500;

    const pctKey = sectors.map(s => s.pct.toFixed(4)).join(",");

    useEffect(() => {
        const targets = sectors.map(s => s.pct);
        const froms = prevPctsRef.current.slice();

        if (froms.length !== targets.length) {
            setAnimatedPcts(targets);
            prevPctsRef.current = targets;
            return;
        }

        if (pctRafRef.current) cancelAnimationFrame(pctRafRef.current);

        let startTime: number | undefined;
        const animate = (ts: number) => {
            if (!startTime) startTime = ts;
            const t = Math.min((ts - startTime) / PCT_DURATION, 1);
            const eased = 1 - Math.pow(1 - t, 3);
            setAnimatedPcts(targets.map((to, i) => froms[i] + (to - froms[i]) * eased));
            if (t < 1) {
                pctRafRef.current = requestAnimationFrame(animate);
            } else {
                setAnimatedPcts(targets);
                prevPctsRef.current = targets;
            }
        };
        pctRafRef.current = requestAnimationFrame(animate);
        return () => { if (pctRafRef.current) cancelAnimationFrame(pctRafRef.current); };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [pctKey]);

    useEffect(() => {
        if (!open) { setProgress(0); return; }
        setProgress(0);
        startRef.current = undefined;

        const animate = (ts: number) => {
            if (startRef.current === undefined) startRef.current = ts;
            const t = Math.min((ts - startRef.current) / DURATION, 1);
            const eased = 1 - Math.pow(1 - t, 3);
            setProgress(eased);
            if (t < 1) rafRef.current = requestAnimationFrame(animate);
        };
        rafRef.current = requestAnimationFrame(animate);
        return () => { if (rafRef.current) cancelAnimationFrame(rafRef.current); };
    }, [open]);

    const SIZE = size;
    const STROKE = SIZE * 0.3125;
    const PAD = 3;
    const R = (SIZE - STROKE) / 2;
    const C = 2 * Math.PI * R;
    const VB_SIZE = SIZE + PAD * 2;

    let cumulative = 0;

    return (
        <svg
            width={SIZE}
            height={SIZE}
            viewBox={`${-PAD} ${-PAD} ${VB_SIZE} ${VB_SIZE}`}
            className="portfolio__donut-svg"
            style={{ cursor: "pointer", overflow: "visible" }}
        >
            <circle
                cx={SIZE / 2} cy={SIZE / 2} r={R}
                fill="none"
                stroke="rgba(255,255,255,0.06)"
                strokeWidth={STROKE}
            />
            {sectors.map((s, i) => {
                const animPct = animatedPcts[i] ?? s.pct;
                const f_start = cumulative;
                cumulative += animPct / 100;
                const f_end = cumulative;

                const visibleLen = Math.max(0, Math.min(progress, f_end) - f_start) * C;
                const dashOffset = C / 4 - f_start * C;

                const isSelected = selectedSector === s.name;
                const opacity = selectedSector === null ? 1 : isSelected ? 1 : 0.3;

                // [추가] 현금 슬라이스는 클릭 불가 처리
                const isCash = s.name === CASH_LABEL;

                return (
                    <circle
                        key={s.name}
                        cx={SIZE / 2} cy={SIZE / 2} r={R}
                        fill="none"
                        stroke={s.color}
                        strokeWidth={isSelected ? STROKE + 3 : STROKE}
                        strokeDasharray={`${visibleLen} ${C - visibleLen}`}
                        strokeDashoffset={dashOffset}
                        strokeLinecap="butt"
                        opacity={opacity}
                        style={{ transition: "opacity 0.2s, stroke-width 0.2s", cursor: isCash ? "default" : "pointer" }}
                        onClick={isCash ? undefined : () => onSectorClick(s.name)}
                    />
                );
            })}
        </svg>
    );
}

interface Props {
    open: boolean;
    onClose: () => void;
}

export default function PortfolioModal({ open, onClose }: Props) {
    const { account } = useAccount();
    const { portfolio } = usePortfolio();

    const fmt = (n: number) => n.toLocaleString("ko-KR");
    const fmtRate = (n: number) => `${n > 0 ? "+" : ""}${n.toFixed(2)}%`;
    const fmtSigned = (n: number) => `${n > 0 ? "+" : ""}${fmt(n)}`;
    const cls = (n: number) => n > 0 ? "pf__pos" : n < 0 ? "pf__neg" : "pf__neutral";

    const [selectedSector, setSelectedSector] = useState<string | null>(null);
    const [sectorOrder, setSectorOrder] = useState<string[]>([]);
    const [stockOrderMap, setStockOrderMap] = useState<Record<string, string[]>>({});

    const handleSectorClick = (name: string) => {
        // [추가] 현금 항목은 클릭해도 선택되지 않도록 무시
        if (name === CASH_LABEL) return;
        setSelectedSector(prev => prev === name ? null : name);
    };

    useEffect(() => {
        if (!selectedSector || !portfolio) return;
        setStockOrderMap(prev => {
            if (prev[selectedSector]) return prev;
            const sector = portfolio.sectors.find(s => s.sector === selectedSector);
            if (!sector) return prev;
            const order = [...sector.stocks]
                .sort((a, b) => b.totalAmount - a.totalAmount)
                .map(s => s.stockCode);
            return { ...prev, [selectedSector]: order };
        });
    }, [selectedSector, portfolio]);

    useEffect(() => {
        if (!open) {
            setSelectedSector(null);
            setSectorOrder([]);
            setStockOrderMap({});
            return;
        }
        if (!portfolio) return;
        const order = [...portfolio.sectors]
            .sort((a, b) => b.ratioInTotal - a.ratioInTotal)
            .map(s => s.sector);
        setSectorOrder(order);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open, !!portfolio]);

    if (!account || !portfolio) {
        return (
            <ModalV2 open={open} title="포트폴리오" onClose={onClose}>
                <div className="portfolio__empty">
                    <span className="portfolio__empty-text">불러오는 중...</span>
                </div>
            </ModalV2>
        );
    }

    const sectorsRaw = portfolio.sectors.map((s, i) => ({
        name: s.sector,
        pct: s.ratioInTotal,
        color: SECTOR_PALETTE[i % SECTOR_PALETTE.length],
    }));

    const sectors = sectorOrder.length
        ? (sectorOrder
            .map(name => sectorsRaw.find(s => s.name === name))
            .filter(Boolean) as typeof sectorsRaw)
        : [...sectorsRaw].sort((a, b) => b.pct - a.pct);

    // [추가] 현금 비중 계산 (전체 100%에서 섹터 비중 합을 뺀 나머지) 및 표시용 목록 구성
    const totalSectorPct = sectorsRaw.reduce((sum, s) => sum + s.pct, 0);
    const cashPct = Math.max(0, 100 - totalSectorPct);
    const cashEntry: SectorData | null = cashPct > 0.05
        ? { name: CASH_LABEL, pct: cashPct, color: CASH_COLOR }
        : null;
    // [수정] 현금 항목을 맨 앞에 위치하도록 변경
    const displaySectors: SectorData[] = cashEntry ? [cashEntry, ...sectors] : sectors;

    const selectedSectorData = selectedSector
        ? portfolio.sectors.find(s => s.sector === selectedSector)
        : undefined;

    const filteredStocksRaw = selectedSectorData?.stocks ?? [];

    const fixedOrder = selectedSector ? stockOrderMap[selectedSector] : undefined;
    const filteredStocks = fixedOrder
        ? (fixedOrder
            .map(code => filteredStocksRaw.find(s => s.stockCode === code))
            .filter(Boolean) as typeof filteredStocksRaw)
        : [...filteredStocksRaw].sort((a, b) => b.totalAmount - a.totalAmount);

    const sectorIndex = sectors.findIndex(s => s.name === selectedSector);
    const sectorBaseColor = selectedSector
        ? (sectorIndex >= 0 ? sectors[sectorIndex].color : "#94a3b8")
        : "#94a3b8";
    const stockColors = generateStockColors(sectorBaseColor, filteredStocks.length);
    const sectorTotal = filteredStocks.reduce((s, h) => s + h.totalAmount, 0);
    const stockSectors: SectorData[] = filteredStocks.map((h, i) => ({
        name: stockNameMap[h.stockCode] ?? h.stockCode,
        pct: sectorTotal > 0 ? (h.totalAmount / sectorTotal) * 100 : 0,
        color: stockColors[i],
    }));

    return (
        <ModalV2 open={open} title="포트폴리오" onClose={onClose}>
            <div className="portfolio">

                <div className="pf-summary">
                    <div className="pf-summary__title">
                        <FiActivity size={12} />
                        계좌 요약
                    </div>

                    {/* [수정] 총자산, 누적손익, 평가손익을 한 줄로 표시 (총매입/총평가 제거). 누적손익·평가손익 카드 디자인(pf-card--accent)은 유지 */}
                    <div className="pf-summary__body">
                        {/* [수정] 누적손익/평가손익과 동일하게 라벨-왼쪽/값-오른쪽 정렬 + 큰 글씨(pf-card--accent) 적용. 색상 바는 pos/neg 클래스가 없어 표시되지 않음 */}
                        <div className="pf-card pf-card--accent">
                            <span className="pf-card__label">총 자산</span>
                            <span className="pf-card__value">{fmt(account.totalValue)}원</span>
                        </div>

                        <div className={`pf-card pf-card--accent ${
                            account.accumulatedProfit > 0 ? "pf-card--pos" :
                                account.accumulatedProfit < 0 ? "pf-card--neg" : "pf-card--neutral"
                        }`}>
                            <span className="pf-card__label">누적손익</span>
                            <span className={`pf-card__value ${cls(account.accumulatedProfit)}`}>
                                {fmtSigned(account.accumulatedProfit)}원
                            </span>
                            <span className={`pf-card__rate ${cls(account.accumulatedProfitRate)}`}>
                                {fmtRate(account.accumulatedProfitRate)}
                            </span>
                        </div>

                        <div className={`pf-card pf-card--accent ${
                            account.totalProfit > 0 ? "pf-card--pos" :
                                account.totalProfit < 0 ? "pf-card--neg" : "pf-card--neutral"
                        }`}>
                            <span className="pf-card__label">평가손익</span>
                            <span className={`pf-card__value ${cls(account.totalProfit)}`}>
                                {fmtSigned(account.totalProfit)}원
                            </span>
                            <span className={`pf-card__rate ${cls(account.totalProfitRate)}`}>
                                {fmtRate(account.totalProfitRate)}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="portfolio__section portfolio__section--sector">
                    <div className="portfolio__section-title">
                        <FiPieChart style={{ marginRight: 6, opacity: 0.7 }} />
                        섹터 분포
                    </div>
                    <div className="portfolio__sector-row">
                        <DonutChart
                            sectors={displaySectors}
                            open={open}
                            selectedSector={selectedSector}
                            onSectorClick={handleSectorClick}
                        />
                        <div className="portfolio__legend">
                            {displaySectors.map((s, i) => {
                                // [추가] 현금 항목 여부 (클릭 불가, hover 스타일 제거)
                                const isCash = s.name === CASH_LABEL;
                                return (
                                    <div
                                        key={s.name}
                                        className={`portfolio__legend-item${selectedSector === s.name ? " portfolio__legend-item--selected" : ""}${selectedSector !== null && selectedSector !== s.name ? " portfolio__legend-item--dim" : ""}${isCash ? " portfolio__legend-item--cash" : ""}`}
                                        style={{ animationDelay: `${0.25 + i * 0.18}s` }}
                                        onClick={isCash ? undefined : () => handleSectorClick(s.name)}
                                    >
                                        <span className="portfolio__legend-dot" style={{ background: s.color }} />
                                        <span className="portfolio__legend-name">{s.name}</span>
                                        <span className="portfolio__legend-pct">{s.pct.toFixed(1)}%</span>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>

                <div className="portfolio__section portfolio__section--table">
                    <div className="portfolio__section-title">
                        <FiBarChart2 style={{ marginRight: 6, opacity: 0.7 }} />
                        보유 종목
                        {selectedSector && (
                            <span
                                className="portfolio__sector-badge"
                                style={{ borderColor: sectorBaseColor, color: sectorBaseColor }}
                            >
                                {selectedSector}
                            </span>
                        )}
                    </div>

                    <div className="portfolio__section-body">
                        {!selectedSector ? (
                            <div className="portfolio__empty">
                                <span className="portfolio__empty-icon">
                                    <FiPieChart size={22} />
                                </span>
                                <span className="portfolio__empty-text">섹터를 선택하면 해당 종목을 볼 수 있습니다</span>
                            </div>
                        ) : (
                            <div className="portfolio__stock-detail-row">
                                <DonutChart
                                    key={selectedSector}
                                    sectors={stockSectors}
                                    open={true}
                                    selectedSector={null}
                                    onSectorClick={() => {}}
                                    size={90}
                                />

                                <div className="portfolio__stock-legend">
                                    <div className="portfolio__stock-legend-body">
                                        <div className="portfolio__stock-legend-header">
                                            <span className="portfolio__stock-legend-dot" style={{ visibility: "hidden" }} />
                                            <span className="portfolio__stock-legend-name">종목명</span>
                                            <span className="portfolio__stock-legend-pct">비중</span>
                                            <span className="portfolio__stock-legend-type">구분</span>
                                            <span className="portfolio__stock-legend-buy">매수금액(유지증거금)</span>
                                            <span className="portfolio__stock-legend-eval">평가금액</span>
                                            <span className="portfolio__stock-legend-pnl">평가손익</span>
                                            <span className="portfolio__stock-legend-rate">수익률</span>
                                        </div>

                                        {filteredStocks.map((h, i) => {
                                            const hasSpot = h.spotAmount > 0;
                                            const hasLeverage = h.leverageAmount > 0;

                                            const rows: {
                                                label: string;
                                                buy: number | null;
                                                margin?: number;
                                                evalAmount: number | null;
                                                pnl: number | null;
                                                rate: number | null;
                                            }[] = [
                                                hasSpot
                                                    ? {
                                                        label: "현물",
                                                        buy: h.spotBuyAmount,
                                                        evalAmount: h.spotAmount,
                                                        pnl: h.spotProfitAmount,
                                                        rate: h.spotProfitRate,
                                                    }
                                                    : { label: "현물", buy: null, evalAmount: null, pnl: null, rate: null },
                                                hasLeverage
                                                    ? {
                                                        label: "신용",
                                                        buy: h.leverageBuyAmount,
                                                        margin: h.leverageEquityAmount,
                                                        evalAmount: h.leverageAmount,
                                                        pnl: h.leverageProfitAmount,
                                                        rate: h.leverageProfitRate,
                                                    }
                                                    : { label: "신용", buy: null, evalAmount: null, pnl: null, rate: null },
                                            ];

                                            return (
                                                <div
                                                    key={h.stockCode}
                                                    className="portfolio__stock-group"
                                                    style={{ animationDelay: `${0.2 + i * 0.12}s` }}
                                                >
                                                    <span
                                                        className="portfolio__stock-legend-dot"
                                                        style={{ background: stockColors[i], gridRow: `span ${rows.length}` }}
                                                    />
                                                    <span
                                                        className="portfolio__stock-legend-name"
                                                        style={{ gridRow: `span ${rows.length}` }}
                                                    >
                                                        {stockNameMap[h.stockCode] ?? h.stockCode}
                                                    </span>
                                                    <span
                                                        className="portfolio__stock-legend-pct"
                                                        style={{ gridRow: `span ${rows.length}` }}
                                                    >
                                                        {(stockSectors[i]?.pct ?? 0).toFixed(1)}%
                                                    </span>

                                                    {rows.map((r, ri) => (
                                                        <Fragment key={ri}>
                                                            <span className="portfolio__stock-legend-type">{r.label}</span>
                                                            <span className="portfolio__stock-legend-buy">
                                                                {r.buy === null
                                                                    ? "-"
                                                                    : `${fmt(r.buy)}${r.margin !== undefined ? ` (${fmt(r.margin)})` : ""}`}
                                                            </span>
                                                            <span className="portfolio__stock-legend-eval">
                                                                {r.evalAmount === null ? "-" : fmt(Math.round(r.evalAmount))}
                                                            </span>
                                                            <span className={`portfolio__stock-legend-pnl ${r.pnl === null ? "" : cls(r.pnl)}`}>
                                                                {r.pnl === null ? "-" : `${r.pnl >= 0 ? "+" : ""}${fmt(Math.round(r.pnl))}`}
                                                            </span>
                                                            <span className={`portfolio__stock-legend-rate ${r.rate === null ? "" : cls(r.rate)}`}>
                                                                {r.rate === null ? "-" : fmtRate(r.rate)}
                                                            </span>
                                                        </Fragment>
                                                    ))}
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </ModalV2>
    );
}
