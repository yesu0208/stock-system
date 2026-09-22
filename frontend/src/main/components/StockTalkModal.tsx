import { useEffect, useRef, useState, useCallback } from "react";
import { FiSearch, FiMessageSquare, FiShare2, FiAward, FiTrendingUp } from "react-icons/fi";
import { useRealtime } from "../context/RealtimeContext";
import { useUser } from "../context/UserContext";
import { useAccount } from "../context/AccountContext";
import { STOCKS, stockNameMap } from "../data/stocks";
import { calcStockStats, DIRECTION_CLASS } from "../../utils/stockUtils";
import { resolveProfileImageUrl, DEFAULT_AVATAR } from "../../utils/image";
import type { StockTalkMessage, StockTalkJoinResponse } from "../../types/stockTalk";
import "./StockTalkModal.css";
import Tooltip from "../../tooltip/Tooltip";

const CHAT_STOCKS = STOCKS.filter((s) => s.realtimeSupported);

interface RoomState {
    joined: boolean;
    messages: StockTalkMessage[];
    participantCount: number;
}

const EMPTY_ROOM: RoomState = { joined: false, messages: [], participantCount: 0 };
const makeEmptyRooms = (): Record<string, RoomState> =>
    Object.fromEntries(CHAT_STOCKS.map((s) => [s.code, { ...EMPTY_ROOM, messages: [] }])); // [수정]

interface Props {
    open: boolean;
}

function formatTime(isoStr: string): string {
    try {
        const d = new Date(isoStr);
        const now = new Date();
        const isToday =
            d.getFullYear() === now.getFullYear() &&
            d.getMonth() === now.getMonth() &&
            d.getDate() === now.getDate();

        const h = String(d.getHours()).padStart(2, "0");
        const m = String(d.getMinutes()).padStart(2, "0");

        if (isToday) return `${h}:${m}`;

        const yy = String(d.getFullYear()).slice(2);
        const mo = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");
        return `${yy}.${mo}.${dd} ${h}:${m}`;
    } catch {
        return "";
    }
}

const AVATAR_EMOJIS = ["🐶", "🐱", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐸", "🐵", "🐔", "🐧", "🦄", "🐙", "🐢"];
const AVATAR_COLORS = ["#f87171", "#fb923c", "#fbbf24", "#a3e635", "#34d399", "#22d3ee", "#60a5fa", "#818cf8", "#a78bfa", "#f472b6", "#fb7185", "#2dd4bf"];

function hashString(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
        hash = (hash << 5) - hash + str.charCodeAt(i);
        hash |= 0;
    }
    return Math.abs(hash);
}

function getFallbackAvatar(username: string): { emoji: string; bg: string } {
    const h = hashString(username || "?");
    return {
        emoji: AVATAR_EMOJIS[h % AVATAR_EMOJIS.length],
        bg: AVATAR_COLORS[h % AVATAR_COLORS.length],
    };
}

const RANK_PREFIX = "[rank-card]";
const STOCK_PREFIX = "[stock-card]";

function parseCard(content: string) {
    if (content.startsWith(RANK_PREFIX)) {
        try { return { type: "rank" as const, data: JSON.parse(content.slice(RANK_PREFIX.length)) }; }
        catch { return null; }
    }
    if (content.startsWith(STOCK_PREFIX)) {
        try { return { type: "stock" as const, data: JSON.parse(content.slice(STOCK_PREFIX.length)) }; }
        catch { return null; }
    }
    return null;
}

const RANK_COLOR_MAP: Record<string, string> = {
    UNRANKED: "#64748b",
    BRONZE: "#b45309",
    SILVER: "#94a3b8",
    GOLD: "#fbbf24",
    PLATINUM: "#14b8a6",
    DIAMOND: "#3b82f6",
    MASTER: "#a855f7",
};

const ROMAN = ["", "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ"];
function formatRank(tier: string, subTier: number | null) {
    if (tier === "UNRANKED" || tier === "MASTER") return tier;
    return `${tier} ${subTier != null ? (ROMAN[subTier] ?? subTier) : ""}`.trim();
}

