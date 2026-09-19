import { useState, useEffect, useRef, useCallback } from "react";
import ModalV2 from "../../components/ModalV2";
import RankBadge from "./RankBadge";
import { FiUser, FiCalendar, FiEdit2, FiCheck, FiX, FiLoader, FiLock } from "react-icons/fi";
import { useUser } from "../context/UserContext";
import { useMsg } from "../context/MsgContext";
import { checkNicknameAPI } from "../../api/auth";
import { updateProfileImage, changeNickname, getRankHistory } from "../../api/userProfile";
import type { RankHistoryItem } from "../../types/rank";
import Tooltip from "../../tooltip/Tooltip";
import PasswordModifyModal from "./PasswordModifyModal";
import { resolveProfileImageUrl, DEFAULT_AVATAR } from "../../utils/image";
import "./MyInfoModal.css";
import Spinner from "../../components/Spinner";

interface Props {
    open: boolean;
    onClose: () => void;
}

const PAGE_SIZE = 20;

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

function getRankBarColor(tier: string): string {
    const colorMap: Record<string, [string, string]> = {
        BRONZE: ["#b45309", "#e08a1a"],
        SILVER: ["#94a3b8", "#dbe4ef"],
        GOLD: ["#fbbf24", "#fef0a3"],
        PLATINUM: ["#14b8a6", "#4de0cf"],
        DIAMOND: ["#3b82f6", "#a8d3ff"],
        MASTER: ["#a855f7", "#d0a0ff"],
    };
    const [from, to] = colorMap[tier] ?? ["#334155", "#94a3b8"];
    return `linear-gradient(90deg, ${from}, ${to})`;
}

type AvailabilityState = "idle" | "checking" | "available" | "taken";

function NicknameRuleItem({
                              ok,
                              pending,
                              children,
                          }: {
    ok: boolean;
    pending?: boolean;
    children: React.ReactNode;
}) {
    return (
        <li className="nickname-rule" data-state={pending ? "pending" : ok ? "ok" : "fail"}>
            <span className="nickname-rule__icon" aria-hidden="true">
                {pending ? (
                    <FiLoader size={12} className="nickname-rule__icon-spin" />
                ) : ok ? (
                    <FiCheck size={12} />
                ) : (
                    <FiX size={12} />
                )}
            </span>
            <span className="nickname-rule__text">{children}</span>
        </li>
    );
}

