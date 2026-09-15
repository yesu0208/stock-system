import { useState, useEffect, useCallback, useRef } from 'react';
import { FaThumbsUp, FaThumbsDown, FaBookmark, FaRegBookmark, FaUserCircle } from 'react-icons/fa';
import { FiMessageSquare, FiChevronLeft, FiSearch, FiX, FiChevronDown, FiEdit, FiTrash2 } from 'react-icons/fi';
import { useStock } from '../context/StockContext';
import { useUser } from '../context/UserContext';
import { useRealtime } from '../context/RealtimeContext';
import { STOCKS } from '../data/stocks';
import { calcStockStats } from '../../utils/stockUtils';
import * as discussionApi from '../../api/discussion';
import type { PostSummary, PostDetail, CommentResponse, ReactionType } from '../../types/discussion';
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

function CommentInputRow({ value, onChange, onSend }: { value: string; onChange: (v: string) => void; onSend: () => void }) {
    return (
        <div className="discussion-input-row">
            <div className="input-wrapper">
                <input
                    className="discussion-input"
                    placeholder="댓글을 남겨보세요"
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    onKeyDown={e => e.key === 'Enter' && onSend()}
                    maxLength={100}
                />
                <span className="input-char-count">{value.length}/100</span>
            </div>
            <button className="discussion-send" onClick={onSend} disabled={!value.trim()}>
                댓글
            </button>
        </div>
    );
}

interface WriteViewProps {
    stockName: string;
    stockCode: string;
    onSubmit: (title: string, content: string) => Promise<void>;
    onCancel: () => void;
}

