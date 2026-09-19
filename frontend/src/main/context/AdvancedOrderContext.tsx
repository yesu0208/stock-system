import {
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";
import { useRealtime } from "./RealtimeContext";
import { STOCKS } from "../data/stocks";

export type Side = "BUY" | "SELL";
export type EntryDirection = "ABOVE" | "BELOW";
export type ExitMode = "PRICE" | "PCT";

export interface TrailingStopPendingOrder {
    orderId: string;
    stockCode: string;
    stockName: string;
    side: Side;
    credit: boolean;
    leverage: number;
    quantity: number;
    stopPercent: number;
    basePrice: number;
    triggerPrice: number;
    createdAt: string;
}

export interface OtocoPendingOrder {
    orderId: string;
    stockCode: string;
    stockName: string;
    entryDirection: EntryDirection;
    triggerPrice: number;
    quantity: number;
    tpTriggerPrice: number | null;
    slTriggerPrice: number | null;
    credit: boolean;
    leverage: number;
    entryFilled: boolean;
    entryRemainingQuantity: number | null;
    createdAt: string;
}

type AdvancedOrderContextType = {
    trailingOrders: TrailingStopPendingOrder[];
    otocoOrders: OtocoPendingOrder[];
    connected: boolean;
};

const AdvancedOrderContext = createContext<AdvancedOrderContextType | undefined>(undefined);

function findStockName(stockCode: string): string {
    return STOCKS.find((s) => s.code === stockCode)?.name ?? stockCode;
}

function leverageRatioToNumber(leverageRatio: string | null | undefined): number {
    switch (leverageRatio) {
        case "X1_5": return 1.5;
        case "X2":   return 2;
        case "X2_5": return 2.5;
        default:     return 1;
    }
}

function isCreditLeverage(leverageRatio: string | null | undefined): boolean {
    return leverageRatio != null && leverageRatio !== "SPOT";
}

interface RawTrailingStopMessage {
    trailingStopId: number;
    stockCode: string;
    trailingStopType: Side;
    leverageRatio: string | null;
    orderQuantity: number;
    stopPercent: number;
    basePrice: number;
    triggerPrice: number | null;
    orderTime: string;
}

interface RawOtocoMessage {
    otocoId: number;
    stockCode: string;
    entryDirection: EntryDirection;
    leverageRatio: string | null;
    orderQuantity: number;
    entryTriggerPrice: number;
    tpTriggerPrice: number | null;
    slTriggerPrice: number | null;
    otocoStatus: "WAITING_ENTRY" | "ENTRY_ORDER_PLACED" | "WAITING_EXIT" | "COMPLETED" | "CANCELED";
    orderTime: string;
    entryRemainingQuantity: number | null;
}

function toTrailingStopPendingOrder(raw: RawTrailingStopMessage): TrailingStopPendingOrder {
    return {
        orderId: String(raw.trailingStopId),
        stockCode: raw.stockCode,
        stockName: findStockName(raw.stockCode),
        side: raw.trailingStopType,
        credit: isCreditLeverage(raw.leverageRatio),
        leverage: leverageRatioToNumber(raw.leverageRatio),
        quantity: raw.orderQuantity,
        stopPercent: raw.stopPercent,
        basePrice: raw.basePrice,
        triggerPrice: raw.triggerPrice ?? raw.basePrice,
        createdAt: raw.orderTime,
    };
}

function toOtocoPendingOrder(raw: RawOtocoMessage): OtocoPendingOrder {
    return {
        orderId: String(raw.otocoId),
        stockCode: raw.stockCode,
        stockName: findStockName(raw.stockCode),
        entryDirection: raw.entryDirection,
        triggerPrice: raw.entryTriggerPrice,
        quantity: raw.orderQuantity,
        tpTriggerPrice: raw.tpTriggerPrice,
        slTriggerPrice: raw.slTriggerPrice,
        credit: isCreditLeverage(raw.leverageRatio),
        leverage: leverageRatioToNumber(raw.leverageRatio),
        entryFilled: raw.otocoStatus === "WAITING_EXIT" || raw.otocoStatus === "COMPLETED",
        entryRemainingQuantity: raw.entryRemainingQuantity ?? null,
        createdAt: raw.orderTime,
    };
}

export function AdvancedOrderProvider({ children }: { children: ReactNode }) {
    const [trailingOrders, setTrailingOrders] = useState<TrailingStopPendingOrder[]>([]);
    const [otocoOrders, setOtocoOrders] = useState<OtocoPendingOrder[]>([]);

    const { subscribeDestination, connected } = useRealtime();

    useEffect(() => {
        const unsubTrailing = subscribeDestination(
            "/user/sub/trailing-stop",
            (data: RawTrailingStopMessage[]) => {
                setTrailingOrders(data.map(toTrailingStopPendingOrder));
            }
        );

        const unsubTrailingUpdate = subscribeDestination(
            "/user/sub/trailing-stop/update",
            (data: RawTrailingStopMessage) => {
                setTrailingOrders((prev) =>
                    prev.map((o) =>
                        o.orderId === String(data.trailingStopId)
                            ? toTrailingStopPendingOrder(data)
                            : o
                    )
                );
            }
        );

        const unsubOtoco = subscribeDestination(
            "/user/sub/otoco",
            (data: RawOtocoMessage[]) => {
                setOtocoOrders(data.map(toOtocoPendingOrder));
            }
        );

        const unsubOtocoUpdate = subscribeDestination(
            "/user/sub/otoco/update",
            (data: RawOtocoMessage) => {
                setOtocoOrders((prev) =>
                    prev.map((o) =>
                        o.orderId === String(data.otocoId)
                            ? toOtocoPendingOrder(data)
                            : o
                    )
                );
            }
        );

        return () => {
            unsubTrailing();
            unsubTrailingUpdate();
            unsubOtoco();
            unsubOtocoUpdate();
        };
    }, [subscribeDestination]);

    return (
        <AdvancedOrderContext.Provider value={{ trailingOrders, otocoOrders, connected }}>
            {children}
        </AdvancedOrderContext.Provider>
    );
}

export function useAdvancedOrders(): AdvancedOrderContextType {
    const context = useContext(AdvancedOrderContext);

    if (!context) {
        throw new Error("useAdvancedOrders must be used within AdvancedOrderProvider");
    }

    return context;
}