export default function MyInfoModal({ open, onClose }: Props) {
    const { user, refresh } = useUser();
    const { success, error } = useMsg();

    const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);

    const [history, setHistory] = useState<RankHistoryItem[]>([]);
    const [hasNext, setHasNext] = useState(true);
    const [loading, setLoading] = useState(false);

    const [animatedPercent, setAnimatedPercent] = useState(0);

    const [isEditingNickname, setIsEditingNickname] = useState(false);
    const [nicknameInput, setNicknameInput] = useState("");
    const [savingNickname, setSavingNickname] = useState(false);
    const [nicknameError, setNicknameError] = useState<string | null>(null);
    const [nicknameAvailability, setNicknameAvailability] = useState<AvailabilityState>("idle");

    const listRef = useRef<HTMLDivElement>(null);
    const pageRef = useRef(1);
    const hasNextRef = useRef(true);
    const loadingRef = useRef(false);
    const nicknameInputRef = useRef<HTMLInputElement>(null);

    const nicknameLengthOk = nicknameInput.length >= 2 && nicknameInput.length <= 10;
    const nicknameFormatOk = /^[a-z0-9가-힣]+$/.test(nicknameInput);

    useEffect(() => {
        if (!isEditingNickname) return;

        if (!nicknameLengthOk || !nicknameFormatOk) {
            setNicknameAvailability("idle");
            return;
        }

        if (user && nicknameInput === user.nickname) {
            setNicknameAvailability("available");
            return;
        }

        setNicknameAvailability("checking");

        const timer = window.setTimeout(async () => {
            try {
                const res = await checkNicknameAPI(nicknameInput);
                setNicknameAvailability(res.exists ? "taken" : "available");
            } catch {
                setNicknameAvailability("idle");
            }
        }, 400);

        return () => window.clearTimeout(timer);
    }, [nicknameInput, nicknameLengthOk, nicknameFormatOk, isEditingNickname, user]);

    const loadHistory = useCallback((pageToLoad: number) => {
        if (loadingRef.current) return;
        if (pageToLoad > 1 && !hasNextRef.current) return;

        loadingRef.current = true;
        setLoading(true);

        getRankHistory(pageToLoad, PAGE_SIZE)
            .then((data) => {
                setHistory((prev) => (pageToLoad === 1 ? data.items : [...prev, ...data.items]));
                hasNextRef.current = data.hasNext;
                setHasNext(data.hasNext);
            })
            .finally(() => {
                loadingRef.current = false;
                setLoading(false);
            });
    }, []);

    useEffect(() => {
        if (!open) return;

        setHistory([]);
        pageRef.current = 1;
        setHasNext(true);
        setAnimatedPercent(0);
        hasNextRef.current = true;

        setIsEditingNickname(false);
        setNicknameError(null);
        setNicknameAvailability("idle");

        loadHistory(1);
    }, [open, loadHistory]);

    useEffect(() => {
        if (!user?.rank || !open) return;

        const { rp, currentRankMinRp, nextRankMinRp } = user.rank;

        // 최고 등급(nextRankMinRp === null)이면 항상 100%로 표시
        const progressPercent = nextRankMinRp == null
            ? 100
            : Math.min(100, Math.max(0, ((rp - currentRankMinRp) / (nextRankMinRp - currentRankMinRp)) * 100));

        const timer = setTimeout(() => {
            setAnimatedPercent(progressPercent);
        }, 50);

        return () => clearTimeout(timer);
    }, [user, open]);

    useEffect(() => {
        if (isEditingNickname) {
            nicknameInputRef.current?.focus();
            nicknameInputRef.current?.select();
        }
    }, [isEditingNickname]);

    const handleScroll = () => {
        const el = listRef.current;
        if (!el || loadingRef.current || !hasNextRef.current) return;

        if (el.scrollHeight - el.scrollTop - el.clientHeight < 50) {
            pageRef.current += 1;
            loadHistory(pageRef.current);
        }
    };

    const handleProfileImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;
        try {
            await updateProfileImage(file);
            refresh();
            success("프로필 사진이 변경되었습니다.");
        } catch (e: any) {
            error(`프로필 사진 변경 실패: ${e.response?.data?.message ?? e.message}`);
        }
    };

    const handleNicknameEditStart = () => {
        if (!user) return;
        setNicknameInput(user.nickname);
        setNicknameError(null);
        setNicknameAvailability("idle");
        setIsEditingNickname(true);
    };

    const handleNicknameCancel = () => {
        setIsEditingNickname(false);
        setNicknameError(null);
        setNicknameAvailability("idle");
    };

    const handleNicknameSave = async () => {
        if (!user) return;
        const trimmed = nicknameInput.trim();

        if (!trimmed) {
            setNicknameError("닉네임은 비어 있을 수 없습니다.");
            return;
        }

        if (!nicknameLengthOk || !nicknameFormatOk) {
            setNicknameError("닉네임 형식을 확인해주세요. \n(2~10자, 영어 소문자/한글/숫자)");
            return;
        }

        if (trimmed === user.nickname) {
            setIsEditingNickname(false);
            return;
        }

        if (nicknameAvailability !== "available") {
            setNicknameError(
                nicknameAvailability === "taken"
                    ? "이미 사용 중인 닉네임입니다."
                    : "닉네임 중복 확인이 필요합니다."
            );
            return;
        }

        setSavingNickname(true);
        setNicknameError(null);

        try {
            await changeNickname({ nickname: trimmed });
            refresh();
            setIsEditingNickname(false);
        } catch (e: any) {
            setNicknameError(e.response?.data?.message ?? "닉네임 변경 중 오류가 발생했습니다.");
        } finally {
            setSavingNickname(false);
        }
    };

    const handleNicknameKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Enter") {
            e.preventDefault();
            handleNicknameSave();
        } else if (e.key === "Escape") {
            e.preventDefault();
            handleNicknameCancel();
        }
    };

    if (!user) {
        return (
            <ModalV2 open={open} title="내 정보" onClose={onClose}>
                <div className="my-info-content">로딩 중...</div>
            </ModalV2>
        );
    }

    const rank = user.rank;

    return (
        <>
            <ModalV2 open={open} title="내 정보" onClose={onClose}>
                <div className="my-info-content">
                    <div className="profile-section">
                        <div className="profile-avatar-wrapper">
                            <img
                                src={resolveProfileImageUrl(user?.profileImageUrl)}
                                alt="프로필"
                                className="profile-avatar"
                                onError={(e) => { e.currentTarget.src = DEFAULT_AVATAR; }}
                            />
                            <Tooltip text="사진 변경" placement="top">
                                <label className="profile-avatar-edit">
                                    <input
                                        type="file"
                                        accept="image/*"
                                        style={{ display: "none" }}
                                        onChange={handleProfileImageChange}
                                    />
                                    ✎
                                </label>
                            </Tooltip>
                        </div>

                        <div className="profile-lines">
                            <div className="profile-line">
                                <div className="profile-circle">
                                    <FiUser />
                                </div>

                                {isEditingNickname ? (
                                    <div className="nickname-edit-area">
                                        <input
                                            ref={nicknameInputRef}
                                            type="text"
                                            className="nickname-input"
                                            value={nicknameInput}
                                            maxLength={20}
                                            disabled={savingNickname}
                                            onChange={(e) => {
                                                setNicknameInput(e.target.value);
                                                if (nicknameError) setNicknameError(null);
                                            }}
                                            onKeyDown={handleNicknameKeyDown}
                                        />
                                        <Tooltip text="저장" placement="top">
                                            <button
                                                type="button"
                                                className="nickname-btn nickname-btn-save"
                                                onClick={handleNicknameSave}
                                                disabled={savingNickname || nicknameAvailability === "checking"}
                                            >
                                                <FiCheck className="icon" />
                                            </button>
                                        </Tooltip>
                                        <Tooltip text="취소" placement="top">
                                            <button
                                                type="button"
                                                className="nickname-btn nickname-btn-cancel"
                                                onClick={handleNicknameCancel}
                                                disabled={savingNickname}
                                            >
                                                <FiX className="icon" />
                                            </button>
                                        </Tooltip>
                                    </div>
                                ) : (
                                    <>
                                        <span className="profile-text">
                                            <strong className="profile-nickname">{user.nickname}</strong>님
                                        </span>
                                        <Tooltip text="닉네임 변경" placement="top">
                                            <button
                                                type="button"
                                                className="nickname-edit-btn"
                                                onClick={handleNicknameEditStart}
                                            >
                                                <FiEdit2 />
                                            </button>
                                        </Tooltip>
                                    </>
                                )}
                            </div>

                            <div className={`nickname-extra${isEditingNickname ? " expanded" : ""}`}>
                                <div className="nickname-extra-inner">
                                    <ul className="nickname-rules-list">
                                        <NicknameRuleItem ok={nicknameLengthOk}>2~10자</NicknameRuleItem>
                                        <NicknameRuleItem ok={nicknameFormatOk}>영어 소문자, 한글, 숫자만 사용</NicknameRuleItem>
                                        <NicknameRuleItem
                                            ok={nicknameAvailability === "available"}
                                            pending={nicknameAvailability === "checking"}
                                        >
                                            사용 가능한 닉네임
                                        </NicknameRuleItem>
                                    </ul>

                                    {nicknameError && <span className="nickname-error">{nicknameError}</span>}
                                </div>
                            </div>

                            <div className="profile-line-row">
                                <div className="profile-line profile-line--join">
                                    <div className="profile-circle">
                                        <FiCalendar />
                                    </div>
                                    <span className="profile-text">{formatJoinDate(user.createdDateTime)} 가입</span>
                                </div>

                                <div className="profile-line profile-line--password">
                                    <div className="profile-circle">
                                        <FiLock />
                                    </div>
                                    <span className="profile-text">비밀번호</span>
                                    <Tooltip text="비밀번호 변경" placement="top">
                                        <button
                                            type="button"
                                            className="nickname-edit-btn"
                                            onClick={() => setIsPasswordModalOpen(true)}
                                        >
                                            <FiEdit2 />
                                        </button>
                                    </Tooltip>
                                </div>
                            </div>
                        </div>
                    </div>

                    {rank && (
                        <div className="rp-section">
                            <div className="rp-grid">
                                <span className="rp-label">최고</span>
                                <RankBadge rank={{ tier: rank.highestTierReached, subTier: null }}/>
                                <span className="rp-info">
                                    {rank.tier === "UNRANKED"
                                        ? "첫 거래를 시작해보세요!"
                                        : rank.nextRankMinRp == null
                                            ? "최고 등급 달성!"
                                            : `다음 등급까지 ${rank.nextRankMinRp - rank.rp} RP 남음`}
                                </span>

                                <span className="rp-label">현재</span>
                                <RankBadge rank={rank}/>
                                <span className="rp-info">
                                    {rank.tier === "UNRANKED"
                                        ? `${rank.rp} RP (장 마감 후 등급 부여)`
                                        : `${rank.rp} ${rank.nextRankMinRp != null ? `/ ${rank.nextRankMinRp}` : ""} RP`}
                                </span>
                            </div>

                            <div className="rp-bar-bg">
                                <div
                                    className="rp-bar-fill"
                                    style={{
                                        width: `${animatedPercent}%`,
                                        background: getRankBarColor(rank.tier),
                                    }}
                                />
                            </div>
                        </div>
                    )}

                    <div className="history-section">
                        <h4 className="section-title">등급 기록 내역</h4>

                        <div className="history-list" ref={listRef} onScroll={handleScroll}>
                            {!loading && history.length === 0 ? (
                                <div className="history-empty">등급 기록이 없습니다</div>
                            ) : (
                                <>
                                    {history.map((item, idx) => (
                                        <div className="history-row" key={`${item.date}-${idx}`}>
                                            <span className="history-date">{item.date}</span>
                                            <div className="history-badge-cell">
                                                <RankBadge rank={{ tier: item.tier, subTier: item.subTier }}/>
                                            </div>
                                            <span className="history-rp">{item.rp} RP</span>
                                            <span
                                                className={
                                                    "history-change " +
                                                    (item.rpChange > 0 ? "positive" : item.rpChange < 0 ? "negative" : "neutral")
                                                }
                                            >
                                                ({item.rpChange > 0 ? "+" : ""}{item.rpChange})
                                            </span>
                                        </div>
                                    ))}

                                    {loading && (
                                        <div className="history-loading">
                                            <Spinner />
                                        </div>
                                    )}

                                    {!hasNext && history.length > 0 && (
                                        <div className="history-end">마지막 기록입니다</div>
                                    )}
                                </>
                            )}
                        </div>
                    </div>
                </div>
            </ModalV2>

            <PasswordModifyModal
                open={isPasswordModalOpen}
                onClose={() => setIsPasswordModalOpen(false)}
            />
        </>
    );
}
