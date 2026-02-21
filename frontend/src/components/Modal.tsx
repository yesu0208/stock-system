import { useState, useEffect } from 'react';

interface ModalProps {
    show: boolean;
    onClose: () => void;
    children: React.ReactNode;
}

export default function Modal({ show, onClose, children }: ModalProps) {
    const [fade, setFade] = useState(false);

    useEffect(() => {
        if (show) {
            const timer = setTimeout(() => setFade(true), 10); // fade-in
            return () => clearTimeout(timer);
        } else {
            // fade-out도 비동기 처리
            const timer = setTimeout(() => setFade(false), 0);
            return () => clearTimeout(timer);
        }
    }, [show]);

    if (!show && !fade) return null;

    return (
        <div
            style={{
        position: 'fixed',
            top: 0,
            left: 0,
            width: '100%',
            height: '100%',
            backgroundColor: 'rgba(0,0,0,0.6)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000,
            opacity: fade ? 1 : 0,
            transition: 'opacity 0.3s ease',
    }}
    onClick={() => {
        setFade(false);
        setTimeout(onClose, 300);
    }}
>
    <div
        style={{
        backgroundColor: '#1E1E1E',
            padding: '30px',
            borderRadius: '12px',
            minWidth: '300px',
            transition: 'transform 0.3s ease',
    }}
    onClick={(e) => e.stopPropagation()}
>
    {children}
    </div>
    </div>
);
}
