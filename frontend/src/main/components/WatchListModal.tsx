import { useState, useEffect, useRef } from 'react';
import { FaTrash, FaSearch, FaPlus } from 'react-icons/fa';
import { STOCKS } from '../data/stocks';
import { useWatchList } from '../context/WatchListContext';
import './WatchListModal.css';

interface StockQuote {
    code: string;
    name: string;
    price: number;
    change: number;
    changeRate: number;
    volume: number;
}

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

function ChangeArrow({ change }: { change: number }) {
    if (change > 0) return <span className="wl-arrow up">▲</span>;
    if (change < 0) return <span className="wl-arrow down">▼</span>;
    return <span className="wl-arrow flat">-</span>;
}

export default function WatchListModal() {
    const { watchList, addStock, removeStock } = useWatchList();

    const [quotes, setQuotes] = useState<Record<string, StockQuote>>({});

    const [query, setQuery] = useState('');
    const searchRef = useRef<HTMLDivElement>(null);

    const [selected, setSelected] = useState<{ code: string; name: string } | null>(null);

    // ── 시세 폴링 (10초) — TODO: 실제 시세 API로 교체, 현재는 mock ──
    useEffect(() => {
        if (watchList.length === 0) return;

        const fetchQuotes = () => {
            const mock: Record<string, StockQuote> = {};
            watchList.forEach(({ stockCode, stockName }) => {
                const price = Math.floor(50000 + Math.random() * 100000);
                const change = Math.floor((Math.random() - 0.5) * 2000);
                mock[stockCode] = {
                    code: stockCode,
                    name: stockName,
                    price,
                    change,
                    changeRate: parseFloat(((change / price) * 100).toFixed(2)),
                    volume: Math.floor(Math.random() * 5_000_000),
                };
            });
            setQuotes(mock);
        };

        fetchQuotes();
        const id = setInterval(fetchQuotes, 10_000);
        return () => clearInterval(id);
    }, [watchList]);

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
                    // [수정] item.stockCode / item.stockName
                    const q = quotes[item.stockCode];
                    const up = q ? q.changeRate >= 0 && q.change !== 0 : null;
                    const flat = q ? q.change === 0 : false;
                    const dirClass = flat ? '' : up === true ? 'up' : up === false ? 'down' : '';

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
                                        {q ? q.price.toLocaleString() : '—'}
                                    </span>
                                    <span className={`wl-card-change ${dirClass}`}>
                                        {q ? (
                                            <>
                                                <ChangeArrow change={q.change} />
                                                {' '}
                                                {Math.abs(q.change).toLocaleString()}
                                                {' '}
                                                ({q.changeRate >= 0 ? '+' : ''}{q.changeRate}%)
                                            </>
                                        ) : '—'}
                                    </span>
                                </div>

                                <div className="wl-card-row wl-card-volume">
                                    <span>거래량 {q ? q.volume.toLocaleString() : '—'} 주</span>
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
