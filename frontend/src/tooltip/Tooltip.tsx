import { useState, useRef } from "react";
import { createPortal } from "react-dom";
import "./Tooltip.css";

interface TooltipProps {
    text: string;
    children: React.ReactNode;
    placement?: "top" | "bottom" | "left" | "right";
}

export default function Tooltip({ text, children, placement = "top" }: TooltipProps) {
    const [visible, setVisible] = useState(false);
    const [coords, setCoords] = useState({ top: 0, left: 0 });
    const wrapperRef = useRef<HTMLSpanElement>(null);

    const updateCoords = () => {
        if (!wrapperRef.current) return;
        const rect = wrapperRef.current.getBoundingClientRect();

        const positions = {
            top:    { top: rect.top + window.scrollY - 6,               left: rect.left + window.scrollX + rect.width / 2 },
            bottom: { top: rect.bottom + window.scrollY + 6,             left: rect.left + window.scrollX + rect.width / 2 },
            left:   { top: rect.top + window.scrollY + rect.height / 2,  left: rect.left + window.scrollX - 6 },
            right:  { top: rect.top + window.scrollY + rect.height / 2,  left: rect.right + window.scrollX + 6 },
        };
        setCoords(positions[placement]);
    };

    return (
        <span
            ref={wrapperRef}
            className="tt-wrapper"
            onMouseEnter={() => { updateCoords(); setVisible(true); }}
            onMouseLeave={() => setVisible(false)}
        >
            {children}
            {visible && createPortal(
                <span
                    className={`tt-box tt-${placement}`}
                    style={{ top: coords.top, left: coords.left }}
                >
                    {text}
                    <span className="tt-arrow" />
                </span>,
                document.body
            )}
        </span>
    );
}
