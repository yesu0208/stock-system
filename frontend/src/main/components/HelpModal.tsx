import { useState } from "react";
import Modal from "../../components/ModalV2";
import "./HelpModal.css";

import {
    FiUser,
    FiAward,
    FiCreditCard,
    FiTrendingUp,
    FiList,
    FiBarChart2,
    FiShoppingCart,
    FiPercent,
    FiXCircle,
    FiBell,
    FiStar,
    FiPieChart,
    FiUsers,
} from "react-icons/fi";

type Props = {
    open: boolean;
    onClose: () => void;
};

type TabId =
    | "member"
    | "grade"
    | "account"
    | "stock"
    | "orderbook"
    | "chart"
    | "order"
    | "creditOrder"
    | "cancelOrder"
    | "alert"
    | "watchlist"
    | "portfolio"
    | "community";

const TABS: { id: TabId; icon: React.ReactNode; label: string }[] = [
    { id: "member",      icon: <FiUser />,         label: "계정"     },
    { id: "grade",       icon: <FiAward />,        label: "등급"     },
    { id: "account",     icon: <FiCreditCard />,   label: "계좌"     },
    { id: "stock",       icon: <FiTrendingUp />,   label: "지원 종목" },
    { id: "orderbook",   icon: <FiList />,         label: "호가창"   },
    { id: "chart",       icon: <FiBarChart2 />,    label: "차트"     },
    { id: "order",       icon: <FiShoppingCart />, label: "주문"     },
    { id: "creditOrder", icon: <FiPercent />,      label: "신용 주문" },
    { id: "cancelOrder", icon: <FiXCircle />,      label: "주문 취소" },
    { id: "alert",       icon: <FiBell />,         label: "알림"     },
    { id: "watchlist",   icon: <FiStar />,         label: "관심 종목" },
    { id: "portfolio",   icon: <FiPieChart />,     label: "포트폴리오" },
    { id: "community",   icon: <FiUsers />,        label: "커뮤니티" },
];

