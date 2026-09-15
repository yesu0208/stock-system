import { useState, useEffect, useCallback, useRef } from 'react';
import { FaThumbsUp, FaThumbsDown, FaBookmark, FaRegBookmark, FaUserCircle } from 'react-icons/fa';
import { FiMessageSquare, FiChevronLeft, FiSearch, FiX, FiChevronDown, FiEdit, FiTrash2 } from 'react-icons/fi';
import { useStock } from '../context/StockContext';
import { useUser } from '../context/UserContext';
import { useRealtime } from '../context/RealtimeContext';
import { STOCKS } from '../data/stocks';
import { calcStockStats } from '../../utils/stockUtils';
import * as discussionApi from '../../api/discussion';
import type { PostSummary, PostDetail, ReactionType } from '../../types/discussion';
import './DiscussionModal.css';
import Tooltip from '../../tooltip/Tooltip';

const MAX_TITLE_LENGTH = 50;
const MAX_CONTENT_LENGTH = 500;

type Tab = 'list' | 'my-posts' | 'my-comments' | 'my-scraps';

function sentimentColor(likes: number, dislikes: number): string {
    if (likes > dislikes) return '#f87171';
    if (dislikes > likes) return '#60a5fa';
    return '#334155';
}

function formatDateTime(iso: string): { date: string; time: string } {
    const d = new Date(iso);
    const yy = String(d.getFullYear()).slice(2);
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');
    const ss = String(d.getSeconds()).padStart(2, '0');
    return { date: `${yy}.${mm}.${dd}`, time: `${hh}:${mi}:${ss}` };
}

function Avatar({ src, alt }: { src: string | null | undefined; alt: string }) {
    const [failed, setFailed] = useState(false);
    if (!src || failed) {
        return <FaUserCircle className="d-avatar d-avatar--fallback" aria-hidden="true" />;
    }
    return <img className="d-avatar" src={src} alt={alt} onError={() => setFailed(true)} />;
}

function StockBadge({ stockName, stockCode }: { stockName: string; stockCode: string }) {
    return (
        <span className="d-stock-badge" title={stockCode}>
            {stockName}
            <span className="d-stock-code">{stockCode}</span>
        </span>
    );
}

function StockInfoHeader() {
    const { selectedStock } = useStock();
    const { subscribeStock } = useRealtime();
    const [tick, setTick] = useState<any>(null);

    useEffect(() => {
        setTick(null);
        return subscribeStock(selectedStock.code, (t: any) => {
            if (t.tickMessageType === 'TRADEPRICE' || t.tickMessageType === 'DETAIL') {
                setTick(t);
            }
        });
    }, [selectedStock.code, subscribeStock]);

    const stats = tick
        ? tick.tickMessageType === 'TRADEPRICE'
            ? calcStockStats(true, tick, null)
            : calcStockStats(false, null, { ...tick, prevClosePrice: tick.prevPrice })
        : null;

    const priceColor = !stats ? '#94a3b8'
        : stats.cur.direction === 'up' || stats.cur.direction === 'upperLimit' ? '#f87171'
            : stats.cur.direction === 'down' || stats.cur.direction === 'lowerLimit' ? '#60a5fa'
                : '#94a3b8';

    return (
        <div className="stock-info-header">
            <div className="sih-left">
                <span className="sih-name">{selectedStock.name}</span>
                <span className="sih-code">{selectedStock.code}</span>
            </div>
            {stats && (
                <div className="sih-right" style={{ color: priceColor }}>
                    <span className="sih-price">{stats.cur.price}</span>
                    <span className="sih-diff">
                        {stats.cur.diffText}
                        <span className="sih-rate">({stats.cur.rateText})</span>
                    </span>
                </div>
            )}
        </div>
    );
}

