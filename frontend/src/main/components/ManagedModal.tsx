import { useEffect, useState } from 'react'
import Modal from '../../components/Modal'
import {
    getAllUsersAdmin, getUserAccountAdmin, getUserPortfolioAdmin,
    getUserOrdersAdmin, getUserAutoOrdersAdmin, getUserOtocosAdmin,
    getUserTrailingStopsAdmin, getUserAlertsAdmin,
} from '../../api/admin'
import RankBadge from './RankBadge'
import { stockNameMap } from '../../constants/stocks'
import type { UserDto } from '../../types/user'
import type { AccountResponse } from '../../types/account'
import type { PortfolioResponse } from '../../types/portfolio'
import type { OrderResponseMessage } from '../../types/order'
import type { AutoOrderResponseMessage } from '../../types/autoOrder'
import type { OtocoResponseMessage } from '../../types/otoco'
import type { TrailingStopResponseMessage } from '../../types/trailingStop'
import type { AlertResponseMessage } from '../../types/alert'

interface Props {
    show: boolean
    onClose: () => void
}

type DetailTab = 'OVERVIEW' | 'ORDERS' | 'AUTO_ORDERS' | 'OTOCO' | 'TRAILING' | 'ALERTS'

const OTOCO_STATUS_LABEL: Record<string, string> = {
    WAITING_ENTRY: '진입 대기',
    ENTRY_ORDER_PLACED: '진입 주문중',
    WAITING_EXIT: '청산 대기',
    COMPLETED: '완료',
    CANCELED: '취소됨',
}