export default function StockTalkModal({ open }: Props) {
    const { subscribeDestination, subscribeStock, publish, connected } = useRealtime();
    const { user } = useUser();
    const { account } = useAccount();

    const [rooms, setRooms] = useState<Record<string, RoomState>>(makeEmptyRooms);
    const [activeTicker, setActiveTicker] = useState<string | null>(null);
    const [unread, setUnread] = useState<Record<string, number>>({});
    const [search, setSearch] = useState("");
    const [inputText, setInputText] = useState("");

    const activeTickerRef = useRef<string | null>(null);
    const joinedBarRef = useRef<HTMLDivElement>(null);
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const textareaRef = useRef<HTMLTextAreaElement>(null);
    const shareRef = useRef<HTMLDivElement>(null);

    const [activeTick, setActiveTick] = useState<any>(null);
    const [shareOpen, setShareOpen] = useState(false);
    const [stockPickOpen, setStockPickOpen] = useState(false);

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

        const unsubs = CHAT_STOCKS.map((stock) =>
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
        setActiveTick(null);
        if (!activeTicker) return;

        return subscribeStock(activeTicker, (tick: any) => {
            if (tick.tickMessageType === "TRADEPRICE" || tick.tickMessageType === "DETAIL") {
                setActiveTick(tick);
            }
        });
    }, [activeTicker, subscribeStock]);

    useEffect(() => {
        if (open) return;

        setRooms((prev) => {
            CHAT_STOCKS.forEach((stock) => {
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
        setInputText("");
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

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [activeTicker, rooms[activeTicker ?? ""]?.messages.length]);

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (shareRef.current && !shareRef.current.contains(e.target as Node)) {
                setShareOpen(false);
                setStockPickOpen(false);
            }
        };
        document.addEventListener("mousedown", handler);
        return () => document.removeEventListener("mousedown", handler);
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
            const next = CHAT_STOCKS.find((s) => s.code !== ticker && rooms[s.code]?.joined);
            return next?.code ?? null;
        });
    }, [publish, updateRoom, rooms]);

    const handleSend = useCallback(() => {
        if (!activeTicker) return;
        const text = inputText.trim();
        if (!text || !rooms[activeTicker]?.joined) return;
        publish(`/app/stock-talk/${activeTicker}/send`, { content: text });
        setInputText("");
        textareaRef.current?.focus();
    }, [publish, activeTicker, rooms, inputText]);

    const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleShareRank = useCallback(() => {
        if (!activeTicker || !user?.rank) return;
        const payload = JSON.stringify({
            tier: user.rank.tier,
            subTier: user.rank.subTier,
            rp: user.rank.rp,
            currentRankMinRp: user.rank.currentRankMinRp,
            nextRankMinRp: user.rank.nextRankMinRp,
            nickname: user.nickname,
        });
        publish(`/app/stock-talk/${activeTicker}/send`, { content: RANK_PREFIX + payload });
        setShareOpen(false);
    }, [activeTicker, user, publish]);

    const holdingEntries = account
        ? Object.entries(account.stocks).map(([code, info]) => ({
            stockCode: code,
            stockName: stockNameMap[code] ?? code,
            quantity: info.quantity,
            avgBuyPrice: info.quantity > 0 ? Math.round(info.totalAmount / info.quantity) : 0,
            currentPrice: account.currentPrices[code] ?? 0,
            profitAmount: account.profitAmounts[code] ?? 0,
            profitRate: account.profitRates[code] ?? 0,
        }))
        : [];

    const handleShareStock = useCallback((h: typeof holdingEntries[number]) => {
        if (!activeTicker) return;
        const payload = JSON.stringify({
            code: h.stockCode,
            name: h.stockName,
            qty: h.quantity,
            pnlRate: h.profitRate,
            evalPnl: h.profitAmount,
            avgBuyPrice: h.avgBuyPrice,
            currentPrice: h.currentPrice,
        });
        publish(`/app/stock-talk/${activeTicker}/send`, { content: STOCK_PREFIX + payload });
        setShareOpen(false);
        setStockPickOpen(false);
    }, [activeTicker, publish]);

    const filteredStocks = CHAT_STOCKS.filter((s) => {
        const keyword = search.toLowerCase();
        return s.name.toLowerCase().includes(keyword) || s.code.toLowerCase().includes(keyword);
    });

    const joinedStocks = CHAT_STOCKS.filter((s) => rooms[s.code]?.joined);
    const activeRoom = activeTicker ? (rooms[activeTicker] ?? EMPTY_ROOM) : null;
    const activeStock = activeTicker ? CHAT_STOCKS.find((s) => s.code === activeTicker) : null;

    const activeStats = activeTick
        ? activeTick.tickMessageType === "TRADEPRICE"
            ? calcStockStats(true, activeTick, null)
            : calcStockStats(false, null, { ...activeTick, prevClosePrice: activeTick.prevPrice })
        : null;

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
                    {!activeRoom || !activeStock ? (
                        <div className="stk__chat-placeholder">
                            <FiMessageSquare className="stk__chat-placeholder-icon" />
                            <span>종목톡에 입장하세요</span>
                        </div>
                    ) : (
                        <>
                            <div className="stk__chat-header">
                                <div className="stk__chat-stock-info">
                                    <div className="stk__chat-stock-left">
                                        <span className="stk__chat-name">{activeStock.name}</span>
                                        <span className="stk__chat-code">{activeStock.code}</span>
                                    </div>
                                    {activeStats && (
                                        <div
                                            className="stk__chat-stock-right"
                                            style={{
                                                color: DIRECTION_CLASS[activeStats.cur.direction] === "price-up"
                                                    ? "#f87171"
                                                    : DIRECTION_CLASS[activeStats.cur.direction] === "price-down"
                                                        ? "#60a5fa"
                                                        : "#94a3b8",
                                            }}
                                        >
                                            <span className="stk__chat-price">{activeStats.cur.price}</span>
                                            <span className="stk__chat-diff">
                                                {activeStats.cur.diffText}
                                                <span className="stk__chat-rate">({activeStats.cur.rateText})</span>
                                            </span>
                                        </div>
                                    )}
                                </div>
                                <span className="stk__chat-count">
                                    <span className="stk__chat-count-dot" />
                                    {activeRoom.participantCount}명
                                </span>
                            </div>

                            <div className="stk__messages">
                                {activeRoom.messages.length === 0 && (
                                    <div className="stk__msg-empty">
                                        <span>첫 메시지를 남겨보세요</span>
                                    </div>
                                )}
                                {activeRoom.messages.map((msg, idx) =>
                                    msg.type === "ENTER" || msg.type === "LEAVE" ? (
                                        <div key={idx} className="stk__msg--system">{msg.content}</div>
                                    ) : (() => {
                                        const isMine = msg.sender === user?.username;
                                        const fallback = getFallbackAvatar(msg.sender);
                                        const card = parseCard(msg.content);

                                        return (
                                            <div key={idx} className={`stk__msg ${isMine ? "stk__msg--mine" : ""}`}>
                                                {!isMine && (
                                                    msg.senderProfileImageUrl ? (
                                                        <img
                                                            className="stk__msg-avatar"
                                                            src={resolveProfileImageUrl(msg.senderProfileImageUrl)}
                                                            alt={msg.senderNickname}
                                                            title={msg.senderNickname}
                                                            onError={(e) => { e.currentTarget.src = DEFAULT_AVATAR; }}
                                                        />
                                                    ) : (
                                                        <span
                                                            className="stk__msg-avatar"
                                                            style={{ background: fallback.bg }}
                                                            title={msg.senderNickname}
                                                        >
                                                            {fallback.emoji}
                                                        </span>
                                                    )
                                                )}
                                                <div className="stk__msg-body">
                                                    <div className="stk__msg-meta">
                                                        {!isMine && (
                                                            <span className="stk__msg-sender">{msg.senderNickname}</span>
                                                        )}
                                                        <span className="stk__msg-time">{formatTime(msg.sentAt)}</span>
                                                    </div>

                                                    {card?.type === "rank" && (
                                                        <div className={`stk__card stk__card--rank ${isMine ? "stk__card--mine" : ""}`}>
                                                            <span className="stk__card-label">
                                                                <FiAward className="stk__share-icon" />
                                                                티어 자랑
                                                            </span>
                                                            <span className="stk__card-nick">{card.data.nickname}</span>
                                                            <span
                                                                className="stk__card-rank-badge"
                                                                style={{ background: RANK_COLOR_MAP[card.data.tier] ?? "#64748b" }}
                                                            >
                                                                {formatRank(card.data.tier, card.data.subTier)}
                                                            </span>
                                                            <div className="stk__card-rp-bar-wrap">
                                                                <div
                                                                    className="stk__card-rp-bar"
                                                                    style={{
                                                                        width: `${
                                                                            card.data.nextRankMinRp != null
                                                                                ? Math.min(100, ((card.data.rp - card.data.currentRankMinRp) /
                                                                                    (card.data.nextRankMinRp - card.data.currentRankMinRp)) * 100)
                                                                                : 100
                                                                        }%`,
                                                                        background: RANK_COLOR_MAP[card.data.tier] ?? "#fbbf24",
                                                                    }}
                                                                />
                                                            </div>
                                                            <span className="stk__card-rp-text">
                                                                {card.data.rp.toLocaleString()} RP
                                                            </span>
                                                        </div>
                                                    )}

                                                    {card?.type === "stock" && (
                                                        <div className={`stk__card stk__card--stock ${isMine ? "stk__card--mine" : ""}`}>
                                                            <span className="stk__card-label">
                                                                <FiTrendingUp className="stk__share-icon" />
                                                                보유종목 자랑
                                                            </span>
                                                            <div className="stk__card-stock-name">
                                                                {card.data.name}
                                                                <span className="stk__card-stock-code">{card.data.code}</span>
                                                            </div>
                                                            <div className="stk__card-stock-row">
                                                                <span className="stk__card-stock-key">현재가</span>
                                                                <span className="stk__card-stock-val">{card.data.currentPrice.toLocaleString()}원</span>
                                                            </div>
                                                            <div className="stk__card-stock-row">
                                                                <span className="stk__card-stock-key">매입가</span>
                                                                <span className="stk__card-stock-val">{card.data.avgBuyPrice.toLocaleString()}원</span>
                                                            </div>
                                                            <div className="stk__card-stock-row">
                                                                <span className="stk__card-stock-key">보유수량</span>
                                                                <span className="stk__card-stock-val">{card.data.qty.toLocaleString()}주</span>
                                                            </div>
                                                            <div className="stk__card-stock-row">
                                                                <span className="stk__card-stock-key">평가손익</span>
                                                                <span
                                                                    className="stk__card-stock-val"
                                                                    style={{ color: card.data.pnlRate >= 0 ? "#f87171" : "#60a5fa" }}
                                                                >
                                                                    {card.data.evalPnl >= 0 ? "+" : ""}{card.data.evalPnl.toLocaleString()}원
                                                                    ({card.data.pnlRate >= 0 ? "+" : ""}{card.data.pnlRate.toFixed(2)}%)
                                                                </span>
                                                            </div>
                                                        </div>
                                                    )}

                                                    {!card && (
                                                        <div className="stk__msg-bubble">{msg.content}</div>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })()
                                )}
                                <div ref={messagesEndRef} />
                            </div>

                            <div className="stk__input-area">
                                <div className="stk__share-wrap" ref={shareRef}>
                                    <Tooltip text="자랑하기" placement="top">
                                        <button
                                            className="stk__share-btn"
                                            onClick={() => { setShareOpen(v => !v); setStockPickOpen(false); }}
                                        >
                                            <FiShare2 className="stk__share-btn-icon" />
                                        </button>
                                    </Tooltip>

                                    {shareOpen && (
                                        <div className="stk__share-menu">
                                            {!stockPickOpen ? (
                                                <>
                                                    <button className="stk__share-item" onClick={handleShareRank}>
                                                        <span className="stk__share-item-inner">
                                                            <FiAward className="stk__share-icon" />
                                                            내 티어 자랑
                                                        </span>
                                                    </button>
                                                    <button className="stk__share-item" onClick={() => setStockPickOpen(true)}>
                                                        <span className="stk__share-item-inner">
                                                            <FiTrendingUp className="stk__share-icon" />
                                                            보유종목 자랑
                                                        </span>
                                                    </button>
                                                </>
                                            ) : (
                                                <>
                                                    <div className="stk__share-back" onClick={() => setStockPickOpen(false)}>
                                                        ← 돌아가기
                                                    </div>
                                                    <div className="stk__share-stock-list">
                                                        {holdingEntries.length === 0 ? (
                                                            <div className="stk__share-empty">보유 종목 없음</div>
                                                        ) : (
                                                            holdingEntries.map(h => (
                                                                <button
                                                                    key={h.stockCode}
                                                                    className="stk__share-item"
                                                                    onClick={() => handleShareStock(h)}
                                                                >
                                                                    <span>{h.stockName}</span>
                                                                    <span style={{ color: h.profitRate >= 0 ? "#f87171" : "#60a5fa", fontSize: 10 }}>
                                                                        {h.profitRate >= 0 ? "+" : ""}{h.profitRate.toFixed(2)}%
                                                                    </span>
                                                                </button>
                                                            ))
                                                        )}
                                                    </div>
                                                </>
                                            )}
                                        </div>
                                    )}
                                </div>

                                <textarea
                                    ref={textareaRef}
                                    className="stk__input"
                                    placeholder="메시지를 입력하세요 (Enter 전송)"
                                    value={inputText}
                                    onChange={(e) => setInputText(e.target.value)}
                                    onKeyDown={handleKeyDown}
                                    rows={1}
                                />
                                <button
                                    className="stk__send-btn"
                                    onClick={handleSend}
                                    disabled={!inputText.trim()}
                                >
                                    전송
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
