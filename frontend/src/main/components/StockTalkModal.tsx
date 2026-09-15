import { useEffect, useRef, useState, useCallback } from "react";
import { FiSearch, FiMessageSquare } from "react-icons/fi";
import { useRealtime } from "../../main/context/RealtimeContext";
import { STOCKS } from "../../main/data/stocks";
import type { StockTalkMessage, StockTalkJoinResponse } from "../../types/stockTalk";
import "./StockTalkModal.css";

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
    const [search, setSearch] = useState("");
    const activeTickerRef = useRef<string | null>(null);
    const joinedBarRef = useRef<HTMLDivElement>(null);

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
        setSearch("");
    }, [open, publish]);

    useEffect(() => {
        const el = joinedBarRef.current;
        if (!el) return;
        const onWheel = (e: WheelEvent) => {
            if (e.deltaY === 0) return;
            e.preventDefault();
            el.scrollBy({ left: e.deltaY, behavior: "smooth" });
        };
        el.addEventListener("wheel", onWheel, { passive: false });
        return () => el.removeEventListener("wheel", onWheel);
    }, []);

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

    const filteredStocks = STOCKS.filter((s) => {
        const keyword = search.toLowerCase();
        return s.name.toLowerCase().includes(keyword) || s.code.toLowerCase().includes(keyword);
    });

    const joinedStocks = STOCKS.filter((s) => rooms[s.code]?.joined);

    return (
        <div className="stk">
            <span className="stk__joined-label">참여중인 종목톡</span>

            <div className="stk__joined-bar" ref={joinedBarRef}>
                {joinedStocks.length === 0 ? (
                    <span className="stk__joined-empty">입장한 톡방이 없습니다</span>
                ) : (
                    joinedStocks.map((stock) => (
                        <button
                            key={stock.code}
                            className={`stk__joined-tab ${activeTicker === stock.code ? "stk__joined-tab--active" : ""}`}
                            onClick={() => setActiveTicker(stock.code)}
                        >
                            <span className="stk__joined-dot" />
                            {stock.name}
                            <span className={`stk__joined-count${!(rooms[stock.code]?.participantCount) ? " stk__joined-count--zero" : ""}`}>
                                {rooms[stock.code]?.participantCount ?? 0}명
                            </span>
                            {(unread[stock.code] ?? 0) > 0 && (
                                <span className="stk__unread-badge">{unread[stock.code]}</span>
                            )}
                            <button
                                className="stk__joined-close"
                                onClick={(e) => { e.stopPropagation(); handleLeave(stock.code); }}
                            >✕</button>
                        </button>
                    ))
                )}
            </div>

            <div className="stk__body">
                <div className="stk__sidebar">
                    <div className="stk__search-wrap">
                        <FiSearch className="stk__search-icon" />
                        <input
                            className="stk__search"
                            placeholder="종목명·종목코드 검색"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>
                    <div className="stk__stock-list">
                        {filteredStocks.length === 0 && (
                            <div className="stk__stock-none">검색 결과 없음</div>
                        )}
                        {filteredStocks.map((stock) => {
                            const r = rooms[stock.code];
                            const isJoined = r?.joined;
                            const isActive = activeTicker === stock.code;
                            return (
                                <div
                                    key={stock.code}
                                    className={`stk__stock-item ${isActive ? "stk__stock-item--active" : ""}`}
                                    onClick={() => isJoined && setActiveTicker(stock.code)}
                                >
                                    <div className="stk__stock-info">
                                        <span className="stk__stock-name">{stock.name}</span>
                                        <span className="stk__stock-code">{stock.code}</span>
                                    </div>
                                    {isJoined && (unread[stock.code] ?? 0) > 0 && (
                                        <span className="stk__unread-badge">{unread[stock.code]}</span>
                                    )}
                                    <span className={`stk__stock-count${!(r?.participantCount) ? " stk__stock-count--zero" : ""}`}>
                                        {r?.participantCount ?? 0}명
                                    </span>
                                    {isJoined ? (
                                        <button
                                            className="stk__stock-btn stk__stock-btn--leave"
                                            onClick={(e) => { e.stopPropagation(); handleLeave(stock.code); }}
                                        >
                                            나가기
                                        </button>
                                    ) : (
                                        <button
                                            className="stk__stock-btn stk__stock-btn--join"
                                            onClick={(e) => { e.stopPropagation(); handleJoin(stock.code); }}
                                        >
                                            입장
                                        </button>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>

                <div className="stk__chat">
                    <div className="stk__chat-placeholder">
                        <FiMessageSquare className="stk__chat-placeholder-icon" />
                        <span>종목톡에 입장하세요</span>
                    </div>
                </div>
            </div>
        </div>
    );
}