function StockSelectorRow({ onWrite }: { onWrite: () => void }) {
    const { selectedStock, setSelectedStock } = useStock();
    const [open, setOpen] = useState(false);
    const [query, setQuery] = useState('');
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const handler = (e: MouseEvent) => {
            if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    useEffect(() => {
        if (!open) setQuery('');
    }, [open]);

    const normalizedQuery = query.trim().toLowerCase();
    const filteredStocks = STOCKS.filter(s =>
        s.name.toLowerCase().includes(normalizedQuery) ||
        s.code.toLowerCase().includes(normalizedQuery)
    );

    return (
        <div className="stock-selector-row">
            <div className="stock-selector" ref={ref}>
                <button className="stock-selector-trigger" onClick={() => setOpen(v => !v)}>
                    <span>{selectedStock.name}</span>
                    <FiChevronDown className={open ? 'rotated' : ''} />
                </button>

                {open && (
                    <div className="stock-selector-dropdown">
                        <div className="stock-selector-search">
                            <FiSearch />
                            <input
                                placeholder="종목명·종목코드 검색"
                                value={query}
                                onChange={e => setQuery(e.target.value)}
                                autoFocus
                            />
                        </div>
                        <ul className="stock-selector-menu">
                            {filteredStocks.length === 0 && (
                                <li className="stock-selector-empty">검색 결과가 없습니다</li>
                            )}
                            {filteredStocks.map(s => (
                                <li
                                    key={s.code}
                                    className={`stock-selector-item${s.code === selectedStock.code ? ' active' : ''}`}
                                    onClick={() => { setSelectedStock(s); setOpen(false); }}
                                >
                                    <span className="ssi-name">{s.name}</span>
                                    <span className="ssi-code">{s.code}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>

            <button className="write-btn" onClick={onWrite}>
                <FiEdit />
                글쓰기
            </button>
        </div>
    );
}

function StockSearchBar({ value, onChange, onClear }: { value: string; onChange: (v: string) => void; onClear: () => void }) {
    return (
        <div className="stock-search-bar">
            <FiSearch className="ssb-icon" />
            <input
                className="ssb-input"
                placeholder="종목명·종목코드 검색"
                value={value}
                onChange={e => onChange(e.target.value)}
            />
            {value && (
                <button className="ssb-clear" onClick={onClear}>
                    <FiX />
                </button>
            )}
        </div>
    );
}

interface PostCardProps {
    post: PostSummary;
    onSelect: (id: number) => void;
    onReact: (postId: number, type: ReactionType, e: React.MouseEvent) => void;
    onScrap: (postId: number, e: React.MouseEvent) => void;
}

function PostCard({ post, onSelect, onReact, onScrap }: PostCardProps) {
    const dt = formatDateTime(post.createdDateTime);

    return (
        <li className="discussion-item" onClick={() => onSelect(post.postId)}>
            <span className="sentiment-bar" style={{ background: sentimentColor(post.likes, post.dislikes) }} />
            <div className="discussion-meta">
                <Avatar src={post.authorProfileImageUrl} alt={post.authorNickname} />
                <span className="d-author">{post.authorNickname}</span>
                <StockBadge stockName={post.stockName} stockCode={post.stockCode} />
                <span className="d-time">
                    <span className="d-time-date">{dt.date}</span>
                    <span className="d-time-clock">{dt.time}</span>
                </span>
            </div>
            <p className="d-title">{post.title}</p>
            <p className="d-content">{post.contentPreview}</p>
            <div className="d-actions">
                <Tooltip text="좋아요" placement="top">
                    <button
                        className={`d-btn like${post.myReaction === 'LIKE' ? ' active' : ''}`}
                        onClick={e => onReact(post.postId, 'LIKE', e)}
                    >
                        <FaThumbsUp /> {post.likes}
                    </button>
                </Tooltip>
                <Tooltip text="싫어요" placement="top">
                    <button
                        className={`d-btn dislike${post.myReaction === 'DISLIKE' ? ' active' : ''}`}
                        onClick={e => onReact(post.postId, 'DISLIKE', e)}
                    >
                        <FaThumbsDown /> {post.dislikes}
                    </button>
                </Tooltip>
                <Tooltip text="스크랩" placement="top">
                    <button
                        className={`d-btn scrap${post.myScrapped ? ' active' : ''}`}
                        onClick={e => onScrap(post.postId, e)}
                    >
                        {post.myScrapped ? <FaBookmark /> : <FaRegBookmark />}
                        {post.scraps}
                    </button>
                </Tooltip>
                <span className="d-comment-count">
                    <FiMessageSquare /> {post.commentCount}
                </span>
            </div>
        </li>
    );
}

export default function DiscussionModal() {
    const { selectedStock } = useStock();
    const { user } = useUser();

    const [tab, setTab] = useState<Tab>('list');
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [isWriting, setIsWriting] = useState(false);
    const [isEditingPost, setIsEditingPost] = useState(false);

    const [posts, setPosts] = useState<PostSummary[]>([]);
    const [nextCursor, setNextCursor] = useState<number | null>(null);
    const [hasNext, setHasNext] = useState(false);
    const [listLoading, setListLoading] = useState(false);

    const [detail, setDetail] = useState<PostDetail | null>(null);
    const [detailLoading, setDetailLoading] = useState(false);

    const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
    const [editingCommentText, setEditingCommentText] = useState('');

    const [searchQuery, setSearchQuery] = useState('');
    const [commentInput, setCommentInput] = useState('');

    const sentinelRef = useRef<HTMLLIElement>(null);

    const loadPosts = useCallback(async (cursor: number | null = null) => {
        setListLoading(true);
        try {
            const page = tab === 'list'
                ? await discussionApi.getPostsByStock(selectedStock.code, cursor ?? undefined)
                : tab === 'my-posts'
                    ? await discussionApi.getMyPosts(cursor ?? undefined)
                    : tab === 'my-comments'
                        ? await discussionApi.getPostsICommentedOn(cursor ?? undefined)
                        : await discussionApi.getScrappedPosts(cursor ?? undefined);

            setPosts(prev => cursor === null ? page.items : [...prev, ...page.items]);
            setNextCursor(page.nextCursor);
            setHasNext(page.hasNext);
        } catch (e) {
            console.error('게시글 목록 조회 실패', e);
        } finally {
            setListLoading(false);
        }
    }, [tab, selectedStock.code]);

    useEffect(() => {
        setSelectedId(null);
        setDetail(null);
        setIsWriting(false);
        setIsEditingPost(false);
        setEditingCommentId(null);
        setSearchQuery('');
        setPosts([]);
        setNextCursor(null);
        loadPosts(null);
    }, [tab, selectedStock.code]);

    useEffect(() => {
        const el = sentinelRef.current;
        if (!el) return;

        const observer = new IntersectionObserver(
            ([entry]) => {
                if (entry.isIntersecting && hasNext && !listLoading && nextCursor !== null) {
                    loadPosts(nextCursor);
                }
            },
            { threshold: 0.1 }
        );

        observer.observe(el);
        return () => observer.disconnect();
    }, [hasNext, listLoading, nextCursor, loadPosts]);

    const handlePostReact = async (postId: number, type: ReactionType, e: React.MouseEvent) => {
        e.stopPropagation();
        const target = posts.find(p => p.postId === postId) ?? (detail?.postId === postId ? detail : null);
        const current = target?.myReaction ?? null;

        setPosts(prev => prev.map(p => p.postId !== postId ? p : { ...p, myReaction: current === type ? null : type }));
        setDetail(prev => prev?.postId !== postId ? prev : { ...prev, myReaction: current === type ? null : type });

        try {
            const data = await discussionApi.reactToPost(postId, type);
            setPosts(prev => prev.map(p => p.postId !== postId ? p : { ...p, likes: data.likes, dislikes: data.dislikes }));
            setDetail(prev => prev?.postId !== postId ? prev : { ...prev, likes: data.likes, dislikes: data.dislikes });
        } catch (err) {
            console.error('게시글 반응 실패', err);
            setPosts(prev => prev.map(p => p.postId !== postId ? p : { ...p, myReaction: current }));
            setDetail(prev => prev?.postId !== postId ? prev : { ...prev, myReaction: current });
        }
    };

    const handleScrap = async (postId: number, e: React.MouseEvent) => {
        e.stopPropagation();
        const target = posts.find(p => p.postId === postId) ?? (detail?.postId === postId ? detail : null);
        const wasScrapped = target?.myScrapped ?? false;

        setPosts(prev => prev.map(p => p.postId !== postId ? p : { ...p, myScrapped: !wasScrapped }));
        setDetail(prev => prev?.postId !== postId ? prev : { ...prev, myScrapped: !wasScrapped });

        try {
            const data = await discussionApi.toggleScrap(postId);
            setPosts(prev => prev.map(p => p.postId !== postId ? p : { ...p, scraps: data.scraps, myScrapped: data.scrapped }));
            setDetail(prev => prev?.postId !== postId ? prev : { ...prev, scraps: data.scraps, myScrapped: data.scrapped });

            if (!data.scrapped && wasScrapped && tab === 'my-scraps') {
                setPosts(prev => prev.filter(p => p.postId !== postId));
            }
        } catch (err) {
            console.error('스크랩 실패', err);
            setPosts(prev => prev.map(p => p.postId !== postId ? p : { ...p, myScrapped: wasScrapped }));
            setDetail(prev => prev?.postId !== postId ? prev : { ...prev, myScrapped: wasScrapped });
        }
    };

    const q = searchQuery.trim().toLowerCase();
    const visiblePosts = tab === 'list' || !q
        ? posts
        : posts.filter(p => p.stockName.toLowerCase().includes(q) || p.stockCode.toLowerCase().includes(q));

    return (
        <div className="discussion-modal">
            <div className="discussion-tabs">
                {(['list', 'my-posts', 'my-comments', 'my-scraps'] as Tab[]).map(t => (
                    <button
                        key={t}
                        className={`discussion-tab${tab === t ? ' active' : ''}`}
                        onClick={() => { setTab(t); setSelectedId(null); setIsWriting(false); setIsEditingPost(false); }}
                    >
                        {t === 'list' ? '전체글' :
                            t === 'my-posts' ? '내 게시글' :
                                t === 'my-comments' ? '내 댓글' : '스크랩'}
                    </button>
                ))}
            </div>

            {/* TODO: 상세 뷰, 글쓰기/수정 뷰 연결 예정 — 지금은 목록 뷰만 표시 */}
            <div className="discussion-list-view">
                {tab === 'list' && (
                    <div className="list-header-block">
                        <StockSelectorRow onWrite={() => setIsWriting(true)} />
                        <StockInfoHeader />
                    </div>
                )}

                {tab !== 'list' && (
                    <StockSearchBar
                        value={searchQuery}
                        onChange={setSearchQuery}
                        onClear={() => setSearchQuery('')}
                    />
                )}

                <ul className="discussion-list">
                    {listLoading && posts.length === 0 && (
                        <li className="discussion-empty"><span>불러오는 중...</span></li>
                    )}
                    {!listLoading && visiblePosts.length === 0 && (
                        <li className="discussion-empty">
                            <FiMessageSquare />
                            <span>
                                {searchQuery.trim() ? `"${searchQuery}" 에 해당하는 글이 없습니다` : '게시글이 없습니다'}
                            </span>
                        </li>
                    )}
                    {visiblePosts.map(post => (
                        <PostCard
                            key={post.postId}
                            post={post}
                            onSelect={setSelectedId}
                            onReact={handlePostReact}
                            onScrap={handleScrap}
                        />
                    ))}

                    {listLoading && posts.length > 0 && (
                        <li className="discussion-empty" style={{ minHeight: 32 }}>
                            <span style={{ fontSize: 11 }}>로딩 중...</span>
                        </li>
                    )}
                    {!hasNext && posts.length > 0 && !listLoading && (
                        <li className="discussion-empty" style={{ minHeight: 28 }}>
                            <span style={{ fontSize: 11, color: '#64748b' }}>마지막 글입니다</span>
                        </li>
                    )}

                    <li ref={sentinelRef} style={{ height: 1, listStyle: 'none' }} aria-hidden />
                </ul>
            </div>
        </div>
    );
}