export default function ManagedModal({ show, onClose }: Props) {
    const [users, setUsers] = useState<UserDto[]>([])
    const [selectedUser, setSelectedUser] = useState<UserDto | null>(null)
    const [detailTab, setDetailTab] = useState<DetailTab>('OVERVIEW')

    const [account, setAccount] = useState<AccountResponse | null>(null)
    const [portfolio, setPortfolio] = useState<PortfolioResponse | null>(null)
    const [orders, setOrders] = useState<OrderResponseMessage[]>([])
    const [autoOrders, setAutoOrders] = useState<AutoOrderResponseMessage[]>([])
    const [otocos, setOtocos] = useState<OtocoResponseMessage[]>([])
    const [trailingStops, setTrailingStops] = useState<TrailingStopResponseMessage[]>([])
    const [alerts, setAlerts] = useState<AlertResponseMessage[]>([])

    const [loading, setLoading] = useState(false)

    useEffect(() => {
        if (!show) return
        setSelectedUser(null)
        getAllUsersAdmin().then(setUsers).catch(() => setUsers([]))
    }, [show])

    const openUser = async (user: UserDto) => {
        setSelectedUser(user)
        setDetailTab('OVERVIEW')
        setLoading(true)
        try {
            const [acc, port, ord, auto, otoco, trailing, alertList] = await Promise.all([
                getUserAccountAdmin(user.username).catch(() => null),
                getUserPortfolioAdmin(user.username).catch(() => null),
                getUserOrdersAdmin(user.username).catch(() => []),
                getUserAutoOrdersAdmin(user.username).catch(() => []),
                getUserOtocosAdmin(user.username).catch(() => []),
                getUserTrailingStopsAdmin(user.username).catch(() => []),
                getUserAlertsAdmin(user.username).catch(() => []),
            ])
            setAccount(acc)
            setPortfolio(port)
            setOrders(ord)
            setAutoOrders(auto)
            setOtocos(otoco)
            setTrailingStops(trailing)
            setAlerts(alertList)
        } finally {
            setLoading(false)
        }
    }

    if (!show) return null

    return (
        <Modal show={show} onClose={onClose}>
            <div style={{ width: '520px', maxHeight: '75vh', display: 'flex', flexDirection: 'column' }}>
                {!selectedUser ? (
                    <>
                        <h3 style={{ textAlign: 'center', marginBottom: '12px' }}>유저 관리</h3>
                        <div style={{ flex: 1, overflowY: 'auto' }}>
                            {users.map(u => (
                                <button key={u.username} onClick={() => openUser(u)} style={styles.userRow}>
                                    <span>{u.nickname} <span style={{ color: '#666' }}>({u.username})</span></span>
                                    <RankBadge rank={u.rank} size={16} showLabel={false} />
                                </button>
                            ))}
                        </div>
                        <button onClick={onClose} style={styles.closeButton}>닫기</button>
                    </>
                ) : (
                    <>
                        <button onClick={() => setSelectedUser(null)} style={styles.backButton}>← 목록으로</button>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', margin: '8px 0 12px 0' }}>
                            <h3 style={{ margin: 0 }}>{selectedUser.nickname} ({selectedUser.username})</h3>
                            {selectedUser.rank && <RankBadge rank={selectedUser.rank} size={18} />}
                        </div>

                        <div style={{ display: 'flex', gap: '4px', marginBottom: '12px', flexWrap: 'wrap' }}>
                            {(['OVERVIEW', 'ORDERS', 'AUTO_ORDERS', 'OTOCO', 'TRAILING', 'ALERTS'] as const).map(tab => (
                                <button
                                    key={tab}
                                    onClick={() => setDetailTab(tab)}
                                    style={{ ...styles.tabButton, ...(detailTab === tab ? styles.tabActive : {}) }}
                                >
                                    {tab === 'OVERVIEW' ? '개요'
                                        : tab === 'ORDERS' ? `주문(${orders.length})`
                                            : tab === 'AUTO_ORDERS' ? `자동주문(${autoOrders.length})`
                                                : tab === 'OTOCO' ? `OTOCO(${otocos.length})`
                                                    : tab === 'TRAILING' ? `트레일링(${trailingStops.length})`
                                                        : `알림(${alerts.length})`}
                                </button>
                            ))}
                        </div>

                        {loading ? (
                            <div style={{ color: '#666', fontSize: '13px', textAlign: 'center', padding: '20px 0' }}>불러오는 중...</div>
                        ) : (
                            <div style={{ flex: 1, overflowY: 'auto', fontSize: '13px' }}>
                                {detailTab === 'OVERVIEW' && (
                                    <>
                                        <div style={styles.row}><span>총 자산</span><span>{(account?.totalValue ?? 0).toLocaleString()}원</span></div>
                                        <div style={styles.row}><span>현금</span><span>{(account?.totalCash ?? 0).toLocaleString()}원</span></div>
                                        <div style={styles.row}><span>총 손익</span><span>{(account?.totalProfit ?? 0).toLocaleString()}원 ({(account?.totalProfitRate ?? 0).toFixed(2)}%)</span></div>
                                        <div style={styles.row}><span>레버리지 순자산</span><span>{(account?.leverageNetValue ?? 0).toLocaleString()}원</span></div>
                                        <div style={styles.row}><span>레버리지 대출금</span><span>{(account?.leverageLoanTotal ?? 0).toLocaleString()}원</span></div>

                                        <div style={{ borderTop: '1px solid #333', margin: '10px 0' }} />

                                        <div style={{ fontSize: '12px', color: '#AAA', marginBottom: '6px' }}>포트폴리오 (업종별)</div>
                                        {(portfolio?.sectors ?? []).length === 0 ? (
                                            <div style={{ color: '#666', fontSize: '12px' }}>보유 종목 없음</div>
                                        ) : (
                                            portfolio!.sectors.map(s => (
                                                <div key={s.sector} style={styles.row}>
                                                    <span>{s.sector}</span>
                                                    <span>{s.evaluationAmount.toLocaleString()}원 ({s.ratioInTotal.toFixed(1)}%)</span>
                                                </div>
                                            ))
                                        )}
                                    </>
                                )}

                                {detailTab === 'ORDERS' && (
                                    orders.length === 0 ? <EmptyRow /> : orders.map(o => (
                                        <div key={o.orderId} style={styles.itemCard}>
                                            <div style={styles.itemHeader}>
                                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                                <span style={{ color: o.orderType === 'BUY' ? '#FF6347' : '#4F9DFF' }}>
                                                    {o.orderType === 'BUY' ? '매수' : '매도'}
                                                </span>
                                            </div>
                                            <div style={styles.itemSub}>
                                                {o.orderPrice.toLocaleString()}원 · {o.orderQuantity - o.remainingQuantity}/{o.orderQuantity}주 체결
                                                {o.leverageRatio && o.leverageRatio !== 'SPOT' && <> · {o.leverageRatio.replace('X', '').replace('_', '.')}배</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'AUTO_ORDERS' && (
                                    autoOrders.length === 0 ? <EmptyRow /> : autoOrders.map(o => (
                                        <div key={o.autoOrderId} style={styles.itemCard}>
                                            <div style={styles.itemHeader}>
                                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                                <span style={{ color: o.autoOrderType === 'BUY' ? '#FF6347' : '#4F9DFF' }}>
                                                    {o.autoOrderType === 'BUY' ? '매수' : '매도'}
                                                </span>
                                            </div>
                                            <div style={styles.itemSub}>
                                                트리거 {o.triggerPrice.toLocaleString()}원 → {o.orderPrice.toLocaleString()}원 · {o.orderQuantity}주
                                                {o.leverageRatio && o.leverageRatio !== 'SPOT' && <> · {o.leverageRatio.replace('X', '').replace('_', '.')}배</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'OTOCO' && (
                                    otocos.length === 0 ? <EmptyRow /> : otocos.map(o => (
                                        <div key={o.otocoId} style={styles.itemCard}>
                                            <div style={styles.itemHeader}>
                                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                                <span style={{ color: '#AAA' }}>{OTOCO_STATUS_LABEL[o.otocoStatus] ?? o.otocoStatus}</span>
                                            </div>
                                            <div style={styles.itemSub}>
                                                진입 {o.entryDirection === 'ABOVE' ? '≥' : '≤'} {o.entryTriggerPrice.toLocaleString()}원 · {o.orderQuantity}주
                                                {o.tpTriggerPrice && <> · TP {o.tpTriggerPrice.toLocaleString()}원</>}
                                                {o.slTriggerPrice && <> · SL {o.slTriggerPrice.toLocaleString()}원</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'TRAILING' && (
                                    trailingStops.length === 0 ? <EmptyRow /> : trailingStops.map(t => (
                                        <div key={t.trailingStopId} style={styles.itemCard}>
                                            <div style={styles.itemHeader}>
                                                <span>{stockNameMap[t.stockCode] ?? t.stockCode}</span>
                                                <span style={{ color: t.trailingStopType === 'BUY' ? '#FF6347' : '#4F9DFF' }}>
                                                    {t.trailingStopType === 'BUY' ? '매수' : '매도'}
                                                </span>
                                            </div>
                                            <div style={styles.itemSub}>
                                                기준가 {t.basePrice.toLocaleString()}원 · 추적 {t.stopPercent}% · {t.orderQuantity}주
                                                {t.triggerPrice != null && <> · 현재 감시가 {t.triggerPrice.toLocaleString()}원</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'ALERTS' && (
                                    alerts.length === 0 ? <EmptyRow /> : alerts.map(a => (
                                        <div key={a.alertId} style={styles.itemCard}>
                                            <div style={styles.itemHeader}>
                                                <span>{stockNameMap[a.stockCode] ?? a.stockCode}</span>
                                                <span>{a.triggerPrice.toLocaleString()}원 {a.direction === 'ABOVE' ? '이상' : '이하'}</span>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        )}

                        <button onClick={onClose} style={styles.closeButton}>닫기</button>
                    </>
                )}
            </div>
        </Modal>
    )
}

function EmptyRow() {
    return <div style={{ color: '#666', fontSize: '12px', textAlign: 'center', padding: '16px 0' }}>항목 없음</div>
}

const styles = {
    userRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', padding: '8px 4px', borderBottom: '1px solid #262626', background: 'none', border: 'none', color: '#FFF', cursor: 'pointer', fontSize: '13px' },
    row: { display: 'flex', justifyContent: 'space-between', padding: '4px 0', color: '#DDD' },
    backButton: { alignSelf: 'flex-start', background: 'none', border: 'none', color: '#4F9DFF', fontSize: '12px', cursor: 'pointer', padding: 0 },
    closeButton: { padding: '8px', fontSize: '13px', backgroundColor: '#333', color: '#FFF', border: 'none', borderRadius: '6px', cursor: 'pointer', marginTop: '8px' },
    tabButton: { padding: '5px 9px', fontSize: '11px', borderRadius: '4px', border: '1px solid #555', backgroundColor: '#222', color: '#FFF', cursor: 'pointer' },
    tabActive: { backgroundColor: '#4F9DFF', borderColor: '#4F9DFF' },
    itemCard: { backgroundColor: '#222', border: '1px solid #333', borderRadius: '6px', padding: '8px 10px', marginBottom: '6px' },
    itemHeader: { display: 'flex', justifyContent: 'space-between', fontSize: '13px' },
    itemSub: { fontSize: '11px', color: '#AAA', marginTop: '4px' },
} as const
