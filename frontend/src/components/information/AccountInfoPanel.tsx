import type { AccountResponse } from '../../types/account.ts'
import type { UserDto } from "../../types/user.ts";

interface Props {
    account: AccountResponse | null
    user: UserDto | null
}

// 날짜 포맷 함수 추가
const formatDate = (dateString: string) => {
    const date = new Date(dateString)

    const yyyy = date.getFullYear()
    const mm = String(date.getMonth() + 1).padStart(2, '0')
    const dd = String(date.getDate()).padStart(2, '0')

    return `${yyyy}.${mm}.${dd}`
}

export default function AccountInfoPanel({ account, user }: Props) {

    if (!account) {
        return (
            <div style={styles.container}>
                <div style={styles.title}>계좌 정보</div>
                <div style={styles.empty}>계좌 정보 수신 대기중...</div>
            </div>
        )
    }

    return (
        <div style={styles.container}>
            <div style={styles.title}>계좌 정보</div>

            <div style={styles.usernameWrapper}>
                <div style={styles.username}>
                    {account.username}
                </div>

                {user && (
                    <div style={styles.joinDate}>
                        ({formatDate(user.createdDateTime)} 가입)
                    </div>
                )}
            </div>

            <div style={styles.divider} />

            <div style={styles.row}>
                <span>누적 손익</span>

                <span>
        <span
            style={{
                color:
                    account.accumulatedProfit > 0
                        ? '#FF6347'
                        : account.accumulatedProfit < 0
                            ? '#4F9DFF'
                            : '#DDD',
                marginRight: '4px'
            }}
        >
            {account.accumulatedProfit.toLocaleString()}원
        </span>

        <span
            style={{
                color:
                    account.accumulatedProfitRate > 0
                        ? '#FF6347'
                        : account.accumulatedProfitRate < 0
                            ? '#4F9DFF'
                            : '#DDD',
            }}
        >
            ({account.accumulatedProfitRate.toFixed(2)}%)
        </span>
    </span>
            </div>


            <InfoRow label="총 자산" value={account.totalValue} highlight />
            <InfoRow label="예수금" value={account.totalCash} />
            <InfoRow label="보유 주식 평가금" value={account.stockValue} />
            <InfoRow label="주문 가능 금액" value={account.availableCash} />
            <InfoRow label="예약 금액" value={account.reservedCash} />
        </div>
    )
}

function InfoRow({
                     label,
                     value,
                     highlight = false,
                 }: {
    label: string
    value: number
    highlight?: boolean
}) {
    return (
        <div style={styles.row}>
            <span>{label}</span>
            <span style={highlight ? styles.highlight : styles.value}>
                {value.toLocaleString()} 원
            </span>
        </div>
    )
}

const styles = {
    container: {
        width: '315px',
        backgroundColor: '#1E1E1E',
        padding: '16px',
        borderRadius: '10px',
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
        height: '260px',
    },

    title: {
        fontSize: '16px',
        fontWeight: 'bold',
        color: '#FFF',
    },

    username: {
        fontSize: '13px',
        color: '#AAA',
    },

    row: {
        display: 'flex',
        justifyContent: 'space-between',
        fontSize: '14px',
        color: '#DDD',
    },

    value: {
        color: '#DDD',
    },

    highlight: {
        fontWeight: 'bold',
        color: '#DDD',
    },

    divider: {
        height: '1px',
        backgroundColor: '#333',
        margin: '6px 0',
    },

    empty: {
        fontSize: '14px',
        color: '#777',
        padding: '10px 0',
    },

    usernameWrapper: {
        display: 'flex',
        alignItems: 'baseline',
        gap: '6px',
    },

    joinDate: {
        fontSize: '12px',
        color: '#888',
    },

} as const
