import { useEffect, useState, type ReactNode } from 'react'

interface Props {
    children: ReactNode
}

// 최소 기준 해상도 — 접속 시 창이 너무 작으면 이 값 밑으로는 내려가지 않음
const MIN_BASE_WIDTH = 1280
const MIN_BASE_HEIGHT = 720

/**
 * ScaleWrapper
 * 앱이 처음 마운트된 시점의 브라우저 창 크기를 "기준 해상도"로 캡처
 *
 * 확대/축소 대칭 스케일 → 확대 전용 스케일로 변경
 *   - 창을 기준보다 "키우면" → scale > 1, 페이지 전체가 이미지처럼 확대됨
 *   - 창을 기준보다 "줄이면" → scale은 항상 1로 고정, 원본 크기 그대로 유지되고
 *     화면 밖으로 나가는 부분은 overflow: hidden에 의해 그냥 잘림(클리핑)
 */
export default function ScaleWrapper({ children }: Props) {
    const [base] = useState(() => ({
        width: Math.max(window.innerWidth, MIN_BASE_WIDTH),
        height: Math.max(window.innerHeight, MIN_BASE_HEIGHT),
    }))

    const [scale, setScale] = useState(1)

    useEffect(() => {
        const updateScale = () => {
            const scaleX = window.innerWidth / base.width
            const scaleY = window.innerHeight / base.height

            const nextScale = Math.max(1, Math.min(scaleX, scaleY))

            setScale(nextScale)
        }

        updateScale()
        window.addEventListener('resize', updateScale)
        return () => window.removeEventListener('resize', updateScale)
    }, [base])

    return (
        <div
            style={{
                width: '100vw',
                height: '100vh',
                overflow: 'hidden',
                background: '#000',
            }}
        >
            <div
                style={{
                    width: base.width,
                    height: base.height,
                    transform: `scale(${scale})`,
                    transformOrigin: 'top left',
                }}
            >
                {children}
            </div>
        </div>
    )
}
