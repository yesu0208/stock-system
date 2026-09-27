import { useEffect, useState, type ReactNode } from 'react'

interface Props {
    children: ReactNode
}

// 최소 기준 해상도 — 접속 시 창이 너무 작으면 이 값 밑으로는 내려가지 않음
const MIN_BASE_WIDTH = 1520
const MIN_BASE_HEIGHT = 720

// 뷰포트가 소수점 크기일 때 innerWidth/innerHeight 반올림 오차 허용 범위(px)
const FIT_TOLERANCE = 1

/**
 * ScaleWrapper
 * 앱이 처음 마운트된 시점의 브라우저 창 크기를 "기준 해상도"로 캡처
 *
 * 확대/축소 대칭 스케일 → 확대 전용 스케일로 변경
 *   - 창을 기준보다 "키우면" → scale > 1, 페이지 전체가 이미지처럼 확대됨
 *   - 창을 기준보다 "줄이면" → scale은 항상 1로 고정, 원본 크기 그대로 유지되고
 *     화면 밖으로 나가는 부분은 스크롤로 확인 가능 (overflow: auto)
 *   - 창이 기준 크기 이상이면(1px 오차 허용) overflow: hidden 유지
 *     → 브라우저 확대/배율(125% 등)로 뷰포트가 소수점 크기일 때 1px 미만 차이로
 *       스크롤바가 생기는 문제 방지
 */
export default function ScaleWrapper({ children }: Props) {
    const [base] = useState(() => ({
        width: Math.max(window.innerWidth, MIN_BASE_WIDTH),
        height: Math.max(window.innerHeight, MIN_BASE_HEIGHT),
    }))

    const [scale, setScale] = useState(1)
    const [fits, setFits] = useState(true)

    useEffect(() => {
        const updateScale = () => {
            const scaleX = window.innerWidth / base.width
            const scaleY = window.innerHeight / base.height

            const nextScale = Math.max(1, Math.min(scaleX, scaleY))

            setScale(nextScale)
            setFits(
                window.innerWidth + FIT_TOLERANCE >= base.width &&
                window.innerHeight + FIT_TOLERANCE >= base.height
            )
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
                overflow: fits ? 'hidden' : 'auto',
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
