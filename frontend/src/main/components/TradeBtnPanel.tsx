import { useState } from 'react';
import { FaStar, FaListAlt, FaNewspaper, FaClipboardList, FaComments } from 'react-icons/fa';
import './TradeBtnPanel.css';
import ModalV2 from '../../components/ModalV2';
import WatchListModal from './WatchListModal';
import NewsModal from './NewsModal';

type ModalType = '관심종목' | '종목토론' | '종목톡' | '주문내역' | '뉴스/공시' | null;

export default function TradeBtnPanel() {
    const [openModal, setOpenModal] = useState<ModalType>(null);

    const close = () => setOpenModal(null);

    return (
        <>
            <div className="trade-btn-panel">
                <button className="trade-btn" onClick={() => setOpenModal('뉴스/공시')}>
                    <FaNewspaper className="icon" />
                    뉴스
                </button>
                <button className="trade-btn" onClick={() => setOpenModal('종목토론')}>
                    <FaClipboardList className="icon" />
                    토론
                </button>
                <button className="trade-btn" onClick={() => setOpenModal('종목톡')}>
                    <FaComments className="icon" />
                    채팅
                </button>
                <button className="trade-btn" onClick={() => setOpenModal('관심종목')}>
                    <FaStar className="icon" />
                    관심
                </button>
                <button className="trade-btn" onClick={() => setOpenModal('주문내역')}>
                    <FaListAlt className="icon" />
                    내역
                </button>
            </div>

            <ModalV2 open={openModal === '관심종목'} title="관심종목" onClose={close}>
                <WatchListModal />
            </ModalV2>

            <ModalV2 open={openModal === '뉴스/공시'} title="뉴스/공시" onClose={close}>
                <NewsModal />
            </ModalV2>

            {/* TODO: 모달 연결 예정
                - 종목토론 → DiscussionModal
                - 종목톡 → StockTalkModal
                - 주문내역 → OrderHistoryModal (이미 있는 컴포넌트 재활용 가능성 있음)
                나머지는 다음 단계에서 하나씩 연결 */}
        </>
    );
}
