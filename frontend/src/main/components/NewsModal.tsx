import { useEffect, useRef, useState } from 'react';
import { useStock } from '../context/StockContext';
import { searchNews } from '../../api/news';
import type { NaverNewsItem } from '../../types/news';
import './NewsModal.css';
import Spinner from "../../components/Spinner";

type TabType = '뉴스' | '공시';

const stripHtml = (str: string) =>
    str.replace(/<[^>]+>/g, '').replace(/&quot;/g, '"').replace(/&amp;/g, '&').replace(/&#039;/g, "'");

const formatDate = (pubDate: string) => {
    try {
        const d = new Date(pubDate);
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const hh = String(d.getHours()).padStart(2, '0');
        const min = String(d.getMinutes()).padStart(2, '0');
        return `${mm}/${dd} ${hh}:${min}`;
    } catch {
        return pubDate;
    }
};

export default function NewsModal() {
    const { selectedStock } = useStock();

    const [activeTab, setActiveTab] = useState<TabType>('뉴스');
    const [news, setNews] = useState<NaverNewsItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [overlayVisible, setOverlayVisible] = useState(true);
    const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const dartUrl = `https://dart.fss.or.kr/dsab001/main.do?autoSearch=true&textCrpNm=${encodeURIComponent(selectedStock.name)}`;

    useEffect(() => {
        if (activeTab !== '뉴스') return;

        setLoading(true);
        setError(null);

        searchNews(selectedStock.name)
            .then(setNews)
            .catch((e) => {
                setError('뉴스를 불러오지 못했습니다.');
                console.error(e);
            })
            .finally(() => setLoading(false));
    }, [activeTab, selectedStock.name]);

    useEffect(() => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
            timerRef.current = null;
        }
        setOverlayVisible(true);
    }, [selectedStock.name]);

    const handleDartLoad = () => {
        if (timerRef.current) {
            clearTimeout(timerRef.current);
        }

        timerRef.current = setTimeout(() => {
            setOverlayVisible(false);
            timerRef.current = null;
        }, 600);
    };

    return (
        <div className="news-modal">
            <div className="nm-stock-label">
                {selectedStock.name}
                <span className="nm-stock-code">{selectedStock.code}</span>
            </div>

            <div className="nm-tab-bar">
                {(['뉴스', '공시'] as TabType[]).map((tab) => (
                    <button
                        key={tab}
                        className={`nm-tab ${activeTab === tab ? 'active' : ''}`}
                        onClick={() => setActiveTab(tab)}
                    >
                        {tab === '뉴스' ? '📰 뉴스' : '📄 공시'}
                    </button>
                ))}
            </div>

            {activeTab === '뉴스' && (
                <div className="nm-news-panel">
                    {loading && <div className="nm-status"><Spinner /></div>}
                    {error && <p className="nm-status error">{error}</p>}
                    {!loading && !error && news.length === 0 && (
                        <p className="nm-status">검색 결과가 없습니다.</p>
                    )}
                    <ul className="nm-news-list">
                        {news.map((item, idx) => (
                            <li key={idx} className="nm-news-item">
                                <a
                                    href={item.originallink || item.link}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="nm-news-link"
                                >
                                    <span className="nm-news-title">
                                        {stripHtml(item.title)}
                                    </span>
                                    <span className="nm-news-desc">
                                        {stripHtml(item.description)}
                                    </span>
                                    <span className="nm-news-date">
                                        {formatDate(item.pubDate)}
                                    </span>
                                </a>
                            </li>
                        ))}
                    </ul>
                </div>
            )}

            <div className="nm-dart-panel" style={{ display: activeTab === '공시' ? 'flex' : 'none' }}>
                <div className={`nm-dart-overlay ${overlayVisible ? '' : 'hidden'}`}>
                    <Spinner />
                </div>

                <iframe
                    src={dartUrl}
                    className="nm-dart-iframe"
                    title="DART 공시"
                    style={{
                        filter: 'invert(1) hue-rotate(180deg) brightness(0.85) contrast(0.9)',
                    }}
                    onLoad={handleDartLoad}
                />
            </div>
        </div>
    );
}
