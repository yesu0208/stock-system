import { useState, useEffect, useRef } from 'react';
import { FaTrash, FaSearch, FaPlus } from 'react-icons/fa';
import { STOCKS } from '../data/stocks';
import { useWatchList } from '../context/WatchListContext';
import { useRealtime } from '../context/RealtimeContext';
import { calcStockStats } from '../../utils/stockUtils';
import './WatchListModal.css';

function StockIcon({ code }: { code: string }) {
    return (
        <img
            src={`https://ssl.pstatic.net/imgstock/fn/real/logo/stock/Stock${code}.svg`}
            alt={code}
            className="stock-icon"
            onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.visibility = 'hidden';
            }}
        />
    );
}

export default function WatchListModal() {
    const { watchList, addStock, removeStock } = useWatchList();
    const { subscribeStock } = useRealtime();

    const [ticksByCode, setTicksByCode] = useState<Record<string, any>>({});

    const [query, setQuery] = useState('');
    const searchRef = useRef<HTMLDivElement>(null);

    const [selected, setSelected] = useState<{ code: string; name: string } | null>(null);

    useEffect(() => {
        const unsubs = watchList.map(({ stockCode }) =>
            subscribeStock(stockCode, (tick: any) => {
                if (tick.tickMessageType === 'TRADEPRICE' || tick.tickMessageType === 'DETAIL') {
                    setTicksByCode(prev => ({ ...prev, [stockCode]: tick }));
                }
            })
        );
        return () => unsubs.forEach(u => u());
    }, [watchList, subscribeStock]);

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
                setQuery('');
                setSelected(null);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    const toggleSelect = (code: string, name: string) => {
        setSelected(prev => (prev?.code === code ? null : { code, name }));
    };

    const handleAdd = async () => {
        if (!selected) return;
        try {
            await addStock(selected.code, selected.name);
            setQuery('');
            setSelected(null);
        } catch {
        }
    };

    const handleRemove = async (code: string) => {
        try {
            await removeStock(code);
        } catch {
        }
    };

    const handleRemoveFromSearch = async () => {
        if (!selected) return;
        try {
            await removeStock(selected.code);
            setQuery('');
            setSelected(null);
        } catch {
        }
    };

    const watchedCodes = new Set(watchList.map(w => w.stockCode));
    const q = query.trim().toLowerCase();
    const filtered = q
        ? STOCKS.filter(
            s => s.name.toLowerCase().includes(q) || s.code.toLowerCase().includes(q)
        ).slice(0, 8)
        : [];

    const selectedIsWatched = !!selected && watchedCodes.has(selected.code);

    return (
        <div className="watchlist-modal">
            <div className="wl-toolbar" ref={searchRef}>
                <div className="wl-search-box">
                    <FaSearch className="wl-search-icon" />
                    <input
                        className="wl-search-input"
                        placeholder="종목명·종목코드 검색"
                        value={query}
                        onChange={e => {
                            setQuery(e.target.value);
                            setSelected(null);
                        }}
                    />
                    {filtered.length > 0 && (
                        <ul className="wl-search-results">
                            {filtered.map(s => {
                                const isWatched = watchedCodes.has(s.code);
                                return (
                                    <li
                                        key={s.code}
                                        className={`wl-search-result-item${selected?.code === s.code ? ' selected' : ''}${isWatched ? ' watched' : ''}`}
                                        onClick={() => toggleSelect(s.code, s.name)}
                                    >
                                        <span className="wl-sr-name">{s.name}</span>
                                        <span className="wl-sr-right">
                                            {isWatched && <span className="wl-sr-badge">추가됨</span>}
                                            <span className="wl-sr-code">{s.code}</span>
                                        </span>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                    {query.trim() && filtered.length === 0 && (
                        <div className="wl-search-empty">검색 결과 없음</div>
                    )}
                </div>
                <button
                    className={`wl-search-toggle${selectedIsWatched ? ' wl-search-toggle--remove' : ''}`}
                    disabled={!selected}
                    onClick={selectedIsWatched ? handleRemoveFromSearch : handleAdd}
                >
                    {selectedIsWatched ? (
                        <>
                            <FaTrash className="wl-search-toggle-icon" />
                            종목 제거
                        </>
                    ) : (
                        <>
                            <FaPlus className="wl-search-toggle-icon" />
                            종목 추가
                        </>
                    )}
                </button>
            </div>

            <ul className="watchlist-list">
                {watchList.length === 0 && (
                    <li className="wl-empty">관심종목을 추가해보세요.</li>
                )}
                {watchList.map(item => {
                    const tick = ticksByCode[item.stockCode];

                    const stats = tick
                        ? tick.tickMessageType === 'TRADEPRICE'
                            ? calcStockStats(true, tick, null)
                            : calcStockStats(false, null, { ...tick, prevClosePrice: tick.prevPrice })
                        : null;

                    const dirClass = stats
                        ? stats.cur.direction === 'up' || stats.cur.direction === 'upperLimit' ? 'up'
                            : stats.cur.direction === 'down' || stats.cur.direction === 'lowerLimit' ? 'down'
                                : ''
                        : '';

                    return (
                        <li key={item.stockCode} className="watchlist-card">
                            <button
                                className="remove-btn"
                                onClick={() => handleRemove(item.stockCode)}
                            >
                                <FaTrash />
                            </button>

                            <StockIcon code={item.stockCode} />

                            <div className="wl-card-body">
                                <div className="wl-card-row wl-card-head">
                                    <span className="item-name">{item.stockName}</span>
                                    <span className="item-code">{item.stockCode}</span>
                                </div>

                                <div className="wl-card-row wl-card-price">
                                    <span className="wl-price-value">
                                        {stats ? stats.cur.price : '—'}
                                    </span>
                                    <span className={`wl-card-change ${dirClass}`}>
                                        {stats ? (
                                            <>
                                                {stats.cur.diffText}
                                                {stats.cur.rateText && <> ({stats.cur.rateText})</>}
                                            </>
                                        ) : '—'}
                                    </span>
                                </div>

                                <div className="wl-card-row wl-card-volume">
                                    <span>거래량 {stats ? stats.volume : '—'} 주</span>
                                </div>
                            </div>
                        </li>
                    );
                })}
            </ul>

            <div className="wl-count">{watchList.length}개 종목</div>
        </div>
    );
}
