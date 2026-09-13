import { useState } from "react";
import ModalV2 from "../../components/ModalV2";
import { changePassword } from "../../api/userProfile";
import { useMsg } from "../context/MsgContext";
import "./PasswordModifyModal.css";

interface Props {
    open: boolean;
    onClose: () => void;
}

const passwordRegex = /^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*]).+$/;

export default function PasswordModifyModal({ open, onClose }: Props) {
    const { success, error } = useMsg();

    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [newPasswordConfirm, setNewPasswordConfirm] = useState("");

    const passwordValid = newPassword.length >= 8 && passwordRegex.test(newPassword);

    const handleClose = () => {
        setCurrentPassword("");
        setNewPassword("");
        setNewPasswordConfirm("");
        onClose();
    };

    const handleSubmit = async () => {
        if (!currentPassword) {
            error("현재 비밀번호를 입력하세요.");
            return;
        }
        if (!passwordValid) {
            error("새 비밀번호는 8자 이상, 소문자·숫자·특수문자(!@#$%^&*)를 모두 포함해야 합니다.");
            return;
        }
        if (newPassword !== newPasswordConfirm) {
            error("새 비밀번호가 일치하지 않습니다.");
            return;
        }

        try {
            await changePassword({ currentPassword, newPassword });
            success("비밀번호가 변경되었습니다.");
            handleClose();
        } catch (e: any) {
            error(`비밀번호 변경 실패: ${e.response?.data?.message ?? e.message}`);
        }
    };

    return (
        <ModalV2 open={open} title="비밀번호 변경" onClose={handleClose}>
            <div className="pwm">
                <input
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="현재 비밀번호"
                    className="pwm__input"
                />
                <input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="새 비밀번호"
                    className="pwm__input"
                />
                <input
                    type="password"
                    value={newPasswordConfirm}
                    onChange={(e) => setNewPasswordConfirm(e.target.value)}
                    placeholder="새 비밀번호 확인"
                    className="pwm__input"
                />
                <p className="pwm__hint">8자 이상, 소문자·숫자·특수문자(!@#$%^&*) 모두 포함</p>

                <button className="pwm__submit" onClick={handleSubmit}>
                    변경하기
                </button>
            </div>
        </ModalV2>
    );
}
