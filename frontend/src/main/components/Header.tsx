import { useEffect, useState } from "react";
import "./Header.css";

import { FiBell, FiHelpCircle, FiLogOut } from "react-icons/fi";
import { useToast } from "../context/ToastContext";
import { useUser } from "../context/UserContext";
import { useMarketPhase, getPhaseColor } from "../context/MarketPhaseContext";
import HelpModal from "./HelpModal";
import LogoutModal from "./LogoutModal";
import ManagedModal from "./ManagedModal";
import Tooltip from "../../tooltip/Tooltip";

interface Props {
    onLogout: () => void;
}

export default function Header({ onLogout }: Props) {
    const [date, setDate] = useState("");
    const [time, setTime] = useState("");

    const { phase, label } = useMarketPhase();
    const market = {
        label: label ?? "확인 중",
        color: getPhaseColor(phase),
    };

    const { isVisible, toggleVisible, toastCount } = useToast();

    const { user } = useUser();
    const isAdmin = user?.role === "ADMIN";

    const [helpOpen, setHelpOpen] = useState(false);
    const [logoutOpen, setLogoutOpen] = useState(false);
    const [managedOpen, setManagedOpen] = useState(false);

    useEffect(() => {
        const updateTime = () => {
            const now = new Date();

            const day = now.getDay();

            const dateStr =
                `${now.getFullYear()}년 ` +
                `${String(now.getMonth() + 1).padStart(2, "0")}월 ` +
                `${String(now.getDate()).padStart(2, "0")}일 ` +
                `(${["일", "월", "화", "수", "목", "금", "토"][day]})`;

            const timeStr =
                `${String(now.getHours()).padStart(2, "0")}:` +
                `${String(now.getMinutes()).padStart(2, "0")}:` +
                `${String(now.getSeconds()).padStart(2, "0")}`;

            setDate(dateStr);
            setTime(timeStr);
        };

        updateTime();
        const timer = setInterval(updateTime, 1000);

        return () => clearInterval(timer);
    }, []);

    return (
        <>
            <header className="header">
                <div className="header__left">
                    <h1>모의투자 시스템</h1>

                    {isAdmin && (
                        <button
                            type="button"
                            className="admin-badge"
                            onClick={() => setManagedOpen(true)}
                        >
                            관리자
                        </button>
                    )}
                </div>

                <div className="header__center">
                    <span className="market-label">시장 상태</span>

                    <span className={`market-status ${market.color}`}>
                        <span className={`market-dot ${market.color}`} />
                        {market.label}
                    </span>

                    <span className="divider" />

                    <span className="current-date">{date}</span>

                    <span className="current-time">{time}</span>
                </div>

                <div className="header__right">
                    <Tooltip
                        text={isVisible ? "메시지 창 숨기기" : "메시지 창 보이기"}
                        placement="bottom"
                    >
                        <button
                            className={`toast-toggle-btn${isVisible ? " toast-toggle-btn--active" : ""}`}
                            onClick={toggleVisible}
                            aria-label={isVisible ? "메시지 창 숨기기" : "메시지 창 보이기"}
                        >
                            <span className="toast-toggle-btn__icon-wrap">
                                <FiBell
                                    className="icon"
                                    style={isVisible ? { color: "#ffd600", fill: "#ffd600" } : undefined}
                                />
                                {toastCount > 0 && (
                                    <span className="toast-toggle-btn__badge">
                                        {toastCount > 99 ? "99+" : toastCount}
                                    </span>
                                )}
                            </span>
                            <span className={`toast-toggle-btn__label${isVisible ? " toast-toggle-btn__label--on" : ""}`}>
                                {isVisible ? "알림 on" : "알림 off"}
                            </span>
                        </button>
                    </Tooltip>

                    <button onClick={() => setHelpOpen(true)}>
                        <FiHelpCircle className="icon" />
                        도움말
                    </button>

                    <button onClick={() => setLogoutOpen(true)}>
                        <FiLogOut className="icon" />
                        로그아웃
                    </button>
                </div>
            </header>

            <HelpModal open={helpOpen} onClose={() => setHelpOpen(false)} />

            <LogoutModal
                open={logoutOpen}
                onClose={() => setLogoutOpen(false)}
                onConfirm={() => {
                    setLogoutOpen(false);
                    onLogout();
                }}
            />

            {isAdmin && (
                <ManagedModal open={managedOpen} onClose={() => setManagedOpen(false)} />
            )}
        </>
    );
}
