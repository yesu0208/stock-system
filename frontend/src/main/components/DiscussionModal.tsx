import { useState, useEffect, useCallback, useRef } from 'react';
import { FaThumbsUp, FaThumbsDown, FaBookmark, FaRegBookmark, FaUserCircle } from 'react-icons/fa';
import { FiMessageSquare, FiChevronLeft, FiSearch, FiX, FiChevronDown, FiEdit, FiTrash2 } from 'react-icons/fi';
import { useStock } from '../context/StockContext';
import { useUser } from '../context/UserContext';
import { STOCKS } from '../data/stocks';
import * as discussionApi from '../../api/discussion';
import type { PostSummary, PostDetail, CommentResponse, ReactionType } from '../../types/discussion';
import './DiscussionModal.css';
import Tooltip from '../../tooltip/Tooltip';

const MAX_TITLE_LENGTH = 50;
const MAX_CONTENT_LENGTH = 500;
const SEARCH_DEBOUNCE_MS = 300;

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

    const [myPostReactions, setMyPostReactions] = useState<Map<number, ReactionType | null>>(new Map());
    const [myCommentReactions, setMyCommentReactions] = useState<Map<number, ReactionType | null>>(new Map());
    const [myScraps, setMyScraps] = useState<Set<number>>(new Set());

    const [searchQuery, setSearchQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');
    const [commentInput, setCommentInput] = useState('');

    const sentinelRef = useRef<HTMLLIElement>(null);

    // TODO: 목록/상세/글쓰기 로직 채워넣을 예정

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

            {/* TODO: 목록 뷰, 상세 뷰, 글쓰기/수정 뷰 */}
        </div>
    );
}
