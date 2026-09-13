import type { RankTier } from '../../types/rank'
import './RankBadge.css'

interface MinimalRank {
    tier: RankTier
    subTier: number | null
}

interface Props {
    rank: MinimalRank | null
    size?: number
}

const TIER_COLOR: Record<RankTier, string> = {
    UNRANKED: '#64748b',
    BRONZE: '#b45309',
    SILVER: '#94a3b8',
    GOLD: '#fbbf24',
    PLATINUM: '#14b8a6',
    DIAMOND: '#3b82f6',
    MASTER: '#a855f7',
}

const TIER_LABEL: Record<RankTier, string> = {
    UNRANKED: 'UNRANKED',
    BRONZE: 'BRONZE',
    SILVER: 'SILVER',
    GOLD: 'GOLD',
    PLATINUM: 'PLATINUM',
    DIAMOND: 'DIAMOND',
    MASTER: 'MASTER',
}

const ROMAN_MAP = ['', 'Ⅰ', 'Ⅱ', 'Ⅲ', 'Ⅳ', 'Ⅴ']

function formatRankText(tier: RankTier, subTier: number | null): string {
    const label = TIER_LABEL[tier]
    return subTier != null ? `${label} ${ROMAN_MAP[subTier] ?? subTier}` : label
}

export default function RankBadge({ rank, size = 24 }: Props) {
    const tier: RankTier = rank?.tier ?? 'UNRANKED'
    const subTier = rank?.subTier ?? null

    const fontSize = Math.max(9, Math.round(size * 0.46))

    return (
        <span
            className="rank-badge"
            style={{
                background: TIER_COLOR[tier],
                fontSize: `${fontSize}px`,
            }}
        >
            {formatRankText(tier, subTier)}
        </span>
    )
}
