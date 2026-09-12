import "./LogoutResultModal.css";
import ModalV2 from "../../components/ModalV2";

type Reason = "manual" | "expired" | null

interface Props {
    reason: Reason
    onConfirm: () => void
}

const COPY: Record<Exclude<Reason, null>, { title: string; message: string; button: string }> = {
    manual: {
        title: "로그아웃 완료",
        message: "정상적으로 로그아웃 처리되었습니다.",
        button: "확인",
    },
    expired: {
        title: "세션 만료",
        message: "세션이 만료되었습니다. 다시 로그인해주세요.",
        button: "로그인 페이지로 이동",
    },
}

export default function LogoutResultModal({ reason, onConfirm }: Props) {
    if (!reason) return null

    const copy = COPY[reason]

    return (
        <ModalV2 open={true} title={copy.title} onClose={onConfirm}>
            <div className="lrm">
                <p className="lrm__message">{copy.message}</p>

                <div className="lrm__footer">
                    <button className="lrm__btn" onClick={onConfirm}>
                        {copy.button}
                    </button>
                </div>
            </div>
        </ModalV2>
    );
}