const CONTENT: Record<TabId, { title: string; sections: { heading: string; items: string[] }[] }> = {
    member: {
        title: "계정",
        sections: [
            {
                heading: "회원 정보",
                items: [
                    "왼쪽 '내 정보' 탭에서 닉네임과 프로필 이미지를 수정할 수 있습니다.",
                ],
            },
            {
                heading: "로그인 및 보안",
                items: [
                    "계정 보안을 위해 비밀번호를 주기적으로 변경해 주세요.",
                    "우측 상단 메뉴의 로그아웃 버튼으로 로그아웃할 수 있습니다.",
                ],
            },
        ],
    },
    grade: {
        title: "등급",
        sections: [
            {
                heading: "등급 안내",
                items: [
                    "투자 수익률과 거래 횟수를 기반으로 등급이 산정됩니다.",
                    "등급은 UNRANKED → BRONZE → SILVER → GOLD → PLATINUM → DIAMOND → MASTER 순으로 오릅니다. (UNRANKED, MASTER 제외 각 등급 내에서 1 ~ 5로 세부 등급 지정)",
                ],
            },
            {
                heading: "등급 갱신",
                items: [
                    "등급은 장이 열린 날 마감시간 기준으로 자동 갱신됩니다.",
                    "보유 포지션 손익도 등급 산정에 반영됩니다.",
                ],
            },
            {
                heading: "등급별 신용 가능 범위",
                items: [
                    "등급에 따라 가능한 신용 주문 범위는 다음과 같습니다.",
                    "보유 포지션 손익도 등급 산정에 반영됩니다.",
                ],
            },
        ],
    },
    account: {
        title: "계좌",
        sections: [
            {
                heading: "계좌 개요",
                items: [
                    "모의투자 계좌에서 가상 자산으로 매매를 체험할 수 있습니다.",
                    "초기 예수금은 시스템에서 지급되며 실제 금액과 무관합니다.",
                ],
            },
            {
                heading: "잔고 및 손익",
                items: [
                    "보유 종목의 평가금액과 손익은 실시간으로 반영됩니다.",
                    "총 자산 = 예수금 + 보유 종목 평가금액입니다.",
                    "실현 손익과 미실현 손익을 구분해서 확인할 수 있습니다.",
                ],
            },
        ],
    },
    stock: {
        title: "지원 종목",
        sections: [
            {
                heading: "종목 검색",
                items: [
                    "상단 검색창에서 종목명 또는 종목코드로 검색할 수 있습니다.",
                    "검색 결과를 클릭하면 해당 종목으로 즉시 전환됩니다.",
                ],
            },
            {
                heading: "지원 범위",
                items: [
                    "국내 주요 거래소에 상장된 종목을 지원합니다.",
                    "상단 바에서 현재가·시가·고가·저가·거래량·거래대금을 확인할 수 있습니다.",
                    "별 아이콘을 눌러 관심종목으로 등록하세요.",
                ],
            },
        ],
    },
    orderbook: {
        title: "호가창",
        sections: [
            {
                heading: "호가 구성",
                items: [
                    "매도/매수 호가와 잔량을 실시간으로 표시합니다.",
                    "현재가 기준 위는 매도호가(빨강), 아래는 매수호가(파랑)입니다.",
                    "각 호가를 클릭하면 주문 패널에 가격이 자동 입력됩니다.",
                ],
            },
            {
                heading: "잔량 바",
                items: [
                    "잔량 크기에 따라 배경 바의 길이가 달라집니다.",
                    "잔량이 많을수록 해당 가격대의 지지/저항이 강하다는 의미입니다.",
                ],
            },
        ],
    },
    chart: {
        title: "차트",
        sections: [
            {
                heading: "캔들 차트",
                items: [
                    "캔들 차트로 시가·고가·저가·종가를 한눈에 확인할 수 있습니다.",
                    "양봉(빨강)은 종가 > 시가, 음봉(파랑)은 종가 < 시가를 나타냅니다.",
                ],
            },
            {
                heading: "기간 전환",
                items: [
                    "우측 상단 버튼으로 1분·5분·15분·60분·일봉 등 기간을 전환하세요.",
                    "기간을 짧게 설정할수록 실시간 등락을 자세히 볼 수 있습니다.",
                ],
            },
        ],
    },
    order: {
        title: "주문",
        sections: [
            {
                heading: "주문 유형",
                items: [
                    "지정가: 원하는 가격을 직접 입력해 주문합니다.",
                    "시장가: 현재 시장 가격으로 즉시 체결됩니다.",
                ],
            },
            {
                heading: "주문 방법",
                items: [
                    "가격과 수량 입력 후 매수/매도 버튼을 누르면 주문이 접수됩니다.",
                    "보유 수량 이상의 매도 주문은 제한됩니다.",
                    "미체결 주문은 주문내역에서 취소할 수 있습니다.",
                ],
            },
        ],
    },
    creditOrder: {
        title: "신용 주문",
        sections: [
            {
                heading: "신용거래 개요",
                items: [
                    "신용주문은 증거금을 담보로 레버리지를 활용해 매매하는 방식입니다.",
                    "레버리지 배율은 종목 및 등급에 따라 다르게 적용될 수 있습니다.",
                ],
            },
            {
                heading: "청산 및 위험 관리",
                items: [
                    "평가손실이 유지증거금 이하로 떨어지면 반대매매(강제 청산)가 발생할 수 있습니다.",
                    "주문 패널에서 현재 레버리지 배율과 청산가를 확인할 수 있습니다.",
                ],
            },
        ],
    },
    cancelOrder: {
        title: "주문 취소",
        sections: [
            {
                heading: "취소 방법",
                items: [
                    "미체결 주문은 주문내역 목록에서 취소 버튼으로 즉시 취소할 수 있습니다.",
                    "일부만 체결된 주문은 잔여 수량만 취소됩니다.",
                ],
            },
            {
                heading: "유의 사항",
                items: [
                    "이미 체결이 완료된 주문은 취소할 수 없습니다.",
                    "취소 요청 처리 중에는 중복 취소를 시도하지 마세요.",
                ],
            },
        ],
    },
    alert: {
        title: "알림",
        sections: [
            {
                heading: "가격 알림 등록",
                items: [
                    "상단 종 아이콘을 눌러 특정 가격 도달 시 알림을 등록하세요.",
                    "목표가, 조건(이상/이하)을 설정해 등록합니다.",
                    "알림이 등록된 종목은 종 아이콘이 노란색으로 표시됩니다.",
                ],
            },
            {
                heading: "알림 수신",
                items: [
                    "조건이 충족되면 헤더 우측 메시지 창에 알림이 표시됩니다.",
                    "헤더의 알림 버튼으로 메시지 창을 켜고 끌 수 있습니다.",
                ],
            },
        ],
    },
    watchlist: {
        title: "관심 종목",
        sections: [
            {
                heading: "등록 및 해제",
                items: [
                    "상단 바의 별 아이콘을 눌러 현재 종목을 관심종목으로 등록하세요.",
                    "같은 버튼을 다시 누르면 관심종목에서 해제됩니다.",
                ],
            },
            {
                heading: "관심종목 목록",
                items: [
                    "관심종목 버튼을 클릭하면 등록된 종목 목록을 확인할 수 있습니다.",
                    "목록에서 종목을 클릭하면 해당 종목으로 바로 이동합니다.",
                ],
            },
        ],
    },
    portfolio: {
        title: "포트폴리오",
        sections: [
            {
                heading: "자산 구성",
                items: [
                    "보유 종목별 비중을 도넛 차트로 한눈에 확인할 수 있습니다.",
                    "업종별, 종목별 등 다양한 기준으로 구성을 전환해 볼 수 있습니다.",
                ],
            },
            {
                heading: "수익률 분석",
                items: [
                    "기간별 누적 수익률 추이를 확인할 수 있습니다.",
                    "종목별 기여도를 통해 수익/손실 원인을 파악할 수 있습니다.",
                ],
            },
        ],
    },
    community: {
        title: "커뮤니티",
        sections: [
            {
                heading: "게시글",
                items: [
                    "종목별 게시글을 작성하고 다른 투자자와 의견을 나눌 수 있습니다.",
                    "글쓰기 버튼을 눌러 제목과 내용을 입력 후 등록하세요.",
                    "본인 게시글은 수정·삭제가 가능합니다.",
                ],
            },
            {
                heading: "반응 및 스크랩",
                items: [
                    "좋아요·싫어요로 게시글에 반응을 남길 수 있습니다.",
                    "북마크 아이콘으로 게시글을 스크랩해 나중에 다시 볼 수 있습니다.",
                    "스크랩한 글은 '스크랩' 탭에서 모아볼 수 있습니다.",
                ],
            },
        ],
    },
};

