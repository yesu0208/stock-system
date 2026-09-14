import "./Footer.css";
import { FiTrendingUp, FiGrid } from "react-icons/fi";
import { FaFire, FaBalanceScale } from "react-icons/fa";
import { useEffect, useState } from "react";
import { useMarketData } from "../context/MarketDataContext";

import IndexModal from "./IndexModal";
import HotStocksModal from "./HotStocksModal";
import SectorModal from "./SectorModal";
import InvestorModal from "./InvestorModal";

type HoverType = "kospi" | "kosdaq" | "fx" | null;
type ModalType = "index" | "investor" | "hot" | "sector" | null;

function formatBaseTime(iso: string): string {
    try {
        const d = new Date(iso);
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");

        const weekdays = ["일", "월", "화", "수", "목", "금", "토"];
        const weekday = weekdays[d.getDay()];

        return `${yyyy}.${mm}.${dd}(${weekday}) 기준`;
    } catch {
        return iso;
    }
}

function withUnit(value?: string): string {
    if (!value || value === "-") return value ?? "-";
    return `${value}억`;
}

export default function Footer() {
    const { marketMain, exchangeRates } = useMarketData();

    const [fxIdx, setFxIdx] = useState(0);
    const [hovered, setHovered] = useState<HoverType>(null);
    const [modal, setModal] = useState<ModalType>(null);

    useEffect(() => {
        const timer = setInterval(() => {
            setFxIdx((prev) =>
                exchangeRates.length ? (prev + 1) % exchangeRates.length : 0
            );
        }, 3000);

        return () => clearInterval(timer);
    }, [exchangeRates]);

    const fx = exchangeRates[fxIdx];

    const getColor = (direction?: string) => {
        if (!direction) return "#cbd5e1";

        if (direction.includes("UP") || direction.includes("상승") || direction.includes("+")) {
            return "#FF6347";
        }
        if (direction.includes("DOWN") || direction.includes("하락") || direction.includes("-")) {
            return "#4F9DFF";
        }
        return "#cbd5e1";
    };

    const getArrow = (direction?: string) => {
        if (!direction) return "";

        if (direction.includes("UP") || direction.includes("상승") || direction.includes("+")) {
            return "▲";
        }
        if (direction.includes("DOWN") || direction.includes("하락") || direction.includes("-")) {
            return "▼";
        }
        return "";
    };

    const formatChange = (value?: string, rate?: string) => {
        if (!value && !rate) return "";
        return `${value ?? ""} (${rate ?? ""})`.trim();
    };

    const labelStyle = {
        color: "#ffffff",
        fontWeight: 600,
        marginRight: "6px",
    };

    const colorClass = (v?: string) => {
        if (!v) return "white";
        const s = v.toString().replace(/,/g, "").trim();

        if (s.startsWith("+")) return "red";
        if (s.startsWith("-")) return "blue";

        const n = Number(s);
        if (isNaN(n)) return "white";

        if (n > 0) return "red";
        if (n < 0) return "blue";

        return "white";
    };

    const Tooltip = ({ type }: { type: "kospi" | "kosdaq" | "fx" }) => {
        const data = type === "kospi" ? marketMain?.kospi : type === "kosdaq" ? marketMain?.kosdaq : undefined;

        if (type === "fx") {
            return (
                <div className="tooltip">
                    <div className="tooltip-section">
                        <div className="tooltip-section">환율</div>

                        {exchangeRates.map((item, idx) => (
                            <div className="grid4 value-row" key={idx}>
                                <div className="cell white">{item.currencyName}</div>
                                <div className="cell" style={{ color: getColor(item.direction) }}>
                                    {item.rate ? `${item.rate} 원` : "-"}
                                </div>
                                <div className="cell" style={{ color: getColor(item.direction) }}>
                                    {getArrow(item.direction)}
                                </div>
                                <div className="cell" style={{ color: getColor(item.direction) }}>
                                    {item.change}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            );
        }

        const b = data?.breadth;
        const p = data?.programTrade;
        const i = data?.investorTrend;

        return (
            <div className="tooltip">
                <div className="tooltip-section">
                    <div className="tooltip-time">{data?.baseTime ? formatBaseTime(data.baseTime) : "-"}</div>
                </div>

                <div className="tooltip-section">
                    <div className="tooltip-title">등락종목</div>

                    <div className="grid5 header-row">
                        <div>상한가</div>
                        <div>상승</div>
                        <div>보합</div>
                        <div>하락</div>
                        <div>하한가</div>
                    </div>

                    <div className="grid5 value-row">
                        <div className="cell red">{b?.upperLimit ?? "-"}</div>
                        <div className="cell red">{b?.rise ?? "-"}</div>
                        <div className="cell white">{b?.steady ?? "-"}</div>
                        <div className="cell blue">{b?.fall ?? "-"}</div>
                        <div className="cell blue">{b?.lowerLimit ?? "-"}</div>
                    </div>
                </div>

                <div className="tooltip-section">
                    <div className="tooltip-title">프로그램 매매</div>

                    <div className="grid3 header-row">
                        <div>차익</div>
                        <div>비차익</div>
                        <div>합계</div>
                    </div>

                    <div className="grid3 value-row">
                        <div className={`cell ${colorClass(p?.arbitrage)}`}>{withUnit(p?.arbitrage)}</div>
                        <div className={`cell ${colorClass(p?.nonArbitrage)}`}>{withUnit(p?.nonArbitrage)}</div>
                        <div className={`cell ${colorClass(p?.total)}`}>{withUnit(p?.total)}</div>
                    </div>
                </div>

                <div className="tooltip-section">
                    <div className="tooltip-title">투자자 동향</div>

                    <div className="grid3 header-row">
                        <div>개인</div>
                        <div>외국인</div>
                        <div>기관</div>
                    </div>

                    <div className="grid3 value-row">
                        <div className={`cell ${colorClass(i?.personal)}`}>{withUnit(i?.personal)}</div>
                        <div className={`cell ${colorClass(i?.foreigner)}`}>{withUnit(i?.foreigner)}</div>
                        <div className={`cell ${colorClass(i?.institution)}`}>{withUnit(i?.institution)}</div>
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="footer-wrapper">
            <footer className="footer">
                <div className="footer-left">
                    <button className="left-btn" onClick={() => setModal("index")}>
                        <FiTrendingUp className="icon" />
                        지수
                    </button>

                    <button className="left-btn" onClick={() => setModal("investor")}>
                        <FaBalanceScale className="icon" />
                        수급
                    </button>

                    <button className="left-btn" onClick={() => setModal("hot")}>
                        <FaFire className="icon" />
                        인기
                    </button>

                    <button className="left-btn" onClick={() => setModal("sector")}>
                        <FiGrid className="icon" />
                        업종
                    </button>
                </div>

                <div
                    className="market-item"
                    onMouseEnter={() => setHovered("kospi")}
                    onMouseLeave={() => setHovered(null)}
                >
                    <span style={labelStyle}>KOSPI</span>
                    <span style={{ color: getColor(marketMain?.kospi.direction) }}>
                        {marketMain?.kospi.currentIndex ?? "-"}{" "}
                        {getArrow(marketMain?.kospi.direction)}{" "}
                        {formatChange(marketMain?.kospi.changeValue, marketMain?.kospi.changeRate)}
                    </span>

                    {hovered === "kospi" && <Tooltip type="kospi" />}
                </div>

                <div className="divider" />

                <div
                    className="market-item"
                    onMouseEnter={() => setHovered("kosdaq")}
                    onMouseLeave={() => setHovered(null)}
                >
                    <span style={labelStyle}>KOSDAQ</span>
                    <span style={{ color: getColor(marketMain?.kosdaq.direction) }}>
                        {marketMain?.kosdaq.currentIndex ?? "-"}{" "}
                        {getArrow(marketMain?.kosdaq.direction)}{" "}
                        {formatChange(marketMain?.kosdaq.changeValue, marketMain?.kosdaq.changeRate)}
                    </span>

                    {hovered === "kosdaq" && <Tooltip type="kosdaq" />}
                </div>

                <div className="divider" />

                <div
                    className="market-item"
                    onMouseEnter={() => setHovered("fx")}
                    onMouseLeave={() => setHovered(null)}
                >
                    <span style={labelStyle}>{fx?.currencyName ?? "환율"}</span>

                    <span style={{ color: getColor(fx?.direction) }}>
                        {fx?.rate ? `${fx.rate} 원 ` : "-"}
                        {getArrow(fx?.direction)}{" "}
                        {fx?.change ?? ""}
                    </span>

                    {hovered === "fx" && <Tooltip type="fx" />}
                </div>
            </footer>

            <IndexModal open={modal === "index"} onClose={() => setModal(null)} />
            <InvestorModal show={modal === "investor"} onClose={() => setModal(null)} />
            <HotStocksModal show={modal === "hot"} onClose={() => setModal(null)} />
            <SectorModal show={modal === "sector"} onClose={() => setModal(null)} />
        </div>
    );
}
