import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import {
    getPostsByStock, getPost, createPost, addComment, reactToPost, reactToComment, toggleScrap,
} from '../../api/discussion'
import { useMsg } from '../context/MsgContext'
import type { PostSummary, PostDetail } from '../../types/discussion'

interface Props {
    show: boolean
    onClose: () => void
    stockCode: string
    stockName: string
}

type ViewType = 'LIST' | 'CREATE' | 'DETAIL'

export default function DiscussionModal({ show, onClose, stockCode, stockName }: Props) {
    const { success, error } = useMsg()
    const [view, setView] = useState<ViewType>('LIST')

    const [posts, setPosts] = useState<PostSummary[]>([])
    const [cursor, setCursor] = useState<number | undefined>(undefined)
    const [hasNext, setHasNext] = useState(false)
    const [loading, setLoading] = useState(false)

    const [title, setTitle] = useState('')
    const [content, setContent] = useState('')

    const [detail, setDetail] = useState<PostDetail | null>(null)
    const [commentText, setCommentText] = useState('')

    const loadPosts = (reset: boolean) => {
        setLoading(true)
        getPostsByStock(stockCode, reset ? undefined : cursor)
            .then(res => {
                setPosts(prev => (reset ? res.items : [...prev, ...res.items]))
                setCursor(res.nextCursor ?? undefined)
                setHasNext(res.hasNext)
            })
            .catch(() => setHasNext(false))
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        if (!show) return
        setView('LIST')
        loadPosts(true)
    }, [show, stockCode])

    const openPost = async (postId: number) => {
        try {
            const d = await getPost(postId)
            setDetail(d)
            setView('DETAIL')
        } catch {
            error('게시글을 불러오지 못했습니다.')
        }
    }

    const handleCreate = async () => {
        if (!title.trim() || !content.trim()) {
            error('제목과 내용을 입력하세요.')
            return
        }
        try {
            const created = await createPost({ stockCode, stockName, title, content })
            success('게시글이 등록되었습니다.')
            setTitle('')
            setContent('')
            setDetail(created)
            setView('DETAIL')
        } catch (e: any) {
            error(`게시글 등록 실패: ${e.response?.data?.message ?? e.message}`)
        }
    }

    const handleReactPost = async (type: 'LIKE' | 'DISLIKE') => {
        if (!detail) return
        const res = await reactToPost(detail.postId, type)
        setDetail({ ...detail, likes: res.likes, dislikes: res.dislikes })
    }

    const handleToggleScrap = async () => {
        if (!detail) return
        const res = await toggleScrap(detail.postId)
        setDetail({ ...detail, scraps: res.scraps })
        success(res.scrapped ? '스크랩했습니다.' : '스크랩을 취소했습니다.')
    }

    const handleAddComment = async () => {
        if (!detail || !commentText.trim()) return
        try {
            await addComment(detail.postId, commentText)
            setCommentText('')
            const refreshed = await getPost(detail.postId)
            setDetail(refreshed)
        } catch {
            error('댓글 등록에 실패했습니다.')
        }
    }

    const handleReactComment = async (commentId: number, type: 'LIKE' | 'DISLIKE') => {
        if (!detail) return
        const res = await reactToComment(detail.postId, commentId, type)
        setDetail({
            ...detail,
            comments: detail.comments.map(c =>
                c.commentId === commentId ? { ...c, likes: res.likes, dislikes: res.dislikes } : c
            ),
        })
    }

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '480px', maxHeight: '70vh', display: 'flex', flexDirection: 'column' }}>
                {view === 'LIST' && (
                    <>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                            <h3 style={{ margin: 0 }}>{stockName} 토론방</h3>
                            <button onClick={() => setView('CREATE')} style={styles.primaryButton}>글쓰기</button>
                        </div>

                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            {posts.length === 0 && !loading ? (
                                <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                                    아직 게시글이 없습니다.
                                </div>
                            ) : (
                                posts.map(p => (
                                    <button key={p.postId} onClick={() => openPost(p.postId)} style={styles.postRow}>
                                        <div style={{ fontSize: '13px', fontWeight: 600 }}>{p.title}</div>
                                        <div style={{ fontSize: '12px', color: '#888', marginTop: '2px' }}>{p.contentPreview}</div>
                                        <div style={{ fontSize: '11px', color: '#666', marginTop: '4px' }}>
                                            {p.authorNickname} · 👍{p.likes} 👎{p.dislikes} · 댓글 {p.commentCount} · 스크랩 {p.scraps}
                                        </div>
                                    </button>
                                ))
                            )}
                        </div>

                        {hasNext && (
                            <button onClick={() => loadPosts(false)} style={styles.moreButton} disabled={loading}>더 보기</button>
                        )}
                        <button onClick={onClose} style={styles.closeButton}>닫기</button>
                    </>
                )}

                {view === 'CREATE' && (
                    <>
                        <h3 style={{ marginBottom: '12px' }}>{stockName} 글쓰기</h3>
                        <input
                            value={title}
                            onChange={e => setTitle(e.target.value)}
                            placeholder="제목"
                            style={styles.titleInput}
                        />
                        <textarea
                            value={content}
                            onChange={e => setContent(e.target.value)}
                            placeholder="내용을 입력하세요"
                            style={styles.textarea}
                        />
                        <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                            <button onClick={handleCreate} style={styles.primaryButton}>등록</button>
                            <button onClick={() => setView('LIST')} style={styles.closeButton}>취소</button>
                        </div>
                    </>
                )}

                {view === 'DETAIL' && detail && (
                    <>
                        <button onClick={() => { setView('LIST'); loadPosts(true) }} style={styles.backButton}>← 목록으로</button>
                        <h3 style={{ margin: '8px 0' }}>{detail.title}</h3>
                        <div style={{ fontSize: '11px', color: '#888', marginBottom: '8px' }}>
                            {detail.authorNickname} · {new Date(detail.createdDateTime).toLocaleString('ko-KR')}
                        </div>
                        <div style={{ flex: '0 0 auto', maxHeight: '140px', overflowY: 'auto', fontSize: '13px', lineHeight: 1.6, marginBottom: '10px', whiteSpace: 'pre-wrap' }}>
                            {detail.content}
                        </div>

                        <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                            <button onClick={() => handleReactPost('LIKE')} style={styles.reactButton}>👍 {detail.likes}</button>
                            <button onClick={() => handleReactPost('DISLIKE')} style={styles.reactButton}>👎 {detail.dislikes}</button>
                            <button onClick={handleToggleScrap} style={styles.reactButton}>⭐ {detail.scraps}</button>
                        </div>

                        <div style={{ borderTop: '1px solid #333', paddingTop: '8px', flex: 1, overflowY: 'auto' }}>
                            <h4 style={{ color: '#AAA', margin: '0 0 6px 0', fontSize: '13px' }}>댓글 {detail.comments.length}</h4>
                            {detail.comments.map(c => (
                                <div key={c.commentId} style={styles.commentRow}>
                                    <div style={{ fontSize: '12px', color: '#AAA' }}>{c.authorNickname}</div>
                                    <div style={{ fontSize: '13px', margin: '2px 0' }}>{c.content}</div>
                                    <div style={{ display: 'flex', gap: '6px' }}>
                                        <button onClick={() => handleReactComment(c.commentId, 'LIKE')} style={styles.smallReactButton}>👍 {c.likes}</button>
                                        <button onClick={() => handleReactComment(c.commentId, 'DISLIKE')} style={styles.smallReactButton}>👎 {c.dislikes}</button>
                                    </div>
                                </div>
                            ))}
                        </div>

                        <div style={{ display: 'flex', gap: '6px', marginTop: '8px' }}>
                            <input
                                value={commentText}
                                onChange={e => setCommentText(e.target.value)}
                                placeholder="댓글을 입력하세요"
                                style={{ ...styles.titleInput, flex: 1 }}
                            />
                            <button onClick={handleAddComment} style={styles.primaryButton}>등록</button>
                        </div>
                    </>
                )}
            </div>
        </Modal>
    )
}

