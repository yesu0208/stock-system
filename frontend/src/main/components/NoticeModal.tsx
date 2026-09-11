import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { getNotices, getNotice } from '../../api/notice'
import type { NoticeSummary, NoticeDetail } from '../../types/notice'

/**
 * NoticeModal
 * 공지는 특정 종목과 무관한 사이트 전역 정보라 StockInfoPanel이 아니라
 * MainLayout(로그인 후 공통 헤더)에서 연결
 */

interface Props {
    show: boolean
    onClose: () => void
}

export default function NoticeModal({ show, onClose }: Props) {
    const [notices, setNotices] = useState<NoticeSummary[]>([])
    const [cursor, setCursor] = useState<number | undefined>(undefined)
    const [hasNext, setHasNext] = useState(false)
    const [loading, setLoading] = useState(false)
    const [detail, setDetail] = useState<NoticeDetail | null>(null)

    const load = (reset: boolean) => {
        setLoading(true)
        getNotices(reset ? undefined : cursor)
            .then(res => {
                setNotices(prev => (reset ? res.items : [...prev, ...res.items]))
                setCursor(res.nextCursor ?? undefined)
                setHasNext(res.hasNext)
            })
            .finally(() => setLoading(false))
    }

    useEffect(() => {
        if (!show) return
        setDetail(null)
        load(true)
    }, [show])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '420px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                {!detail ? (
                    <>
                        <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>공지사항</h3>
                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            {notices.length === 0 && !loading ? (
                                <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>
                                    등록된 공지사항이 없습니다.
                                </div>
                            ) : (
                                notices.map(n => (
                                    <button
                                        key={n.noticeId}
                                        onClick={() => getNotice(n.noticeId).then(setDetail)}
                                        style={styles.row}
                                    >
                                        <div style={{ fontSize: '13px', fontWeight: 600 }}>{n.title}</div>
                                        <div style={{ fontSize: '12px', color: '#888', marginTop: '2px' }}>{n.contentPreview}</div>
                                        <div style={{ fontSize: '11px', color: '#666', marginTop: '4px' }}>
                                            {new Date(n.createdDateTime).toLocaleDateString('ko-KR')}
                                        </div>
                                    </button>
                                ))
                            )}
                        </div>
                        {hasNext && (
                            <button onClick={() => load(false)} style={styles.moreButton} disabled={loading}>더 보기</button>
                        )}
                        <button onClick={onClose} style={styles.closeButton}>닫기</button>
                    </>
                ) : (
                    <>
                        <button onClick={() => setDetail(null)} style={styles.backButton}>← 목록으로</button>
                        <h3 style={{ margin: '8px 0' }}>{detail.title}</h3>
                        <div style={{ fontSize: '11px', color: '#888', marginBottom: '12px' }}>
                            {new Date(detail.createdDateTime).toLocaleString('ko-KR')}
                        </div>
                        <div style={{ flex: 1, overflowY: 'auto', fontSize: '13px', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>
                            {detail.content}
                        </div>
                        <button onClick={onClose} style={styles.closeButton}>닫기</button>
                    </>
                )}
            </div>
        </Modal>
    )
}

const styles = {
    row: { display: 'block', width: '100%', textAlign: 'left' as const, background: 'none', border: 'none', borderBottom: '1px solid #262626', padding: '10px 4px', cursor: 'pointer', color: '#FFF' },
    moreButton: { padding: '8px', fontSize: '13px', backgroundColor: '#222', color: '#FFF', border: '1px solid #333', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    backButton: { alignSelf: 'flex-start', background: 'none', border: 'none', color: '#4F9DFF', fontSize: '12px', cursor: 'pointer', padding: 0, marginBottom: '4px' },
} as const
