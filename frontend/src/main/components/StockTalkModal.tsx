import { useEffect, useRef, useState, useCallback } from "react";
import { useRealtime } from "../../main/context/RealtimeContext";
import { STOCKS } from "../../main/data/stocks";
import type { StockTalkMessage, StockTalkJoinResponse } from "../../types/stockTalk";

interface RoomState {
    joined: boolean;
    messages: StockTalkMessage[];
    participantCount: number;
}

const EMPTY_ROOM: RoomState = { joined: false, messages: [], participantCount: 0 };
const makeEmptyRooms = (): Record<string, RoomState> =>
    Object.fromEntries(STOCKS.map((s) => [s.code, { ...EMPTY_ROOM, messages: [] }]));

interface Props {
    open: boolean;
    onClose: () => void;
}

export default function StockTalkModal({ open, onClose }: Props) {
    const { subscribeDestination, publish, connected } = useRealtime();

    const [rooms, setRooms] = useState<Record<string, RoomState>>(makeEmptyRooms);
    const [activeTicker, setActiveTicker] = useState<string | null>(null);
    const [unread, setUnread] = useState<Record<string, number>>({});
    const activeTickerRef = useRef<string | null>(null);

    const updateRoom = useCallback((ticker: string, updater: (prev: RoomState) => RoomState) => {
        setRooms((prev) => ({
            ...prev,
            [ticker]: updater(prev[ticker] ?? { ...EMPTY_ROOM }),
        }));
    }, []);

    useEffect(() => {
        activeTickerRef.current = activeTicker;
        if (activeTicker) {
            setUnread((prev) => ({ ...prev, [activeTicker]: 0 }));
        }
    }, [activeTicker]);

    useEffect(() => {
        if (!open || !connected) return;

        return subscribeDestination("/user/sub/stock-talk/history", (data: StockTalkJoinResponse) => {
            updateRoom(data.ticker, (prev) => ({
                ...prev,
                joined: true,
                messages: data.messages,
                participantCount: data.participantCount,
            }));
        });
    }, [open, connected, subscribeDestination, updateRoom]);

    useEffect(() => {
        if (!open || !connected) return;

        const unsubs = STOCKS.map((stock) =>
            subscribeDestination(`/sub/stock-talk/${stock.code}`, (data: StockTalkMessage) => {
                updateRoom(stock.code, (prev) => ({
                    ...prev,
                    messages: [...prev.messages, data],
                    participantCount: data.participantCount,
                }));

                if (data.type === "CHAT" && activeTickerRef.current !== stock.code) {
                    setUnread((prev) => ({ ...prev, [stock.code]: (prev[stock.code] ?? 0) + 1 }));
                }
            })
        );

        return () => unsubs.forEach((u) => u());
    }, [open, connected, subscribeDestination, updateRoom]);

    useEffect(() => {
        if (open) return;

        setRooms((prev) => {
            STOCKS.forEach((stock) => {
                if (prev[stock.code]?.joined) {
                    publish(`/app/stock-talk/${stock.code}/leave`, {});
                }
            });
            return prev;
        });

        setRooms(makeEmptyRooms());
        setActiveTicker(null);
        setUnread({});
    }, [open, publish]);

    const handleJoin = useCallback((ticker: string) => {
        publish(`/app/stock-talk/${ticker}/join`, {});
        updateRoom(ticker, (prev) => ({ ...prev, joined: true }));
        setActiveTicker(ticker);
    }, [publish, updateRoom]);

    const handleLeave = useCallback((ticker: string) => {
        publish(`/app/stock-talk/${ticker}/leave`, {});
        updateRoom(ticker, (prev) => ({ ...prev, joined: false }));
        setActiveTicker((prev) => {
            if (prev !== ticker) return prev;
            const next = STOCKS.find((s) => s.code !== ticker && rooms[s.code]?.joined);
            return next?.code ?? null;
        });
    }, [publish, updateRoom, rooms]);

    const handleSend = useCallback((ticker: string, content: string) => {
        const text = content.trim();
        if (!text || !rooms[ticker]?.joined) return;
        publish(`/app/stock-talk/${ticker}/send`, { content: text });
    }, [publish, rooms]);

    // TODO: 다음 단계에서 JSX(탭 바/사이드바/채팅 패널) 붙일 예정
    return null;
}
