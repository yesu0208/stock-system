import {
    useEffect,
    useRef,
    useState,
} from "react";

import {
    createChart,
    ColorType,
    CandlestickSeries,
    HistogramSeries,
    LineSeries,
    type ISeriesApi,
    type UTCTimestamp,
    type CandlestickData,
} from "lightweight-charts";

import { useStock } from "../context/StockContext";
import { useAccount } from "../context/AccountContext";
import { useChartData } from "../context/ChartDataContext";
import { useMinuteCandles } from "../hooks/useMinuteCandles";
import { useDailyCandles } from "../hooks/useDailyCandles";

import styles from "./TradingChart.module.css";

type CandleWithVolume = CandlestickData & {
    volume?: number;
};

type AvgPriceKey = "CASH" | "X1_5" | "X2" | "X2_5";

const AVG_PRICE_BUTTONS: { key: AvgPriceKey; label: string; color: string }[] = [
    { key: "CASH", label: "현금", color: "#94a3b8" },
    { key: "X1_5", label: "1.5x", color: "#7dd3fc" },
    { key: "X2",   label: "2x",   color: "#fbbf24" },
    { key: "X2_5", label: "2.5x", color: "#f87171" },
];

export default function TradingChart() {
    const { selectedStock } = useStock();
    const stockCode = selectedStock.code;

    const { account } = useAccount();

    const { setMinuteCandles: setChartMinuteCandles, setPrevClose } = useChartData();

    const rawMinuteCandles = useMinuteCandles(stockCode);
    const rawDailyCandles = useDailyCandles(stockCode);

    const chartContainerRef = useRef<HTMLDivElement | null>(null);

    const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);
    const volumeSeriesRef = useRef<any>(null);

    const ma5Ref = useRef<any>(null);
    const ma10Ref = useRef<any>(null);
    const ma20Ref = useRef<any>(null);
    const ma60Ref = useRef<any>(null);
    const ma120Ref = useRef<any>(null);

    const [activeAvgPrices, setActiveAvgPrices] = useState<Set<AvgPriceKey>>(new Set());

    const avgPriceLineRefs = useRef<Record<AvgPriceKey, any>>({
        CASH: null,
        X1_5: null,
        X2: null,
        X2_5: null,
    });

    function toggleAvgPrice(key: AvgPriceKey) {
        setActiveAvgPrices((prev) => {
            const next = new Set(prev);
            next.has(key) ? next.delete(key) : next.add(key);
            return next;
        });
    }

    function getAvgPrice(key: AvgPriceKey): number {
        if (key === "CASH") {
            const stockInfo = account?.stocks[stockCode];
            return stockInfo && stockInfo.quantity > 0
                ? Math.round(stockInfo.totalAmount / stockInfo.quantity)
                : 0;
        }

        const position = account?.leveragePositions?.find(
            (p) => p.stockCode === stockCode && p.leverageRatio === key
        );
        return position && position.quantity > 0
            ? Math.round(position.purchaseAmount / position.quantity)
            : 0;
    }

    const [timeframe, setTimeframe] = useState<"minute" | "day">("minute");
    const timeframeRef = useRef<"minute" | "day">("minute");

    const minuteCurrentRef = useRef<any>(null);
    const dailyCurrentRef = useRef<any>(null);

    const getCurrentData = () =>
        timeframeRef.current === "minute"
            ? minuteCandlesRef.current
            : dailyCandlesRef.current;

    const minuteCandlesRef = useRef<CandleWithVolume[]>([]);
    const dailyCandlesRef = useRef<CandleWithVolume[]>([]);

    const minuteInitializedRef = useRef(false);
    const dailyInitializedRef = useRef(false);

    const chartRef = useRef<any>(null);

    const [tooltip, setTooltip] = useState({
        visible: false,
        x: 0,
        y: 0,
        time: 0,
        open: 0,
        high: 0,
        low: 0,
        close: 0,
        volume: 0,
        basePrice: 0,

        openPct: 0,
        highPct: 0,
        lowPct: 0,
        closePct: 0,

        ma5: null as number | null,
        ma10: null as number | null,
        ma20: null as number | null,
        ma60: null as number | null,
        ma120: null as number | null,
    });

    const formatTime = (t: any) => {
        if (typeof t !== "number") return "";

        const d = new Date(t * 1000);

        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");

        const hour = String(d.getHours()).padStart(2, "0");
        const minute = String(d.getMinutes()).padStart(2, "0");

        if (timeframe === "day") {
            return `${year}.${month}.${day}`;
        }

        return `${year}.${month}.${day} ${hour}:${minute}`;
    };

    const formatTimeMDHM = (t: any) => {
        if (typeof t !== "number") return "";

        const d = new Date(t * 1000);

        const month = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");

        if (timeframeRef.current === "day") {
            return `${month}/${day}`;
        }

        const hour = String(d.getHours()).padStart(2, "0");
        const minute = String(d.getMinutes()).padStart(2, "0");

        return `${month}/${day} ${hour}:${minute}`;
    };

    const parseDailyDate = (date: string): UTCTimestamp => {
        const year = Number(date.slice(0, 4));
        const month = Number(date.slice(4, 6)) - 1;
        const day = Number(date.slice(6, 8));

        return Math.floor(
            new Date(year, month, day).getTime() / 1000
        ) as UTCTimestamp;
    };

    const parseMinuteDateTime = (
        date: string,
        time: string
    ): UTCTimestamp => {
        const year = Number(date.slice(0, 4));
        const month = Number(date.slice(4, 6)) - 1;
        const day = Number(date.slice(6, 8));

        const hour = Number(time.slice(0, 2));
        const minute = Number(time.slice(2, 4));
        const second = time.length >= 6 ? Number(time.slice(4, 6)) : 0;

        return Math.floor(
            new Date(
                year,
                month,
                day,
                hour,
                minute,
                second
            ).getTime() / 1000
        ) as UTCTimestamp;
    };

    const [extremeMarks, setExtremeMarks] = useState({
        max: null as any,
        min: null as any,
    });

    useEffect(() => {
        timeframeRef.current = timeframe;
    }, [timeframe]);

    useEffect(() => {
        if (!seriesRef.current) return;

        AVG_PRICE_BUTTONS.forEach(({ key, label, color }) => {
            const isActive = activeAvgPrices.has(key);
            const price = getAvgPrice(key);
            const existingLine = avgPriceLineRefs.current[key];

            if (!isActive || price <= 0) {
                if (existingLine) {
                    seriesRef.current!.removePriceLine(existingLine);
                    avgPriceLineRefs.current[key] = null;
                }
                return;
            }

            if (existingLine) {
                seriesRef.current!.removePriceLine(existingLine);
            }

            avgPriceLineRefs.current[key] = seriesRef.current!.createPriceLine({
                price,
                color,
                lineWidth: 1,
                lineStyle: 2,
                axisLabelVisible: true,
                title: label,
            });
        });
    }, [activeAvgPrices, account, stockCode]);

    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            width: chartContainerRef.current.clientWidth,
            height: chartContainerRef.current.clientHeight,

            localization: {
                timeFormatter: (time: any) => {
                    const date = new Date((time as number) * 1000);

                    const year = date.getFullYear();
                    const month = String(date.getMonth() + 1).padStart(2, "0");
                    const day = String(date.getDate()).padStart(2, "0");
                    const hour = String(date.getHours()).padStart(2, "0");
                    const minute = String(date.getMinutes()).padStart(2, "0");

                    return `${year}.${month}.${day} ${hour}:${minute}`;
                },
            },

            layout: {
                background: {
                    type: ColorType.Solid,
                    color: "#0f172a",
                },
                attributionLogo: false,
                textColor: "#d1d5db",
            },

            grid: {
                vertLines: {
                    visible: false,
                },
                horzLines: {
                    visible: false,
                },
            },

            timeScale: {
                timeVisible: true,
                secondsVisible: false,

                rightOffset: 5,

                tickMarkFormatter: (time: any) => {
                    const date = new Date((time as number) * 1000);

                    if (timeframeRef.current === "minute") {
                        const hour = String(date.getHours()).padStart(2, "0");
                        const minute = String(date.getMinutes()).padStart(2, "0");

                        return `${hour}:${minute}`;
                    }

                    const month = String(date.getMonth() + 1).padStart(2, "0");
                    const day = String(date.getDate()).padStart(2, "0");

                    return `${month}/${day}`;
                },
            },

            handleScroll: {
                mouseWheel: true,
                pressedMouseMove: true,
                horzTouchDrag: true,
                vertTouchDrag: false,
            },

            handleScale: {
                axisPressedMouseMove: {
                    time: true,
                    price: false,
                },

                mouseWheel: true,
                pinch: true,
            },
        });

        chartRef.current = chart;

        chart.timeScale().subscribeVisibleLogicalRangeChange(() => {
            requestAnimationFrame(() => {
                updateVisibleHighLow(chart);
            });
        });

        const maFormat = {
            type: "custom" as const,
            minMove: 1,
            formatter: (price: number) =>
                Math.round(price).toLocaleString("ko-KR"),
        };

        ma5Ref.current = chart.addSeries(LineSeries, {
            color: "#facc15",
            lineWidth: 1,
            priceLineVisible: false,
            lastValueVisible: false,
            priceFormat: maFormat,
        });

        ma10Ref.current = chart.addSeries(LineSeries, {
            color: "#22c55e",
            lineWidth: 1,
            priceLineVisible: false,
            lastValueVisible: false,
            priceFormat: maFormat,
        });

        ma20Ref.current = chart.addSeries(LineSeries, {
            color: "#38bdf8",
            lineWidth: 1,
            priceLineVisible: false,
            lastValueVisible: false,
            priceFormat: maFormat,
        });

        ma60Ref.current = chart.addSeries(LineSeries, {
            color: "#a78bfa",
            lineWidth: 1,
            priceLineVisible: false,
            lastValueVisible: false,
            priceFormat: maFormat,
        });

        ma120Ref.current = chart.addSeries(LineSeries, {
            color: "#f472b6",
            lineWidth: 1,
            priceLineVisible: false,
            lastValueVisible: false,
            priceFormat: maFormat,
        });

        volumeSeriesRef.current = chart.addSeries(
            HistogramSeries,
            {
                priceLineVisible: true,
                lastValueVisible: true,
            },
            1
        );

        volumeSeriesRef.current.applyOptions({
            priceFormat: {
                type: "custom",
                minMove: 1,
                formatter: (price: number) => {
                    if (price >= 1_000_000) {
                        return (price / 1_000_000).toFixed(1) + "M";
                    } else if (price >= 1_000) {
                        return (price / 1_000).toFixed(1) + "K";
                    }
                    return String(Math.round(price));
                },
            },
        });

        chart.priceScale("right").applyOptions({
            visible: true,
            autoScale: true,

            scaleMargins: {
                top: 0.17,
                bottom: 0.17,
            }
        });

        const candlestickSeries = chart.addSeries(
            CandlestickSeries,
            {
                upColor: "#FF6347",
                downColor: "#4F9DFF",
                wickUpColor: "#FF6347",
                wickDownColor: "#4F9DFF",
                borderVisible: false,
                priceScaleId: "right",
                priceFormat: {
                    type: "custom",
                    minMove: 1,
                    formatter: (price: number) =>
                        Math.round(price).toLocaleString("ko-KR"),
                },
            },
            0
        );

        seriesRef.current = candlestickSeries;

        setTimeout(() => {
            const panes = chart.panes();

            if (panes[0]) panes[0].setHeight(270);
            if (panes[1]) panes[1].setHeight(30);
        }, 0);

        chart.subscribeCrosshairMove((param) => {
            if (!param.point || !param.time) {
                setTooltip((prev) => ({
                    ...prev,
                    visible: false,
                }));
                return;
            }

            const data = param.seriesData.get(seriesRef.current!);

            if (!data) {
                setTooltip((prev) => ({
                    ...prev,
                    visible: false,
                }));
                return;
            }

            const candle = data as any;

            const fullCandle =
                getCurrentData().find(
                    (c) => c.time === candle.time
                ) as any;

            const candleIndex = getCurrentData().findIndex(
                (c) => c.time === candle.time
            );

            const isDaily = timeframeRef.current === "day";

            const basePrice = isDaily
                ? (candleIndex > 0
                    ? getCurrentData()[candleIndex - 1].close
                    : candle.open)
                : candle.open;

            const openPct = basePrice ? ((candle.open - basePrice) / basePrice) * 100 : 0;
            const highPct = basePrice ? ((candle.high - basePrice) / basePrice) * 100 : 0;
            const lowPct = basePrice ? ((candle.low - basePrice) / basePrice) * 100 : 0;
            const closePct = basePrice ? ((candle.close - basePrice) / basePrice) * 100 : 0;

            const getMAValue = (period: number) => {
                if (candleIndex < period - 1) return null;

                const slice = getCurrentData().slice(
                    candleIndex - period + 1,
                    candleIndex + 1
                );

                const sum = slice.reduce(
                    (acc, cur) => acc + cur.close,
                    0
                );

                return sum / period;
            };

            const rect = chartContainerRef.current!.getBoundingClientRect();

            setTooltip({
                visible: true,
                x: rect.left + param.point.x,
                y: rect.top + param.point.y,
                time: param.time as number,
                open: candle.open,
                high: candle.high,
                low: candle.low,
                close: candle.close,
                volume: fullCandle?.volume ?? 0,
                basePrice,
                openPct,
                highPct,
                lowPct,
                closePct,

                ma5: getMAValue(5),
                ma10: getMAValue(10),
                ma20: getMAValue(20),
                ma60: getMAValue(60),
                ma120: getMAValue(120),
            });
        });

        const resizeObserver = new ResizeObserver((entries) => {
            if (!entries.length) return;

            const { width, height } = entries[0].contentRect;

            if (width === 0 || height === 0) return;

            chart.applyOptions({
                width,
                height,
            });
        });

        resizeObserver.observe(chartContainerRef.current);

        return () => {
            resizeObserver.disconnect();
            chart.remove();
        };
    }, []);

    useEffect(() => {
        minuteInitializedRef.current = false;
        dailyInitializedRef.current = false;
        setTooltip((prev) => ({ ...prev, visible: false }));
        setExtremeMarks({ max: null, min: null });
    }, [stockCode]);

    const buildVolumeSeries = (data: CandleWithVolume[]) =>
        data.map((c, idx) => {
            const prevVol = idx > 0 ? data[idx - 1].volume ?? 0 : 0;
            const vol = c.volume ?? 0;

            return {
                time: c.time,
                value: vol,
                color: vol >= prevVol ? "#FF6347" : "#4F9DFF",
            };
        });

    const buildVolumeBar = (candle: CandleWithVolume, data: CandleWithVolume[]) => {
        const idx = data.length - 1;
        const prevVol = idx > 0 ? data[idx - 1].volume ?? 0 : 0;

        return {
            time: candle.time,
            value: candle.volume ?? 0,
            color: (candle.volume ?? 0) >= prevVol ? "#FF6347" : "#4F9DFF",
        };
    };

    useEffect(() => {
        if (!seriesRef.current) return;
        if (!selectedStock?.realtimeSupported) return;
        if (rawMinuteCandles.length === 0) return;

        const converted: CandleWithVolume[] = rawMinuteCandles
            .map((c) => ({
                time: parseMinuteDateTime(c.date, c.time),
                open: c.open,
                high: c.high,
                low: c.low,
                close: c.close,
                volume: c.volume,
            }))
            .sort((a, b) => Number(a.time) - Number(b.time));

        const prevLastTime = minuteCurrentRef.current?.time as number | undefined;

        minuteCandlesRef.current = converted;
        minuteCurrentRef.current = converted[converted.length - 1];

        setChartMinuteCandles(stockCode, converted as any);

        if (timeframeRef.current !== "minute") return;

        if (!minuteInitializedRef.current) {
            seriesRef.current.setData(converted);
            volumeSeriesRef.current?.setData(buildVolumeSeries(converted));
            minuteInitializedRef.current = true;

            updateMovingAverages();

            setTimeout(() => {
                chartRef.current?.timeScale().fitContent();
                if (chartRef.current) updateVisibleHighLow(chartRef.current);
            }, 0);
        } else {
            const last = converted[converted.length - 1];

            const isTimeRegressed =
                prevLastTime != null && Number(last.time) < Number(prevLastTime);

            if (isTimeRegressed) {
                seriesRef.current.setData(converted);
                volumeSeriesRef.current?.setData(buildVolumeSeries(converted));

                setTimeout(() => {
                    chartRef.current?.timeScale().fitContent();
                    if (chartRef.current) updateVisibleHighLow(chartRef.current);
                }, 0);
            } else {
                seriesRef.current.update(last);
                volumeSeriesRef.current?.update(buildVolumeBar(last, converted));
            }

            updateMovingAverages();

            if (chartRef.current) updateVisibleHighLow(chartRef.current);
        }
    }, [rawMinuteCandles, selectedStock?.realtimeSupported, stockCode]);

    useEffect(() => {
        if (!seriesRef.current) return;
        if (!selectedStock?.realtimeSupported) return;
        if (rawDailyCandles.length === 0) return;

        const converted: CandleWithVolume[] = rawDailyCandles
            .map((c) => ({
                time: parseDailyDate(c.date),
                open: c.open,
                high: c.high,
                low: c.low,
                close: c.close,
                volume: c.volume,
            }))
            .sort((a, b) => Number(a.time) - Number(b.time));

        const prevLastTime = dailyCurrentRef.current?.time as number | undefined;

        dailyCandlesRef.current = converted;
        dailyCurrentRef.current = converted[converted.length - 1];

        if (converted.length >= 2) {
            setPrevClose(stockCode, converted[converted.length - 2].close);
        }

        if (timeframeRef.current !== "day") return;

        if (!dailyInitializedRef.current) {
            seriesRef.current.setData(converted);
            volumeSeriesRef.current?.setData(buildVolumeSeries(converted));
            dailyInitializedRef.current = true;

            updateMovingAverages();

            setTimeout(() => {
                chartRef.current?.timeScale().fitContent();
                if (chartRef.current) updateVisibleHighLow(chartRef.current);
            }, 0);
        } else {
            const last = converted[converted.length - 1];

            const isTimeRegressed =
                prevLastTime != null && Number(last.time) < Number(prevLastTime);

            if (isTimeRegressed) {
                seriesRef.current.setData(converted);
                volumeSeriesRef.current?.setData(buildVolumeSeries(converted));

                setTimeout(() => {
                    chartRef.current?.timeScale().fitContent();
                    if (chartRef.current) updateVisibleHighLow(chartRef.current);
                }, 0);
            } else {
                seriesRef.current.update(last);
                volumeSeriesRef.current?.update(buildVolumeBar(last, converted));
            }

            updateMovingAverages();

            if (chartRef.current) updateVisibleHighLow(chartRef.current);
        }
    }, [rawDailyCandles, selectedStock?.realtimeSupported, stockCode]);

    const calculateMA = (
        data: CandleWithVolume[],
        period: number
    ) => {
        const result: any[] = [];

        for (let i = 0; i < data.length; i++) {
            if (i < period - 1) continue;

            const slice = data.slice(i - period + 1, i + 1);

            const sum = slice.reduce(
                (acc, cur) => acc + cur.close,
                0
            );

            result.push({
                time: data[i].time,
                value: sum / period,
            });
        }

        return result;
    };

    const updateMovingAverages = () => {
        const candles = getCurrentData();

        ma5Ref.current?.setData(calculateMA(candles, 5));
        ma10Ref.current?.setData(calculateMA(candles, 10));
        ma20Ref.current?.setData(calculateMA(candles, 20));
        ma60Ref.current?.setData(calculateMA(candles, 60));
        ma120Ref.current?.setData(calculateMA(candles, 120));
    };

    const updateVisibleHighLow = (chart: any) => {
        if (!seriesRef.current) return;

        const visibleRange = chart.timeScale().getVisibleLogicalRange();

        if (!visibleRange) return;

        const candles = getCurrentData();

        if (candles.length === 0) return;

        const from = Math.floor(visibleRange.from);
        const to = Math.ceil(visibleRange.to);

        const visibleCandles = candles.slice(
            Math.max(0, from),
            Math.min(candles.length, to + 1)
        );

        if (visibleCandles.length === 0) return;

        let maxCandle = visibleCandles[0];
        let minCandle = visibleCandles[0];

        for (const c of visibleCandles) {
            if (c.high > maxCandle.high) {
                maxCandle = c;
            }

            if (c.low < minCandle.low) {
                minCandle = c;
            }
        }

        const maxX = chart.timeScale().timeToCoordinate(maxCandle.time);
        const minX = chart.timeScale().timeToCoordinate(minCandle.time);

        const rawMaxY = seriesRef.current.priceToCoordinate(maxCandle.high);
        const rawMinY = seriesRef.current.priceToCoordinate(minCandle.low);

        const paneHeight = chart.panes()[0]?.getHeight() ?? 270;

        const LABEL_HEIGHT_ESTIMATE = 30;
        const LABEL_GAP = 6;
        const LABEL_MARGIN = LABEL_HEIGHT_ESTIMATE + LABEL_GAP;

        const maxY = rawMaxY != null ? Math.max(rawMaxY, LABEL_MARGIN) : null;
        const minY = rawMinY != null ? Math.min(rawMinY, paneHeight - LABEL_MARGIN) : null;

        const lastVisibleCandle = visibleCandles[visibleCandles.length - 1];

        const referencePrice = lastVisibleCandle?.close ?? 0;

        const chartWidth = chartContainerRef.current?.clientWidth ?? 0;

        setExtremeMarks({
            max:
                maxX != null && maxY != null
                    ? {
                        x: maxX,
                        y: maxY,

                        align:
                            maxX < 80
                                ? "left"
                                : maxX > chartWidth - 120
                                    ? "right"
                                    : "center",

                        value: maxCandle.high,
                        time: maxCandle.time,

                        pct: ((referencePrice - maxCandle.high) / maxCandle.high) * 100,
                    }
                    : null,

            min:
                minX != null && minY != null
                    ? {
                        x: minX,
                        y: minY,

                        align:
                            minX < 80
                                ? "left"
                                : minX > chartWidth - 120
                                    ? "right"
                                    : "center",

                        value: minCandle.low,
                        time: minCandle.time,

                        pct: ((referencePrice - minCandle.low) / minCandle.low) * 100,
                    }
                    : null,
        });
    };

    useEffect(() => {
        if (!seriesRef.current) return;

        const data =
            timeframe === "minute"
                ? minuteCandlesRef.current
                : dailyCandlesRef.current;

        if (data.length === 0) return;

        seriesRef.current.setData(data);
        volumeSeriesRef.current?.setData(buildVolumeSeries(data));

        updateMovingAverages();

        setTimeout(() => {
            chartRef.current?.timeScale().fitContent();

            if (chartRef.current) {
                updateVisibleHighLow(chartRef.current);
            }
        }, 0);
    }, [timeframe]);

    return (
        <div className={`${styles.wrapper} ${!selectedStock?.realtimeSupported ? styles.wrapperUnsupported : ''}`}>

            {!selectedStock?.realtimeSupported && (
                <div className={styles.unsupportedOverlay}>
                    <p className={styles.unsupportedMsg}>
                        차트를 지원하지 않는 종목입니다
                    </p>
                </div>
            )}

            <div className={styles.header}
                 style={{visibility: !selectedStock?.realtimeSupported ? 'hidden' : 'visible'}}>
                <span className={styles.headerTitle}>
                    차트
                </span>

                <div className={styles.headerButtons}>
                    <span className={styles.avgPriceLabel}>기간</span>
                    <div className={styles.btnGroup}>
                        <button
                            onClick={() => setTimeframe("minute")}
                            className={`${styles.btnBase} ${timeframe === "minute" ? styles.btnActive : styles.btnInactive}`}
                        >
                            분봉
                        </button>

                        <button
                            onClick={() => setTimeframe("day")}
                            className={`${styles.btnBase} ${timeframe === "day" ? styles.btnActive : styles.btnInactive}`}
                        >
                            일봉
                        </button>
                    </div>

                    <div className={styles.btnGroupDivider} />

                    <span className={styles.avgPriceLabel}>매입가</span>
                    <div className={styles.btnGroup}>
                        {AVG_PRICE_BUTTONS.map(({ key, label, color }) => {
                            const isActive = activeAvgPrices.has(key);
                            return (
                                <button
                                    key={key}
                                    onClick={() => toggleAvgPrice(key)}
                                    className={`${styles.btnBase} ${styles.btnInactive}`}
                                    style={isActive ? { background: color, color: "#0f172a" } : undefined}
                                >
                                    {label}
                                </button>
                            );
                        })}
                    </div>
                </div>
            </div>

            <div className={styles.chartArea}
                 style={{visibility: !selectedStock?.realtimeSupported ? 'hidden' : 'visible'}}>
                <div
                    ref={chartContainerRef}
                    className={styles.chartContainer}
                />

                {extremeMarks.max && (
                    <div
                        className={styles.extremeMax}
                        style={{
                            left: extremeMarks.max.x,
                            top: extremeMarks.max.y,
                            transform:
                                extremeMarks.max.align === "left"
                                    ? "translate(0%, calc(-100% - 6px))"
                                    : extremeMarks.max.align === "right"
                                        ? "translate(-100%, calc(-100% - 6px))"
                                        : "translate(-50%, calc(-100% - 6px))",
                        }}
                    >
                        <div>
                            최고{" "}
                            {extremeMarks.max.value.toLocaleString(undefined, {
                                maximumFractionDigits: 0,
                            })}
                            {" "}
                            (
                            {`${extremeMarks.max.pct >= 0 ? "+" : ""}${extremeMarks.max.pct.toFixed(2)}%`}
                            )
                        </div>

                        <div className={styles.extremeTime}>
                            {formatTimeMDHM(extremeMarks.max.time)}
                        </div>
                    </div>
                )}

                {extremeMarks.min && (
                    <div
                        className={styles.extremeMin}
                        style={{
                            left: extremeMarks.min.x,
                            top: extremeMarks.min.y,
                            transform:
                                extremeMarks.min.align === "left"
                                    ? "translate(0%, 6px)"
                                    : extremeMarks.min.align === "right"
                                        ? "translate(-100%, 6px)"
                                        : "translate(-50%, 6px)",
                        }}
                    >
                        <div>
                            최저{" "}
                            {extremeMarks.min.value.toLocaleString(undefined, {
                                maximumFractionDigits: 0,
                            })}
                            {" "}
                            (
                            {`${extremeMarks.min.pct >= 0 ? "+" : ""}${extremeMarks.min.pct.toFixed(2)}%`}
                            )
                        </div>

                        <div className={styles.extremeTime}>
                            {formatTimeMDHM(extremeMarks.min.time)}
                        </div>
                    </div>
                )}

                {tooltip.visible && (
                    <div
                        className={styles.tooltip}
                        style={{
                            left: tooltip.x + 10,
                            top: tooltip.y + 10,
                        }}
                    >
                        <div className={styles.tooltipTime}>
                            {formatTime(tooltip.time)}
                        </div>

                        <div className={styles.tooltipDivider}/>

                        {(() => {
                            const base = tooltip.basePrice;

                            const getColor = (v: number) =>
                                v > 0
                                    ? "#FF6347"
                                    : v < 0
                                        ? "#4F9DFF"
                                        : "#ffffff";

                            const openPct = base
                                ? ((tooltip.open - base) / base) * 100
                                : 0;

                            const highPct = base
                                ? ((tooltip.high - base) / base) * 100
                                : 0;

                            const lowPct = base
                                ? ((tooltip.low - base) / base) * 100
                                : 0;

                            const closePct = base
                                ? ((tooltip.close - base) / base) * 100
                                : 0;

                            return (
                                <div className={styles.tooltipRows}>
                                    <div className={styles.tooltipRow}>
                                    <span>
                                        <span className={styles.tooltipLabel}>시가</span>

                                        <span style={{color: "#ffffff"}}>
                                            {tooltip.open.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })}
                                        </span>
                                    </span>

                                        <span style={{color: getColor(openPct)}}>
                                        {(() => {
                                            const diff = tooltip.open - base;

                                            return `${diff > 0 ? "+" : ""}${diff.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })} (${openPct > 0 ? "+" : ""}${openPct.toFixed(2)}%)`;
                                        })()}
                                    </span>
                                    </div>

                                    <div className={styles.tooltipRow}>
                                    <span>
                                        <span className={styles.tooltipLabel}>고가</span>

                                        <span style={{color: "#ffffff"}}>
                                            {tooltip.high.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })}
                                        </span>
                                    </span>

                                        <span style={{color: getColor(highPct)}}>
                                        {(() => {
                                            const diff = tooltip.high - base;

                                            return `${diff > 0 ? "+" : ""}${diff.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })} (${highPct > 0 ? "+" : ""}${highPct.toFixed(2)}%)`;
                                        })()}
                                    </span>
                                    </div>

                                    <div className={styles.tooltipRow}>
                                    <span>
                                        <span className={styles.tooltipLabel}>저가</span>

                                        <span style={{color: "#ffffff"}}>
                                            {tooltip.low.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })}
                                        </span>
                                    </span>

                                        <span style={{color: getColor(lowPct)}}>
                                        {(() => {
                                            const diff = tooltip.low - base;

                                            return `${diff > 0 ? "+" : ""}${diff.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })} (${lowPct > 0 ? "+" : ""}${lowPct.toFixed(2)}%)`;
                                        })()}
                                    </span>
                                    </div>

                                    <div className={styles.tooltipRow}>
                                    <span>
                                        <span className={styles.tooltipLabel}>종가</span>

                                        <span style={{color: "#ffffff"}}>
                                            {tooltip.close.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })}
                                        </span>
                                    </span>

                                        <span style={{color: getColor(closePct)}}>
                                        {(() => {
                                            const diff = tooltip.close - base;

                                            return `${diff > 0 ? "+" : ""}${diff.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })} (${closePct > 0 ? "+" : ""}${closePct.toFixed(2)}%)`;
                                        })()}
                                    </span>
                                    </div>

                                    <div className={styles.tooltipRow}>
                                        <div className={styles.tooltipVolumeLeft}>
                                        <span className={styles.tooltipVolumeLabel}>
                                            거래량
                                        </span>

                                            <span className={styles.tooltipVolumeValue}>
                                            {tooltip.volume.toLocaleString(undefined, {
                                                maximumFractionDigits: 0,
                                            })} 주
                                        </span>
                                        </div>
                                    </div>

                                    <div className={styles.tooltipMaSection}>
                                        {[
                                            {label: "MA5", color: "#facc15", value: tooltip.ma5},
                                            {label: "MA10", color: "#22c55e", value: tooltip.ma10},
                                            {label: "MA20", color: "#38bdf8", value: tooltip.ma20},
                                            {label: "MA60", color: "#a78bfa", value: tooltip.ma60},
                                            {label: "MA120", color: "#f472b6", value: tooltip.ma120},
                                        ].map((ma) => {
                                            if (ma.value == null) {
                                                return (
                                                    <div key={ma.label} className={styles.tooltipRow}>
                                                    <span>
                                                        <span
                                                            className={styles.tooltipMaLabel}
                                                            style={{color: ma.color}}
                                                        >
                                                            {ma.label}
                                                        </span>

                                                        <span style={{color: "#ffffff"}}>-</span>
                                                    </span>

                                                        <span style={{color: "#ffffff"}}>-</span>
                                                    </div>
                                                );
                                            }

                                            const diff = tooltip.close - ma.value;
                                            const pct = ((tooltip.close - ma.value) / ma.value) * 100;
                                            const valueColor = "#ffffff";

                                            return (
                                                <div key={ma.label} className={styles.tooltipRow}>
                                                    <span>
                                                    <span
                                                        className={styles.tooltipMaLabel}
                                                        style={{color: ma.color}}
                                                    >
                                                        {ma.label}
                                                    </span>

                                                        <span style={{color: "#ffffff"}}>
                                                        {ma.value.toLocaleString(undefined, {
                                                            maximumFractionDigits: 0,
                                                        })}
                                                    </span>
                                                </span>

                                                    <span style={{color: valueColor}}>
                                                    {diff > 0 ? "+" : ""}
                                                        {diff.toLocaleString(undefined, {
                                                            maximumFractionDigits: 0,
                                                        })}
                                                        {" "}
                                                        (
                                                        {pct > 0 ? "+" : ""}
                                                        {pct.toFixed(2)}%
                                                    )
                                                </span>
                                                </div>
                                            );
                                        })}
                                    </div>
                                </div>
                            );
                        })()}
                    </div>
                )}
            </div>
        </div>
    );
}
