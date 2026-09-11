import type { RankResponse } from '../../types/rank'

/**
 * RankBadge
 * src/assets/ranks/ 아래에 bronze1.svg, bronze2.svg ...
 * 형태로 아이콘이 있다고 가정하고 (tier + subTier) -> 파일명으로 매핑
 * UNRANKED/MASTER처럼 subTier가 없을 수 있는 경우 "unranked.svg",
 * "master.svg"로 가정 — 실제 파일명/서브티어 범위가 다르면
 * resolveIconPath만 고치면 됨. 매칭되는 아이콘이 없으면 회색 원으로 대체.
 */

const rankIcons = import.meta.glob('/src/assets/ranks/*.svg', { eager: true, import: 'default' }) as Record<string, string>

function resolveIconPath(tier: string, subTier: number | null): string | undefined {
    const filename = subTier != null ? `${tier.toLowerCase()}${subTier}` : tier.toLowerCase()
    const match = Object.entries(rankIcons).find(([path]) => path.endsWith(`/${filename}.svg`))
    return match?.[1]
}

const TIER_LABEL: Record<string, string> = {
    UNRANKED: '언랭크',
    BRONZE: '브론즈',
    SILVER: '실버',
    GOLD: '골드',
    PLATINUM: '플래티넘',
    DIAMOND: '다이아몬드',
    MASTER: '마스터',
}

interface Props {
    rank: RankResponse | null
    size?: number
    showLabel?: boolean
}

export default function RankBadge({ rank, size = 24, showLabel = true }: Props) {
    if (!rank) {
        return <span style={{ fontSize: '12px', color: '#888' }}>랭크 없음</span>
    }

    const iconSrc = resolveIconPath(rank.tier, rank.subTier)
    const label = TIER_LABEL[rank.tier] ?? rank.tier

    return (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            {iconSrc ? (
                <img src={iconSrc} alt={label} width={size} height={size} />
            ) : (
                <span style={{ width: size, height: size, display: 'inline-block', borderRadius: '50%', backgroundColor: '#444' }} />
            )}
            {showLabel && (
                <span style={{ fontSize: '12px', color: '#DDD' }}>
                    {label}{rank.subTier != null ? ` ${rank.subTier}` : ''}
                </span>
            )}
        </span>
    )
}
