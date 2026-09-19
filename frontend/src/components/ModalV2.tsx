import "./ModalV2.css";
import { useEffect, useState, useRef, useCallback } from "react";
import { createPortal } from "react-dom";

let modalStack: symbol[] = [];

type ModalV2Props = {
    open: boolean;
    title: string;
    onClose: () => void;
    children: React.ReactNode;
    extraClass?: string;
};

function useDraggable(open: boolean) {
    const boxRef = useRef<HTMLDivElement>(null);
    const [offset, setOffset] = useState({ x: 0, y: 0 });

    useEffect(() => {
        if (open) setOffset({ x: 0, y: 0 });
    }, [open]);

    const onMouseDown = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
        if ((e.target as HTMLElement).closest("button")) return;

        const startX = e.clientX - offset.x;
        const startY = e.clientY - offset.y;

        const onMouseMove = (ev: MouseEvent) => {
            setOffset({ x: ev.clientX - startX, y: ev.clientY - startY });
        };

        const onMouseUp = () => {
            window.removeEventListener("mousemove", onMouseMove);
            window.removeEventListener("mouseup", onMouseUp);
        };

        window.addEventListener("mousemove", onMouseMove);
        window.addEventListener("mouseup", onMouseUp);
    }, [offset]);

    return { boxRef, offset, onMouseDown };
}

const TITLE_TO_CLASS: Record<string, string> = {
    "로그아웃": "logout-modal",
    "도움말": "help-modal",
    "관리자": "managed-modal",
    "로그아웃 완료": "logout-result-modal",
    "세션 만료": "session-expired-modal",
    "내 정보": "my-info-modal",
    "비밀번호 변경": "password-modify-modal",
    "내 계좌": "my-account-modal",
    "포트폴리오": "portfolio-modal",
    "지수": "index-modal",
    "인기 종목": "hot-stocks-modal",
    "투자자별 매매동향": "investor-modal",
    "업종": "sector-modal",
    "알림": "alert-modal",
    "관심종목": "watchlist-modal-v2",
    "뉴스/공시": "news-modal-v2",
    "종목톡": "stock-talk-modal",
    "종목토론": "discussion-modal",
    "주문내역": "order-history-modal",
    "주문 취소": "cancel-confirm-modal",
    "주문 확인": "order-confirm-modal",
    "고급 주문": "advanced-order-modal",
};

export default function ModalV2({
                                    open,
                                    title,
                                    onClose,
                                    children,
                                    extraClass,
                                }: ModalV2Props) {
    const [render, setRender] = useState(open);
    const { boxRef, offset, onMouseDown } = useDraggable(open);
    const idRef = useRef(Symbol());

    useEffect(() => {
        if (open) {
            setRender(true);
            return;
        }
        const timer = setTimeout(() => setRender(false), 200);
        return () => clearTimeout(timer);
    }, [open]);

    useEffect(() => {
        if (!open) return;

        modalStack.push(idRef.current);

        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key !== "Escape") return;
            const topId = modalStack[modalStack.length - 1];
            if (topId === idRef.current) {
                onClose();
            }
        };

        window.addEventListener("keydown", handleKeyDown);

        return () => {
            modalStack = modalStack.filter((id) => id !== idRef.current);
            window.removeEventListener("keydown", handleKeyDown);
        };
    }, [open, onClose]);

    if (!render) return null;

    const sizeClass = TITLE_TO_CLASS[title] ?? "";

    return createPortal(
        <div className={`modal-v2-overlay ${open ? "show" : render ? "hide" : ""}`}>
            <div
                ref={boxRef}
                className={`modal-v2-box ${open ? "show" : "hide"} ${sizeClass} ${extraClass ?? ""}`}
                style={{
                    "--drag-x": `${offset.x}px`,
                    "--drag-y": `${offset.y}px`,
                } as React.CSSProperties}
                onClick={(e) => e.stopPropagation()}
            >
                <div className="modal-v2-header modal-v2-drag-handle" onMouseDown={onMouseDown}>
                    <span>{title}</span>
                    <button onClick={onClose}>
                        <span className="modal-v2-close-icon">✕</span>
                    </button>
                </div>

                <div className="modal-v2-body">
                    {children}
                </div>
            </div>
        </div>,
        document.body
    );
}
