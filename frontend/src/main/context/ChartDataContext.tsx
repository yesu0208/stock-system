import { createContext, useContext, useState, useCallback, type ReactNode } from "react";

export type ChartCandle = {
    time: number; // UTCTimestamp(초)
    open: number;
    high: number;
    low: number;
    close: number;
    volume?: number;
};

type ChartDataState = {
    /** 종목코드별 분봉 캔들 배열 — TradingChart가 fetch/WS로 채워줌 */
    minuteCandles: Record<string, ChartCandle[]>;
    /** 종목코드별 전일 종가 — 일봉 히스토리의 직전 거래일 종가 */
    prevClose: Record<string, number>;
};

type ChartDataContextValue = ChartDataState & {
    /** 분봉 배열 전체 교체 — REST 초기 로딩 시 */
    setMinuteCandles: (code: string, candles: ChartCandle[]) => void;
    /** 마지막 캔들 갱신 or 새 캔들 추가 — WS 실시간 업데이트 시 */
    upsertMinuteCandle: (code: string, candle: ChartCandle) => void;
    /** 전일 종가 설정 */
    setPrevClose: (code: string, value: number) => void;
};

const ChartDataContext = createContext<ChartDataContextValue | null>(null);

export function ChartDataProvider({ children }: { children: ReactNode }) {
    const [minuteCandles, setMinuteCandlesState] = useState<Record<string, ChartCandle[]>>({});
    const [prevClose, setPrevCloseState] = useState<Record<string, number>>({});

    const setMinuteCandles = useCallback((code: string, candles: ChartCandle[]) => {
        setMinuteCandlesState((prev) => ({ ...prev, [code]: candles }));
    }, []);

    const upsertMinuteCandle = useCallback((code: string, candle: ChartCandle) => {
        setMinuteCandlesState((prev) => {
            const arr = prev[code] ?? [];
            const last = arr[arr.length - 1];
            const nextArr =
                last && last.time === candle.time
                    ? [...arr.slice(0, -1), candle] // 같은 분봉 갱신
                    : [...arr, candle]; // 새 분봉 추가
            return { ...prev, [code]: nextArr };
        });
    }, []);

    const setPrevClose = useCallback((code: string, value: number) => {
        setPrevCloseState((prev) => ({ ...prev, [code]: value }));
    }, []);

    return (
        <ChartDataContext.Provider
            value={{ minuteCandles, prevClose, setMinuteCandles, upsertMinuteCandle, setPrevClose }}
        >
            {children}
        </ChartDataContext.Provider>
    );
}

export function useChartData() {
    const ctx = useContext(ChartDataContext);
    if (!ctx) throw new Error("useChartData must be used within ChartDataProvider");
    return ctx;
}
