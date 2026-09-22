import "./UserPanel.css";
import RankBadge from "./RankBadge";
import { FiUser, FiCalendar, FiAward, FiEye, FiEyeOff, FiCreditCard, FiPieChart } from "react-icons/fi";
import MyAccountModal from "./MyAccountModal";
import { useState } from "react";
import { useUser } from "../context/UserContext";
import { useAccount } from "../context/AccountContext";
import type { MarginStatus, AccountStatus } from "../../types/account";
import Tooltip from "../../tooltip/Tooltip";
import MyInfoModal from "./MyInfoModal";
import { resolveProfileImageUrl, DEFAULT_AVATAR } from "../../utils/image";
import PortfolioModal from "./PortfolioModal";

function formatJoinDate(iso: string): string {
    try {
        const d = new Date(iso);
        const yyyy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");
        return `${yyyy}.${mm}.${dd}`;
    } catch {
        return iso;
    }
}

const MARGIN_STATUS_LABEL: Record<MarginStatus, string> = {
    NORMAL: "정상",
    MARGIN_CALL: "마진콜",
    LIQUIDATION_PENDING: "청산 대기",
};

const MARGIN_STATUS_CLASS: Record<MarginStatus, string> = {
    NORMAL: "margin-status-text--normal",
    MARGIN_CALL: "margin-status-text--call",
    LIQUIDATION_PENDING: "margin-status-text--risk",
};

const ACCOUNT_STATUS_LABEL: Record<AccountStatus, string> = {
    NORMAL: "정상",
    NEGATIVE: "마이너스",
    SUSPENDED: "거래 정지",
};

const ACCOUNT_STATUS_CLASS: Record<AccountStatus, string> = {
    NORMAL: "margin-status-text--normal",
    NEGATIVE: "margin-status-text--call",
    SUSPENDED: "margin-status-text--risk",
};

export default function UserPanel() {
    const { user } = useUser();
    const { account } = useAccount();

    const getValueClass = (value: number) => {
        if (value > 0) return "positive";
        if (value < 0) return "negative";
        return "neutral";
    };

    const fmt = (n: number) => n.toLocaleString("ko-KR");
    const fmtSigned = (n: number) => `${n > 0 ? "+" : ""}${fmt(n)}`;
    const fmtRate = (n: number) => `${n > 0 ? "+" : ""}${n.toFixed(2)}%`;

    const [hideBalance, setHideBalance] = useState(false);
    const [openModal, setOpenModal] = useState<null | "info" | "portfolio" | "account">(null);

    return (
        <aside className="user-panel">
            <div className="profile">
                <img
                    src={resolveProfileImageUrl(user?.profileImageUrl)}
                    alt="프로필"
                    className="avatar"
                    onError={(e) => { e.currentTarget.src = DEFAULT_AVATAR; }}
                />

                <div className="profile-info">
                    <div className="profile-row">
                        <FiUser className="icon" />
                        <span>
                            {user?.nickname ? (
                                <>
                                    <strong className="profile-nickname">{user.nickname}</strong>님
                                </>
                            ) : (
                                "..."
                            )}
                        </span>
                    </div>

                    <div className="profile-row">
                        <FiCalendar className="icon" />
                        <span>{user ? `${formatJoinDate(user.createdDateTime)} 가입` : "..."}</span>
                    </div>

                    <div className="rank-box">
                        <FiAward className="icon" />
                        <span className="rp-score">{user?.rank ? `${user.rank.rp} RP` : "..."}</span>
                        {user && <RankBadge rank={user.rank} />}
                    </div>
                </div>
            </div>

            <div className="account-panel">
                <div className="section-header">
                    <h3>계좌 정보</h3>

                    <Tooltip text={hideBalance ? "계좌 표시" : "계좌 숨기기"} placement="top">
                        <button
                            className="visibility-btn"
                            onClick={() => setHideBalance((prev) => !prev)}
                        >
                            {hideBalance ? <FiEyeOff /> : <FiEye />}
                        </button>
                    </Tooltip>
                </div>

                {account?.accountStatus === "SUSPENDED" && (
                    <div className="account-suspended-banner">
                        ⚠ 계좌가 거래 정지되었습니다. 고객센터에 문의해주세요.
                    </div>
                )}

                {account ? (
                    <>
                        <div className="row">
                            <span>계좌 상태</span>
                            {hideBalance ? (
                                <span>****</span>
                            ) : (
                                <span className={`margin-status-text ${ACCOUNT_STATUS_CLASS[account.accountStatus]}`}>
                                    {ACCOUNT_STATUS_LABEL[account.accountStatus]}
                                </span>
                            )}
                        </div>

                        <div className="row">
                            <span>마진 상태</span>
                            {hideBalance ? (
                                <span>****</span>
                            ) : (
                                <span className={`margin-status-text ${MARGIN_STATUS_CLASS[account.marginStatus]}`}>
                                    {MARGIN_STATUS_LABEL[account.marginStatus]}
                                </span>
                            )}
                        </div>

                        <div className="row">
                            <span>총 자산</span>
                            <span>{hideBalance ? "******** 원" : `${fmt(account.totalValue)} 원`}</span>
                        </div>

                        <div className="row">
                            <span>평가손익</span>
                            <span className={hideBalance ? "" : getValueClass(account.totalProfit)}>
                                {hideBalance ? "******** 원" : `${fmtSigned(account.totalProfit)} 원`}
                            </span>
                        </div>

                        <div className="row">
                            <span>수익률</span>
                            <span className={hideBalance ? "" : getValueClass(account.totalProfitRate)}>
                                {hideBalance ? "**.** %" : fmtRate(account.totalProfitRate)}
                            </span>
                        </div>

                        <div className="row">
                            <span>예수금</span>
                            <span>{hideBalance ? "******** 원" : `${fmt(account.totalCash)} 원`}</span>
                        </div>

                        <div className="row">
                            <span>주식 평가금액</span>
                            <span>{hideBalance ? "******** 원" : `${fmt(account.stockValue)} 원`}</span>
                        </div>

                        <div className="row">
                            <span>레버리지 순자산</span>
                            <span>{hideBalance ? "******** 원" : `${fmt(account.leverageNetValue ?? 0)} 원`}</span>
                        </div>

                        <div className="row">
                            <span>매수 가능금액</span>
                            <span>{hideBalance ? "******** 원" : `${fmt(account.availableCash)} 원`}</span>
                        </div>
                    </>
                ) : (
                    <div className="row" style={{ justifyContent: "center", opacity: 0.4 }}>
                        불러오는 중...
                    </div>
                )}
            </div>

            <div className="menu-section">
                <button className="menu-btn" onClick={() => setOpenModal("info")}>
                    <FiUser className="menu-icon" />
                    내 정보
                </button>
                <button className="menu-btn" onClick={() => setOpenModal("account")}>
                    <FiCreditCard className="menu-icon" />
                    내 계좌
                </button>
                <button className="menu-btn" onClick={() => setOpenModal("portfolio")}>
                    <FiPieChart className="menu-icon" />
                    포트폴리오
                </button>
            </div>

            <MyInfoModal open={openModal === "info"} onClose={() => setOpenModal(null)} />
            <MyAccountModal open={openModal === "account"} onClose={() => setOpenModal(null)} />
            <PortfolioModal open={openModal === "portfolio"} onClose={() => setOpenModal(null)} />
        </aside>
    );
}