const styles = {
    primaryButton: { padding: '6px 12px', fontSize: '13px', borderRadius: '6px', border: 'none', backgroundColor: '#4F9DFF', color: '#FFF', cursor: 'pointer', fontWeight: 600 },
    postRow: { display: 'block', width: '100%', textAlign: 'left' as const, background: 'none', border: 'none', borderBottom: '1px solid #262626', padding: '10px 4px', cursor: 'pointer', color: '#FFF' },
    moreButton: { padding: '8px', fontSize: '13px', backgroundColor: '#222', color: '#FFF', border: '1px solid #333', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    backButton: { alignSelf: 'flex-start', background: 'none', border: 'none', color: '#4F9DFF', fontSize: '12px', cursor: 'pointer', padding: 0 },
    titleInput: { padding: '8px', fontSize: '13px', borderRadius: '6px', border: '1px solid #333', backgroundColor: '#222', color: '#FFF', marginBottom: '8px' },
    textarea: { padding: '8px', fontSize: '13px', borderRadius: '6px', border: '1px solid #333', backgroundColor: '#222', color: '#FFF', minHeight: '120px', resize: 'vertical' as const },
    reactButton: { padding: '4px 10px', fontSize: '12px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    commentRow: { borderBottom: '1px solid #262626', padding: '6px 0' },
    smallReactButton: { padding: '2px 6px', fontSize: '10px', borderRadius: '4px', border: '1px solid #444', backgroundColor: '#1A1A1A', color: '#AAA', cursor: 'pointer' },
} as const
