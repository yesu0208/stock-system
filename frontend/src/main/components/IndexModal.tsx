import "./IndexModal.css";
import ModalV2 from "../../components/ModalV2";
import { useMarketData } from "../context/MarketDataContext";
import { useState } from "react";

type Tab = "KOSPI" | "KOSDAQ" | "FUTURES";

interface Props {
    open: boolean;
    onClose: () => void;
}

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

export default function IndexModal({ open, onClose }: Props) {
    const { marketMain } = useMarketData();
    const [tab, setTab] = useState<Tab>("KOSPI");

    const color = (dir: string) => {
        if (dir === "UP" || dir.includes("+")) return "#ff5b5b";
        if (dir === "DOWN" || dir.includes("-")) return "#4f9dff";
        return "#fff";
    };

    const valueColor = (value: string) => {
        if (value.includes("+")) return "#ff5b5b";
        if (value.includes("-")) return "#4f9dff";
        return "#fff";
    };

    const breadthColor = (type: string) => {
        if (type === "rise" || type === "upperLimit") return "#ff5b5b";
        if (type === "fall" || type === "lowerLimit") return "#4f9dff";
        return "#fff";
    };

    return (
        <ModalV2 open={open} title="지수" onClose={onClose}>
            {!marketMain ? (
                <div className="index-wrap">
                    <div className="box">로딩 중...</div>
                </div>
            ) : (
                <div className="index-wrap">
                    {(() => {
                        const current =
                            tab === "KOSPI"
                                ? marketMain.kospi
                                : tab === "KOSDAQ"
                                    ? marketMain.kosdaq
                                    : marketMain.kospi200;

                        return (
                            <>
                                <div className="tab">
                                    <button
                                        className={tab === "KOSPI" ? "active" : ""}
                                        onClick={() => setTab("KOSPI")}
                                    >
                                        KOSPI
                                    </button>

                                    <button
                                        className={tab === "KOSDAQ" ? "active" : ""}
                                        onClick={() => setTab("KOSDAQ")}
                                    >
                                        KOSDAQ
                                    </button>

                                    <button
                                        className={tab === "FUTURES" ? "active" : ""}
                                        onClick={() => setTab("FUTURES")}
                                    >
                                        KOSPI 200
                                    </button>
                                </div>

                                <div className="content fade" key={tab}>
                                    <div className="box main">
                                        <div className="index" style={{ color: color(current.direction) }}>
                                            {current.currentIndex}
                                        </div>

                                        <div className="right">
                                            <div className="change" style={{ color: color(current.direction) }}>
                <span className="triangle">
                    {current.direction === "UP" || current.direction.includes("+")
                        ? "▲"
                        : current.direction === "DOWN" || current.direction.includes("-")
                            ? "▼"
                            : ""}
                </span>
                                                {current.changeValue} ({current.changeRate})
                                            </div>

                                            <div className="time">기준 {formatBaseTime(current.baseTime)}</div>
                                        </div>
                                    </div>

                                    <div className="section-title">등락 종목</div>
                                    <div className="box">
                                        <div className="grid5">
                                            <div>
                                                <div className="label">상한</div>
                                                <div style={{ color: breadthColor("upperLimit") }}>
                                                    {current.breadth.upperLimit ?? "0"}
                                                </div>
                                            </div>
                                            <div>
                                                <div className="label">상승</div>
                                                <div style={{ color: breadthColor("rise") }}>
                                                    {current.breadth.rise ?? "0"}
                                                </div>
                                            </div>
                                            <div>
                                                <div className="label">보합</div>
                                                <div>{current.breadth.steady ?? "0"}</div>
                                            </div>
                                            <div>
                                                <div className="label">하락</div>
                                                <div style={{ color: breadthColor("fall") }}>
                                                    {current.breadth.fall ?? "0"}
                                                </div>
                                            </div>
                                            <div>
                                                <div className="label">하한</div>
                                                <div style={{ color: breadthColor("lowerLimit") }}>
                                                    {current.breadth.lowerLimit ?? "0"}
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="section-title">프로그램 매매</div>
                                    <div className="box grid3">
                                        <div>
                                            <div className="label">차익</div>
                                            <div style={{ color: valueColor(current.programTrade.arbitrage) }}>
                                                {withUnit(current.programTrade.arbitrage)}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="label">비차익</div>
                                            <div style={{ color: valueColor(current.programTrade.nonArbitrage) }}>
                                                {withUnit(current.programTrade.nonArbitrage)}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="label">전체</div>
                                            <div style={{ color: valueColor(current.programTrade.total) }}>
                                                {withUnit(current.programTrade.total)}
                                            </div>
                                        </div>
                                    </div>

                                    <div className="section-title">투자자별 동향</div>
                                    <div className="box grid3">
                                        <div>
                                            <div className="label">개인</div>
                                            <div style={{ color: valueColor(current.investorTrend.personal) }}>
                                                {withUnit(current.investorTrend.personal)}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="label">외국인</div>
                                            <div style={{ color: valueColor(current.investorTrend.foreigner) }}>
                                                {withUnit(current.investorTrend.foreigner)}
                                            </div>
                                        </div>
                                        <div>
                                            <div className="label">기관</div>
                                            <div style={{ color: valueColor(current.investorTrend.institution) }}>
                                                {withUnit(current.investorTrend.institution)}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </>
                        );
                    })()}
                </div>
            )}
        </ModalV2>
    );
}
