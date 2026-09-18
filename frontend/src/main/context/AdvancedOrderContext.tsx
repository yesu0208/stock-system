import {
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";
import { useRealtime } from "./RealtimeContext";

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
    tpMode: ExitMode;
    tpPrice: number | null;
    tpPct: number | null;
    slMode: ExitMode;
    slPrice: number | null;
    slPct: number | null;
    credit: boolean;
    leverage: number;
    entryFilled: boolean;
    createdAt: string;
}

type AdvancedOrderContextType = {
    trailingOrders: TrailingStopPendingOrder[];
    otocoOrders: OtocoPendingOrder[];
    connected: boolean;
};

const AdvancedOrderContext = createContext<AdvancedOrderContextType | undefined>(undefined);

export function AdvancedOrderProvider({ children }: { children: ReactNode }) {
    const [trailingOrders, setTrailingOrders] = useState<TrailingStopPendingOrder[]>([]);
    const [otocoOrders, setOtocoOrders] = useState<OtocoPendingOrder[]>([]);

    const { subscribeDestination, connected } = useRealtime();

    useEffect(() => {
        const unsubTrailing = subscribeDestination(
            "/user/sub/trailing-stop",
            (data: TrailingStopPendingOrder[]) => {
                setTrailingOrders(data);
            }
        );

        const unsubOtoco = subscribeDestination(
            "/user/sub/otoco",
            (data: OtocoPendingOrder[]) => {
                setOtocoOrders(data);
            }
        );

        return () => {
            unsubTrailing();
            unsubOtoco();
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
