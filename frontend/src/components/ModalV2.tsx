import "./ModalV2.css";
import { useEffect, useState, useRef, useCallback } from "react";

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

// title 문자열 → ModalV2.css의 크기 클래스 매핑.
// 다른 모달을 ModalV2로 옮길 때마다 이 표에 한 줄씩 추가.
const TITLE_TO_CLASS: Record<string, string> = {
    "로그아웃": "logout-modal",
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
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === "Escape") onClose();
        };
        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [open, onClose]);

    if (!render) return null;

    const sizeClass = TITLE_TO_CLASS[title] ?? "";

    return (
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
        </div>
    );
}