function WriteView({ stockName, stockCode, onSubmit, onCancel }: WriteViewProps) {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [submitting, setSubmitting] = useState(false);

    const canSubmit = title.trim().length > 0 && content.trim().length > 0 && !submitting;

    const handleSubmit = async () => {
        if (!canSubmit) return;
        setSubmitting(true);
        try {
            await onSubmit(title.trim(), content.trim());
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="write-view">
            <button className="detail-back" onClick={onCancel}>
                <FiChevronLeft /> 목록으로
            </button>

            <div className="write-form">
                <div className="write-stock-badge">
                    <StockBadge stockName={stockName} stockCode={stockCode} />
                </div>

                <div className="write-field">
                    <div className="write-field-header">
                        <label className="write-label">제목</label>
                        <span className="write-char-count">{title.length}/{MAX_TITLE_LENGTH}</span>
                    </div>
                    <input
                        className="write-title-input"
                        placeholder="제목을 입력하세요"
                        value={title}
                        maxLength={MAX_TITLE_LENGTH}
                        onChange={e => setTitle(e.target.value)}
                    />
                </div>

                <div className="write-field write-field--content">
                    <div className="write-field-header">
                        <label className="write-label">내용</label>
                        <span className="write-char-count">{content.length}/{MAX_CONTENT_LENGTH}</span>
                    </div>
                    <textarea
                        className="write-content-input"
                        placeholder="내용을 입력하세요"
                        value={content}
                        maxLength={MAX_CONTENT_LENGTH}
                        onChange={e => setContent(e.target.value)}
                    />
                </div>

                <div className="write-actions">
                    <button className="write-cancel-btn" onClick={onCancel}>취소</button>
                    <button className="write-submit-btn" onClick={handleSubmit} disabled={!canSubmit}>
                        {submitting ? '등록 중...' : '등록'}
                    </button>
                </div>
            </div>
        </div>
    );
}

interface EditPostViewProps {
    stockName: string;
    stockCode: string;
    initialTitle: string;
    initialContent: string;
    onSubmit: (title: string, content: string) => Promise<void>;
    onCancel: () => void;
}

function EditPostView({ stockName, stockCode, initialTitle, initialContent, onSubmit, onCancel }: EditPostViewProps) {
    const [title, setTitle] = useState(initialTitle);
    const [content, setContent] = useState(initialContent);
    const [submitting, setSubmitting] = useState(false);

    const canSubmit =
        title.trim().length > 0 &&
        content.trim().length > 0 &&
        (title.trim() !== initialTitle || content.trim() !== initialContent) &&
        !submitting;

    const handleSubmit = async () => {
        if (!canSubmit) return;
        setSubmitting(true);
        try {
            await onSubmit(title.trim(), content.trim());
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="write-view">
            <button className="detail-back" onClick={onCancel}>
                <FiChevronLeft /> 돌아가기
            </button>

            <div className="write-form">
                <div className="write-stock-badge">
                    <StockBadge stockName={stockName} stockCode={stockCode} />
                </div>

                <div className="write-field">
                    <div className="write-field-header">
                        <label className="write-label">제목</label>
                        <span className="write-char-count">{title.length}/{MAX_TITLE_LENGTH}</span>
                    </div>
                    <input
                        className="write-title-input"
                        value={title}
                        maxLength={MAX_TITLE_LENGTH}
                        onChange={e => setTitle(e.target.value)}
                    />
                </div>

                <div className="write-field write-field--content">
                    <div className="write-field-header">
                        <label className="write-label">내용</label>
                        <span className="write-char-count">{content.length}/{MAX_CONTENT_LENGTH}</span>
                    </div>
                    <textarea
                        className="write-content-input"
                        value={content}
                        maxLength={MAX_CONTENT_LENGTH}
                        onChange={e => setContent(e.target.value)}
                    />
                </div>

                <div className="write-actions">
                    <button className="write-cancel-btn" onClick={onCancel}>취소</button>
                    <button className="write-submit-btn" onClick={handleSubmit} disabled={!canSubmit}>
                        {submitting ? '수정 중...' : '수정 완료'}
                    </button>
                </div>
            </div>
        </div>
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

    const loadDetail = useCallback(async (postId: number) => {
        setDetailLoading(true);
        try {
            const data = await discussionApi.getPost(postId);
            setDetail(data);
        } catch (e) {
            console.error('게시글 상세 조회 실패', e);
        } finally {
            setDetailLoading(false);
        }
    }, []);

    useEffect(() => {
        if (selectedId !== null) loadDetail(selectedId);
        else setDetail(null);
    }, [selectedId, loadDetail]);

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

    const handleCommentReact = async (postId: number, commentId: number, type: ReactionType) => {
        const comment = detail?.comments.find(c => c.commentId === commentId);
        const current = comment?.myReaction ?? null;

        setDetail(prev => {
            if (!prev) return prev;
            return {
                ...prev,
                comments: prev.comments.map(c =>
                    c.commentId !== commentId ? c : { ...c, myReaction: current === type ? null : type }
                ),
            };
        });

        try {
            const data = await discussionApi.reactToComment(postId, commentId, type);
            setDetail(prev => {
                if (!prev) return prev;
                return {
                    ...prev,
                    comments: prev.comments.map(c =>
                        c.commentId !== commentId ? c : { ...c, likes: data.likes, dislikes: data.dislikes }
                    ),
                };
            });
        } catch (err) {
            console.error('댓글 반응 실패', err);
            setDetail(prev => {
                if (!prev) return prev;
                return {
                    ...prev,
                    comments: prev.comments.map(c => c.commentId !== commentId ? c : { ...c, myReaction: current }),
                };
            });
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

    const handleCommentSend = async () => {
        if (!commentInput.trim() || selectedId === null) return;
        try {
            const newComment: CommentResponse = await discussionApi.addComment(selectedId, commentInput.trim());
            setDetail(prev => prev ? { ...prev, comments: [...prev.comments, newComment] } : prev);
            setPosts(prev => prev.map(p =>
                p.postId !== selectedId ? p : { ...p, commentCount: p.commentCount + 1 }
            ));
            setCommentInput('');
        } catch (e) {
            console.error('댓글 등록 실패', e);
        }
    };

    const handlePostSubmit = async (title: string, content: string) => {
        try {
            const newPost = await discussionApi.createPost({
                stockCode: selectedStock.code,
                stockName: selectedStock.name,
                title,
                content,
            });

            const newSummary: PostSummary = {
                postId: newPost.postId,
                stockCode: newPost.stockCode,
                stockName: newPost.stockName,
                title: newPost.title,
                authorId: newPost.authorId,
                authorNickname: newPost.authorNickname,
                authorProfileImageUrl: newPost.authorProfileImageUrl,
                createdDateTime: newPost.createdDateTime,
                contentPreview: newPost.content.slice(0, 100),
                likes: 0,
                dislikes: 0,
                commentCount: 0,
                scraps: 0,
                myReaction: null,
                myScrapped: false,
            };

            setPosts(prev => [newSummary, ...prev]);
            setIsWriting(false);
        } catch (e) {
            console.error('게시글 등록 실패', e);
        }
    };

    const handlePostEdit = async (title: string, content: string) => {
        if (!detail) return;
        try {
            const updated = await discussionApi.editPost(detail.postId, title, content);
            setDetail(updated);
            setPosts(prev => prev.map(p =>
                p.postId !== updated.postId ? p : {
                    ...p,
                    title: updated.title,
                    contentPreview: updated.content.slice(0, 100),
                }
            ));
            setIsEditingPost(false);
        } catch (e) {
            console.error('게시글 수정 실패', e);
        }
    };

    const handlePostDelete = async (postId: number) => {
        if (!window.confirm('게시글을 삭제하시겠습니까?')) return;
        try {
            await discussionApi.deletePost(postId);
            setPosts(prev => prev.filter(p => p.postId !== postId));
            setSelectedId(null);
            setDetail(null);
        } catch (e) {
            console.error('게시글 삭제 실패', e);
        }
    };

    const handleCommentEditStart = (comment: CommentResponse) => {
        setEditingCommentId(comment.commentId);
        setEditingCommentText(comment.content);
    };

    const handleCommentEditSubmit = async (postId: number, commentId: number) => {
        const trimmed = editingCommentText.trim();
        if (!trimmed) return;
        try {
            const updated = await discussionApi.editComment(postId, commentId, trimmed);
            setDetail(prev => {
                if (!prev) return prev;
                return { ...prev, comments: prev.comments.map(c => c.commentId !== commentId ? c : updated) };
            });
            setEditingCommentId(null);
            setEditingCommentText('');
        } catch (e) {
            console.error('댓글 수정 실패', e);
        }
    };

    const handleCommentDelete = async (postId: number, commentId: number) => {
        if (!window.confirm('댓글을 삭제하시겠습니까?')) return;
        try {
            await discussionApi.deleteComment(postId, commentId);
            setDetail(prev => {
                if (!prev) return prev;
                return { ...prev, comments: prev.comments.filter(c => c.commentId !== commentId) };
            });
            setPosts(prev => prev.map(p =>
                p.postId !== postId ? p : { ...p, commentCount: Math.max(0, p.commentCount - 1) }
            ));
        } catch (e) {
            console.error('댓글 삭제 실패', e);
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

            {isWriting ? (
                <WriteView
                    stockName={selectedStock.name}
                    stockCode={selectedStock.code}
                    onSubmit={handlePostSubmit}
                    onCancel={() => setIsWriting(false)}
                />
            ) : isEditingPost && detail ? (
                <EditPostView
                    stockName={detail.stockName}
                    stockCode={detail.stockCode}
                    initialTitle={detail.title}
                    initialContent={detail.content}
                    onSubmit={handlePostEdit}
                    onCancel={() => setIsEditingPost(false)}
                />
            ) : selectedId !== null ? (
                <div className="discussion-detail-view">
                    <button className="detail-back" onClick={() => { setSelectedId(null); setEditingCommentId(null); }}>
                        <FiChevronLeft /> 목록으로
                    </button>

                    <div className="detail-scroll-area">
                        {detailLoading || !detail ? (
                            <div className="discussion-empty"><span>불러오는 중...</span></div>
                        ) : (
                            <>
                                <div className="detail-post">
                                    <div className="discussion-meta">
                                        <Avatar src={detail.authorProfileImageUrl} alt={detail.authorNickname} />
                                        <span className="d-author">{detail.authorNickname}</span>
                                        <StockBadge stockName={detail.stockName} stockCode={detail.stockCode} />
                                        <span className="d-time">
                                            <span className="d-time-date">{formatDateTime(detail.createdDateTime).date}</span>
                                            <span className="d-time-clock">{formatDateTime(detail.createdDateTime).time}</span>
                                            {detail.updatedDateTime !== detail.createdDateTime && (
                                                <span className="d-edited-badge">(수정됨)</span>
                                            )}
                                        </span>
                                    </div>
                                    <p className="detail-post-title">{detail.title}</p>
                                    <p className="detail-post-content">{detail.content}</p>
                                    <div className="detail-divider" />
                                    <div className="d-actions">
                                        <Tooltip text="좋아요" placement="top">
                                            <button
                                                className={`d-btn like${detail.myReaction === 'LIKE' ? ' active' : ''}`}
                                                onClick={e => handlePostReact(detail.postId, 'LIKE', e)}
                                            >
                                                <FaThumbsUp /> {detail.likes}
                                            </button>
                                        </Tooltip>
                                        <Tooltip text="싫어요" placement="top">
                                            <button
                                                className={`d-btn dislike${detail.myReaction === 'DISLIKE' ? ' active' : ''}`}
                                                onClick={e => handlePostReact(detail.postId, 'DISLIKE', e)}
                                            >
                                                <FaThumbsDown /> {detail.dislikes}
                                            </button>
                                        </Tooltip>
                                        <Tooltip text="스크랩" placement="top">
                                            <button
                                                className={`d-btn scrap${detail.myScrapped ? ' active' : ''}`}
                                                onClick={e => handleScrap(detail.postId, e)}
                                            >
                                                {detail.myScrapped ? <FaBookmark /> : <FaRegBookmark />}
                                                {detail.scraps}
                                            </button>
                                        </Tooltip>

                                        {detail.authorId === user?.username && (
                                            <div className="post-owner-actions">
                                                <Tooltip text="수정" placement="top">
                                                    <button className="owner-btn edit" onClick={() => setIsEditingPost(true)}>
                                                        <FiEdit />
                                                    </button>
                                                </Tooltip>
                                                <Tooltip text="삭제" placement="top">
                                                    <button className="owner-btn delete" onClick={() => handlePostDelete(detail.postId)}>
                                                        <FiTrash2 />
                                                    </button>
                                                </Tooltip>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                <span className="detail-comments-label">댓글 {detail.comments.length}</span>

                                <ul className="detail-comments">
                                    {detail.comments.length === 0 && (
                                        <li className="discussion-empty">
                                            <FiMessageSquare />
                                            <span>첫 댓글을 남겨보세요</span>
                                        </li>
                                    )}
                                    {detail.comments.map(c => {
                                        const cdt = formatDateTime(c.createdDateTime);
                                        const isMyComment = c.authorId === user?.username;
                                        const isEditingThis = editingCommentId === c.commentId;

                                        return (
                                            <li key={c.commentId} className="detail-comment-item">
                                                <div className="discussion-meta">
                                                    <Avatar src={c.authorProfileImageUrl} alt={c.authorNickname} />
                                                    <span className="d-author">{c.authorNickname}</span>
                                                    <span className="d-time">
                                                        <span className="d-time-date">{cdt.date}</span>
                                                        <span className="d-time-clock">{cdt.time}</span>
                                                        {c.updatedDateTime !== c.createdDateTime && (
                                                            <span className="d-edited-badge">(수정됨)</span>
                                                        )}
                                                    </span>
                                                </div>

                                                {isEditingThis ? (
                                                    <div className="comment-edit-form">
                                                        <div className="input-wrapper">
                                                            <input
                                                                className="discussion-input"
                                                                value={editingCommentText}
                                                                onChange={e => setEditingCommentText(e.target.value)}
                                                                onKeyDown={e => {
                                                                    if (e.key === 'Enter') handleCommentEditSubmit(detail.postId, c.commentId);
                                                                    if (e.key === 'Escape') { setEditingCommentId(null); setEditingCommentText(''); }
                                                                }}
                                                                maxLength={100}
                                                                autoFocus
                                                            />
                                                            <span className="input-char-count">{editingCommentText.length}/100</span>
                                                        </div>
                                                        <div className="comment-edit-actions">
                                                            <button
                                                                className="write-cancel-btn"
                                                                onClick={() => { setEditingCommentId(null); setEditingCommentText(''); }}
                                                            >
                                                                취소
                                                            </button>
                                                            <button
                                                                className="write-submit-btn"
                                                                onClick={() => handleCommentEditSubmit(detail.postId, c.commentId)}
                                                                disabled={!editingCommentText.trim() || editingCommentText.trim() === c.content}
                                                            >
                                                                수정
                                                            </button>
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <p className="d-content">{c.content}</p>
                                                )}

                                                <div className="d-actions">
                                                    <Tooltip text="좋아요" placement="top">
                                                        <button
                                                            className={`d-btn like${c.myReaction === 'LIKE' ? ' active' : ''}`}
                                                            onClick={() => handleCommentReact(detail.postId, c.commentId, 'LIKE')}
                                                        >
                                                            <FaThumbsUp /> {c.likes}
                                                        </button>
                                                    </Tooltip>
                                                    <Tooltip text="싫어요" placement="top">
                                                        <button
                                                            className={`d-btn dislike${c.myReaction === 'DISLIKE' ? ' active' : ''}`}
                                                            onClick={() => handleCommentReact(detail.postId, c.commentId, 'DISLIKE')}
                                                        >
                                                            <FaThumbsDown /> {c.dislikes}
                                                        </button>
                                                    </Tooltip>

                                                    {isMyComment && !isEditingThis && (
                                                        <div className="post-owner-actions">
                                                            <Tooltip text="수정" placement="top">
                                                                <button className="owner-btn edit" onClick={() => handleCommentEditStart(c)}>
                                                                    <FiEdit />
                                                                </button>
                                                            </Tooltip>
                                                            <Tooltip text="삭제" placement="top">
                                                                <button
                                                                    className="owner-btn delete"
                                                                    onClick={() => handleCommentDelete(detail.postId, c.commentId)}
                                                                >
                                                                    <FiTrash2 />
                                                                </button>
                                                            </Tooltip>
                                                        </div>
                                                    )}
                                                </div>
                                            </li>
                                        );
                                    })}
                                </ul>
                            </>
                        )}
                    </div>

                    <CommentInputRow value={commentInput} onChange={setCommentInput} onSend={handleCommentSend} />
                </div>
            ) : (
                <div className="discussion-list-view">
                    {tab === 'list' && (
                        <div className="list-header-block">
                            <StockSelectorRow onWrite={() => setIsWriting(true)} />
                            <StockInfoHeader />
                        </div>
                    )}

                    {tab !== 'list' && (
                        <StockSearchBar value={searchQuery} onChange={setSearchQuery} onClear={() => setSearchQuery('')} />
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
            )}
        </div>
    );
}
