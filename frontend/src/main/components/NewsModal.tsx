import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import { searchNews } from '../../api/news'
import type { NaverNewsItem } from '../../types/news'

/**
 * NewsModal
 * 백엔드 NewsController.java(GET /api/v1/news?keyword=...) 확인 결과
 * 네이버 뉴스 검색 API를 그대로 감싼 것으로, title/description에
 * 검색어 강조용 <b> 태그가 포함되어 내려옴 — 렌더링 전에 제거
 */

interface Props {
    show: boolean
    onClose: () => void
    keyword: string   // 보통 종목명
}

function stripTags(html: string): string {
    return html.replace(/<[^>]*>/g, '')
}

export default function NewsModal({ show, onClose, keyword }: Props) {
    const [news, setNews] = useState<NaverNewsItem[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!show) return
        setLoading(true)
        searchNews(keyword)
            .then(setNews)
            .catch(() => setNews([]))
            .finally(() => setLoading(false))
    }, [show, keyword])

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '420px', maxHeight: '65vh', display: 'flex', flexDirection: 'column' }}>
                <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>{keyword} 관련 뉴스</h3>

                <div style={{ flex: 1, overflowY: 'auto' }}>
                    {loading ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                    ) : news.length === 0 ? (
                        <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>관련 뉴스가 없습니다.</div>
                    ) : (
                        news.map((n, i) => (
                            <a key={i} href={n.originallink || n.link} target="_blank" rel="noopener noreferrer" style={styles.row}>
                                <div style={{ fontSize: '13px', fontWeight: 600 }}>{stripTags(n.title)}</div>
                                <div style={{ fontSize: '12px', color: '#888', marginTop: '2px' }}>{stripTags(n.description)}</div>
                                <div style={{ fontSize: '11px', color: '#666', marginTop: '4px' }}>{n.pubDate}</div>
                            </a>
                        ))
                    )}
                </div>

                <button onClick={onClose} style={styles.closeButton}>닫기</button>
            </div>
        </Modal>
    )
}

const styles = {
    row: { display: 'block', textDecoration: 'none', borderBottom: '1px solid #262626', padding: '10px 4px', color: '#FFF' },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
} as const
