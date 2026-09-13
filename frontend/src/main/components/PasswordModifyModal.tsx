import { useState, useRef, useEffect } from "react";
import ModalV2 from "../../components/ModalV2";
import { changePassword } from "../../api/userProfile";
import { useMsg } from "../context/MsgContext";
import { FiEye, FiEyeOff, FiCheck, FiX } from "react-icons/fi";
import "./PasswordModifyModal.css";

interface Props {
    open: boolean;
    onClose: () => void;
}

type SubmitState = "idle" | "loading" | "success" | "failure";

function PwRuleItem({ ok, children }: { ok: boolean; children: React.ReactNode }) {
    return (
        <li className="pwm-rule" data-state={ok ? "ok" : "fail"}>
            <span className="pwm-rule__icon" aria-hidden="true">
                {ok ? <FiCheck size={13} /> : <FiX size={13} />}
            </span>
            <span>{children}</span>
        </li>
    );
}

export default function PasswordModifyModal({ open, onClose }: Props) {
    const { success } = useMsg();

    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [newPasswordConfirm, setNewPasswordConfirm] = useState("");

    const [showCurrentPw, setShowCurrentPw] = useState(false);
    const [showNewPw, setShowNewPw] = useState(false);
    const [showNewPwConfirm, setShowNewPwConfirm] = useState(false);

    const [currentPwInvalid, setCurrentPwInvalid] = useState(false);
    const [newPwInvalid, setNewPwInvalid] = useState(false);
    const [newPwConfirmInvalid, setNewPwConfirmInvalid] = useState(false);

    const [errorMsg, setErrorMsg] = useState("");
    const [state, setState] = useState<SubmitState>("idle");

    const errorInnerRef = useRef<HTMLParagraphElement>(null);
    const [errorHeight, setErrorHeight] = useState(0);

    useEffect(() => {
        if (errorMsg && errorInnerRef.current) {
            setErrorHeight(errorInnerRef.current.scrollHeight);
        } else {
            setErrorHeight(0);
        }
    }, [errorMsg]);

    const pwLengthOk = newPassword.length >= 8;
    const pwLowerOk = /[a-z]/.test(newPassword);
    const pwNumberOk = /[0-9]/.test(newPassword);
    const pwSpecialOk = /[!@#$%^&*]/.test(newPassword);
    const pwConfirmMatchOk = newPasswordConfirm.length > 0 && newPassword === newPasswordConfirm;

    const resetAndClose = () => {
        setCurrentPassword("");
        setNewPassword("");
        setNewPasswordConfirm("");
        setShowCurrentPw(false);
        setShowNewPw(false);
        setShowNewPwConfirm(false);
        setCurrentPwInvalid(false);
        setNewPwInvalid(false);
        setNewPwConfirmInvalid(false);
        setErrorMsg("");
        setState("idle");
        onClose();
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (state !== "idle") return;

        const isCurrentEmpty = !currentPassword.trim();
        setCurrentPwInvalid(isCurrentEmpty);

        if (isCurrentEmpty) {
            setErrorMsg("현재 비밀번호를 입력해주세요.");
            return;
        }

        if (!pwLengthOk || !pwLowerOk || !pwNumberOk || !pwSpecialOk) {
            setNewPwInvalid(true);
            setErrorMsg("새 비밀번호 조건을 확인해주세요.");
            return;
        }

        if (!pwConfirmMatchOk) {
            setNewPwConfirmInvalid(true);
            setErrorMsg("새 비밀번호가 일치하지 않습니다.");
            return;
        }

        setErrorMsg("");
        setState("loading");

        try {
            const MIN_LOADING_MS = 600;
            await Promise.all([
                changePassword({ currentPassword, newPassword }),
                new Promise((resolve) => setTimeout(resolve, MIN_LOADING_MS)),
            ]);

            setState("success");
            success("비밀번호가 변경되었습니다.");
            window.setTimeout(resetAndClose, 500);
        } catch (e: any) {
            setState("failure");
            setCurrentPwInvalid(true);
            setErrorMsg(e.response?.data?.message ?? "현재 비밀번호가 일치하지 않습니다.");
            window.setTimeout(() => setState("idle"), 1200);
        }
    };

    return (
        <ModalV2 open={open} title="비밀번호 변경" onClose={resetAndClose}>
            <form className="pwm" onSubmit={handleSubmit}>
                <div className="pwm-field">
                    <label htmlFor="pwm-current">현재 비밀번호</label>
                    <div className="pwm-field__input-wrap">
                        <input
                            id="pwm-current"
                            type={showCurrentPw ? "text" : "password"}
                            value={currentPassword}
                            onChange={(e) => {
                                setCurrentPassword(e.target.value);
                                if (currentPwInvalid) setCurrentPwInvalid(false);
                            }}
                            placeholder="현재 비밀번호를 입력하세요"
                            autoComplete="current-password"
                            disabled={state !== "idle"}
                            className={currentPwInvalid ? "is-invalid" : ""}
                        />
                        <button
                            type="button"
                            className="pwm-field__toggle-pw"
                            onClick={() => setShowCurrentPw((prev) => !prev)}
                            disabled={state !== "idle"}
                            tabIndex={-1}
                        >
                            {showCurrentPw ? <FiEyeOff size={15} /> : <FiEye size={15} />}
                        </button>
                    </div>
                </div>

                <div className="pwm-field">
                    <label htmlFor="pwm-new">새 비밀번호</label>
                    <div className="pwm-field__input-wrap">
                        <input
                            id="pwm-new"
                            type={showNewPw ? "text" : "password"}
                            value={newPassword}
                            onChange={(e) => {
                                setNewPassword(e.target.value);
                                if (newPwInvalid) setNewPwInvalid(false);
                            }}
                            placeholder="새 비밀번호를 입력하세요"
                            autoComplete="new-password"
                            disabled={state !== "idle"}
                            className={newPwInvalid ? "is-invalid" : ""}
                        />
                        <button
                            type="button"
                            className="pwm-field__toggle-pw"
                            onClick={() => setShowNewPw((prev) => !prev)}
                            disabled={state !== "idle"}
                            tabIndex={-1}
                        >
                            {showNewPw ? <FiEyeOff size={15} /> : <FiEye size={15} />}
                        </button>
                    </div>
                </div>

                <div className="pwm-field">
                    <label htmlFor="pwm-new-confirm">새 비밀번호 확인</label>
                    <div className="pwm-field__input-wrap">
                        <input
                            id="pwm-new-confirm"
                            type={showNewPwConfirm ? "text" : "password"}
                            value={newPasswordConfirm}
                            onChange={(e) => {
                                setNewPasswordConfirm(e.target.value);
                                if (newPwConfirmInvalid) setNewPwConfirmInvalid(false);
                            }}
                            placeholder="새 비밀번호를 다시 입력하세요"
                            autoComplete="new-password"
                            disabled={state !== "idle"}
                            className={newPwConfirmInvalid ? "is-invalid" : ""}
                        />
                        <button
                            type="button"
                            className="pwm-field__toggle-pw"
                            onClick={() => setShowNewPwConfirm((prev) => !prev)}
                            disabled={state !== "idle"}
                            tabIndex={-1}
                        >
                            {showNewPwConfirm ? <FiEyeOff size={15} /> : <FiEye size={15} />}
                        </button>
                    </div>
                </div>

                <ul className="pwm-rules">
                    <PwRuleItem ok={pwLengthOk}>8자 이상</PwRuleItem>
                    <PwRuleItem ok={pwLowerOk}>영어 소문자 포함</PwRuleItem>
                    <PwRuleItem ok={pwNumberOk}>숫자 포함</PwRuleItem>
                    <PwRuleItem ok={pwSpecialOk}>특수문자 포함 (!@#$%^&*)</PwRuleItem>
                    <PwRuleItem ok={pwConfirmMatchOk}>새 비밀번호와 일치</PwRuleItem>
                </ul>

                <div className="pwm-error-wrap" style={{ height: errorHeight }}>
                    <p className="pwm-error" ref={errorInnerRef}>{errorMsg}</p>
                </div>

                <button type="submit" className="pwm-submit" data-state={state} disabled={state !== "idle"}>
                    {state === "loading" ? "변경 중..." : state === "success" ? "변경 완료" : "변경하기"}
                </button>
            </form>
        </ModalV2>
    );
}