export default function HelpModal({ open, onClose }: Props) {
    const [activeTab, setActiveTab] = useState<TabId>("member");

    const current = CONTENT[activeTab];
    const activeTabMeta = TABS.find((t) => t.id === activeTab)!;

    return (
        <Modal open={open} title="도움말" onClose={onClose}>
            <div className="help-modal">
                <nav className="help-sidebar">
                    {TABS.map((tab) => (
                        <button
                            key={tab.id}
                            className={`help-tab-btn${activeTab === tab.id ? " active" : ""}`}
                            onClick={() => setActiveTab(tab.id)}
                        >
                            <span className="help-tab-btn__icon" aria-hidden="true">
                                {tab.icon}
                            </span>
                            <span className="help-tab-btn__label">{tab.label}</span>
                        </button>
                    ))}
                </nav>

                <div className="help-content">
                    <div className="help-content__inner" key={activeTab}>
                        <div className="help-content__header">
            <span className="help-content__icon" aria-hidden="true">
                {activeTabMeta.icon}
            </span>
                            <span className="help-content__title">{current.title}</span>
                        </div>

                        <div className="help-content__body">
                            {current.sections.map((sec) => (
                                <div key={sec.heading} className="help-section">
                                    <p className="help-section__heading">{sec.heading}</p>
                                    <ul className="help-section__list">
                                        {sec.items.map((item, i) => (
                                            <li key={i} className="help-section__item">
                                                {item}
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </Modal>
    );
}
