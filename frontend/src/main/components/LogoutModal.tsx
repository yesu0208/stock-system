import "./LogoutModal.css";
import ModalV2 from "../../components/ModalV2";

interface Props {
    open: boolean;
    onClose: () => void;
    onConfirm: () => void;
}

export default function LogoutModal({ open, onClose, onConfirm }: Props) {
    return (
        <ModalV2 open={open} title="로그아웃" onClose={onClose}>
            <div className="lom">
                <p className="lom__message">로그아웃 하시겠습니까?</p>

                <div className="lom__footer">
                    <button className="lom__btn lom__btn--confirm" onClick={onConfirm}>
                        로그아웃
                    </button>
                    <button className="lom__btn lom__btn--close" onClick={onClose}>
                        취소
                    </button>
                </div>
            </div>
        </ModalV2>
    );
}
