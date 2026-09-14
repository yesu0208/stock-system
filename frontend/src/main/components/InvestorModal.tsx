import "./InvestorModal.css";
import ModalV2 from "../../components/ModalV2";
import { useEffect, useState } from "react";
import { getInvestorTrend } from "../../api/marketInfo";
import type { InvestorTrendDto } from "../../types/marketWidgets";

interface Props {
    open: boolean;
    onClose: () => void;
}

const COLUMNS: { key: keyof InvestorTrendDto; label: string }[] = [
    { key: "dateOrTime", label: "시간/날짜" },
    { key: "individual", label: "개인" },
    { key: "foreigner", label: "외국인" },
    { key: "institution", label: "기관" },
    { key: "financeInvestment", label: "금융투자" },
    { key: "insurance", label: "보험" },
    { key: "fund", label: "투신" },
    { key: "bank", label: "은행" },
    { key: "etcFinance", label: "기타금융" },
    { key: "pension", label: "연기금" },
    { key: "corporation", label: "기타법인" },
];

const FIRST_COL_WIDTH = 100;
const COL_WIDTH = 82;

export default function InvestorModal({ open, onClose }: Props) {
    const [market, setMarket] = useState<"KOSPI" | "KOSDAQ">("KOSPI");
    const [trendType, setTrendType] = useState<"time" | "day">("time");
    const [data, setData] = useState<InvestorTrendDto[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!open) return;
        setLoading(true);
        getInvestorTrend(market, trendType, 1)
            .then((res) => setData(res.data))
            .catch(() => setData([]))
            .finally(() => setLoading(false));
    }, [open, market, trendType]);

    const gridTemplateColumns = `${FIRST_COL_WIDTH}px repeat(${COLUMNS.length - 1}, ${COL_WIDTH}px)`;

    return (
        <ModalV2 open={open} title="투자자별 매매동향" onClose={onClose}>
            <div className="investor-wrap">
                <div className="tab-row">
                    <div className="tab">
                        {(["KOSPI", "KOSDAQ"] as const).map((m) => (
                            <button
                                key={m}
                                className={market === m ? "active" : ""}
                                onClick={() => setMarket(m)}
                            >
                                {m}
                            </button>
                        ))}
                    </div>
                    <div className="tab">
                        {(["time", "day"] as const).map((t) => (
                            <button
                                key={t}
                                className={trendType === t ? "active" : ""}
                                onClick={() => setTrendType(t)}
                            >
                                {t === "time" ? "시간별" : "일별"}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="table-box">
                    <div className="table-inner">
                        <div className="header-row" style={{ gridTemplateColumns }}>
                            {COLUMNS.map((col) => (
                                <span key={col.key} className={col.key === "dateOrTime" ? "" : "num-cell"}>
                                    {col.label}
                                </span>
                            ))}
                        </div>

                        {loading ? (
                            <div className="empty-state">불러오는 중...</div>
                        ) : data.length === 0 ? (
                            <div className="empty-state">데이터가 없습니다.</div>
                        ) : (
                            data.map((d, i) => (
                                <div key={i} className="data-row" style={{ gridTemplateColumns }}>
                                    {COLUMNS.map((col) => {
                                        if (col.key === "dateOrTime") {
                                            return <span key={col.key}>{d.dateOrTime}</span>;
                                        }
                                        const value = d[col.key] as number;
                                        return (
                                            <span
                                                key={col.key}
                                                className="num-cell"
                                                style={{ color: value >= 0 ? "#ff5b5b" : "#4f9dff" }}
                                            >
                                                {value.toLocaleString()}
                                            </span>
                                        );
                                    })}
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
        </ModalV2>
    );
}
