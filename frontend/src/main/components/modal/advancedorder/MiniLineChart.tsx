import { useEffect, useRef } from "react";

import {
    createChart,
    ColorType,
    AreaSeries,
    LineSeries,
    LineStyle,
} from "lightweight-charts";
import type { ISeriesApi, UTCTimestamp } from "lightweight-charts";

import { useChartData } from "../../../context/ChartDataContext";

export type MiniChartCandle = {
    time: UTCTimestamp;
    open: number;
    high: number;
    low: number;
    close: number;
    volume?: number;
};

type Props = {
    data?: MiniChartCandle[];
    prevClose?: number;

    code?: string;

    width?: number;
    height?: number;
    backgroundColor?: string;
};

export default function MiniLineChart({
                                          data: dataProp,
                                          prevClose: prevCloseProp,
                                          code,
                                          width = 320,
                                          height = 160,
                                          backgroundColor = "transparent",
                                      }: Props) {
    const { minuteCandles, prevClose: prevCloseMap } = useChartData();

    const data = code ? (minuteCandles[code] ?? []) : (dataProp ?? []);
    const prevClose = code ? (prevCloseMap[code] ?? 0) : (prevCloseProp ?? 0);

    const containerRef = useRef<HTMLDivElement | null>(null);
    const chartRef = useRef<any>(null);
    const areaRef = useRef<ISeriesApi<"Area"> | null>(null);
    const lineRef = useRef<ISeriesApi<"Line"> | null>(null);

    const toKST = (utcSeconds: number) => {
        return (utcSeconds + 9 * 60 * 60) as UTCTimestamp;
    };

    const getColor = (last: number, prev: number) => {
        if (last > prev) {
            return {
                line: "#ef4444",
                top: "rgba(239,68,68,0.35)",
                bottom: "rgba(239,68,68,0)",
            };
        }

        if (last < prev) {
            return {
                line: "#3b82f6",
                top: "rgba(59,130,246,0.35)",
                bottom: "rgba(59,130,246,0)",
            };
        }

        return {
            line: "#ffffff",
            top: "rgba(255,255,255,0.25)",
            bottom: "rgba(255,255,255,0)",
        };
    };

    useEffect(() => {
        if (!containerRef.current) return;

        const chart = createChart(containerRef.current, {
            autoSize: true,

            layout: {
                background: {
                    type: ColorType.Solid,
                    color: backgroundColor,
                },
                attributionLogo: false,
            },

            localization: {
                timeFormatter: (
                    time: UTCTimestamp
                ) => {
                    const date = new Date(
                        Number(time) * 1000
                    );

                    const hh = String(
                        date.getUTCHours()
                    ).padStart(2, "0");

                    const mm = String(
                        date.getUTCMinutes()
                    ).padStart(2, "0");

                    return `${hh}:${mm}`;
                },
            },

            grid: {
                vertLines: {
                    visible: false,
                },
                horzLines: {
                    visible: false,
                },
            },

            rightPriceScale: {
                visible: true,
                borderVisible: true,
                ticksVisible: false,
            },

            leftPriceScale: {
                visible: false,
            },

            timeScale: {
                visible: true,
                borderVisible: true,
                ticksVisible: false,

                timeVisible: true,
                secondsVisible: false,

                uniformDistribution: true,
                allowBoldLabels: false,

                tickMarkFormatter: (
                    time: UTCTimestamp
                ) => {
                    const date = new Date(
                        Number(time) * 1000
                    );

                    const hh = String(
                        date.getUTCHours()
                    ).padStart(2, "0");

                    const mm = String(
                        date.getUTCMinutes()
                    ).padStart(2, "0");

                    return `${hh}:${mm}`;
                },
            },

            crosshair: {
                vertLine: {
                    visible: false,
                    labelVisible: false,
                },
                horzLine: {
                    visible: false,
                    labelVisible: false,
                },
            },

            handleScroll: false,
            handleScale: false,
        });

        chartRef.current = chart;

        const last =
            data[data.length - 1]?.close ??
            prevClose;

        const color = getColor(
            last,
            prevClose
        );

        const area = chart.addSeries(
            AreaSeries,
            {
                lineColor: color.line,
                topColor: color.top,
                bottomColor: color.bottom,
                lineWidth: 2,

                priceLineVisible: false,
                lastValueVisible: false,

                priceFormat: {
                    type: "custom",
                    minMove: 1,
                    formatter: (price: number) =>
                        Math.round(price).toLocaleString(),
                },
            }
        );

        areaRef.current = area;

        const prevLine = chart.addSeries(
            LineSeries,
            {
                color: "#9ca3af",
                lineWidth: 1,
                lineStyle:
                LineStyle.Dashed,

                priceLineVisible: false,
                lastValueVisible: false,
            }
        );

        lineRef.current = prevLine;

        const horizontalData =
            data.map((c) => ({
                time: toKST(c.time),
                value: prevClose,
            }));

        prevLine.setData(horizontalData);


        return () => {
            chart.remove();
        };
    }, []);

    useEffect(() => {
        if (
            !areaRef.current ||
            !chartRef.current ||
            !lineRef.current
        )
            return;

        if (!data.length) return;

        const latestDate =
            data.reduce((max, cur) =>
                cur.time > max.time
                    ? cur
                    : max
            ).time;

        const filtered =
            data.filter((c) => {
                const day = Math.floor(
                    c.time / 86400
                );

                const latestDay =
                    Math.floor(
                        latestDate / 86400
                    );

                return (
                    day === latestDay
                );
            });

        const cleaned = filtered
            .filter(
                (c) =>
                    Number.isFinite(
                        c.time
                    ) &&
                    Number.isFinite(
                        c.close
                    )
            )
            .sort(
                (a, b) =>
                    a.time - b.time
            );

        const areaData =
            cleaned.map((c) => ({
                time: toKST(c.time),
                value: c.close,
            }));

        areaRef.current.setData(
            areaData
        );

        const last =
            cleaned[
            cleaned.length - 1
                ]?.close ?? prevClose;

        const color = getColor(
            last,
            prevClose
        );

        areaRef.current.applyOptions({
            lineColor: color.line,
            topColor: color.top,
            bottomColor: color.bottom,
        });

        const horizontalData =
            cleaned.map((c) => ({
                time: toKST(c.time),
                value: prevClose,
            }));

        lineRef.current.setData(
            horizontalData
        );

        chartRef.current
            .timeScale()
            .fitContent();
    }, [data, prevClose]);

    return (
        <div
            ref={containerRef}
            style={{
                width,
                height,
            }}
        />
    );
}
