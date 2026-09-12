import { useEffect, useState } from 'react'
import ModalV2 from '../../components/ModalV2'
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
import './ManagedModal.css'

interface Props {
    open: boolean
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

const TAB_LABEL: Record<DetailTab, string> = {
    OVERVIEW: '개요',
    ORDERS: '주문',
    AUTO_ORDERS: '자동주문',
    OTOCO: 'OTOCO',
    TRAILING: '트레일링',
    ALERTS: '알림',
}

function EmptyRow() {
    return <div className="managed-modal__empty">데이터가 없습니다.</div>
}

export default function ManagedModal({ open, onClose }: Props) {
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
        if (!open) return
        setSelectedUser(null)
        getAllUsersAdmin().then(setUsers).catch(() => setUsers([]))
    }, [open])

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

    return (
        <ModalV2 open={open} title="관리자" onClose={onClose}>
            <div className="managed-modal__content">
                {!selectedUser ? (
                    <>
                        <p className="managed-modal__section-label">전체 회원</p>
                        <div className="managed-modal__user-list">
                            {users.length === 0 ? (
                                <EmptyRow />
                            ) : (
                                users.map(u => (
                                    <button
                                        key={u.username}
                                        onClick={() => openUser(u)}
                                        className="managed-modal__user-row"
                                    >
                                        <span className="managed-modal__user-info">
                                            <span className="managed-modal__user-nickname">{u.nickname}</span>
                                            <span className="managed-modal__user-id">({u.username})</span>
                                        </span>
                                        <RankBadge rank={u.rank} size={16} showLabel={false} />
                                    </button>
                                ))
                            )}
                        </div>
                    </>
                ) : (
                    <>
                        <button
                            onClick={() => setSelectedUser(null)}
                            className="managed-modal__back"
                        >
                            ← 목록으로
                        </button>

                        <div className="managed-modal__user-header">
                            <h4 className="managed-modal__user-header-name">
                                {selectedUser.nickname}
                                <span className="managed-modal__user-id">({selectedUser.username})</span>
                            </h4>
                            {selectedUser.rank && <RankBadge rank={selectedUser.rank} size={18} />}
                        </div>

                        <div className="managed-modal__tabs">
                            {(Object.keys(TAB_LABEL) as DetailTab[]).map(tab => (
                                <button
                                    key={tab}
                                    onClick={() => setDetailTab(tab)}
                                    className={`managed-modal__tab${detailTab === tab ? ' active' : ''}`}
                                >
                                    {TAB_LABEL[tab]}
                                    {tab === 'ORDERS' && ` (${orders.length})`}
                                    {tab === 'AUTO_ORDERS' && ` (${autoOrders.length})`}
                                    {tab === 'OTOCO' && ` (${otocos.length})`}
                                    {tab === 'TRAILING' && ` (${trailingStops.length})`}
                                    {tab === 'ALERTS' && ` (${alerts.length})`}
                                </button>
                            ))}
                        </div>

                        {loading ? (
                            <div className="managed-modal__loading">불러오는 중...</div>
                        ) : (
                            <div className="managed-modal__detail-body">
                                {detailTab === 'OVERVIEW' && (
                                    <>
                                        <div className="managed-modal__section">
                                            <div className="managed-modal__row">
                                                <span>총 자산</span>
                                                <span>{(account?.totalValue ?? 0).toLocaleString()}원</span>
                                            </div>
                                            <div className="managed-modal__row">
                                                <span>현금</span>
                                                <span>{(account?.totalCash ?? 0).toLocaleString()}원</span>
                                            </div>
                                            <div className="managed-modal__row">
                                                <span>총 손익</span>
                                                <span>
                                                    {(account?.totalProfit ?? 0).toLocaleString()}원
                                                    ({(account?.totalProfitRate ?? 0).toFixed(2)}%)
                                                </span>
                                            </div>
                                            <div className="managed-modal__row">
                                                <span>레버리지 순자산</span>
                                                <span>{(account?.leverageNetValue ?? 0).toLocaleString()}원</span>
                                            </div>
                                            <div className="managed-modal__row">
                                                <span>레버리지 대출금</span>
                                                <span>{(account?.leverageLoanTotal ?? 0).toLocaleString()}원</span>
                                            </div>
                                        </div>

                                        <p className="managed-modal__section-label">포트폴리오 (업종별)</p>
                                        <div className="managed-modal__section">
                                            {(portfolio?.sectors ?? []).length === 0 ? (
                                                <EmptyRow />
                                            ) : (
                                                portfolio!.sectors.map(s => (
                                                    <div key={s.sector} className="managed-modal__row">
                                                        <span>{s.sector}</span>
                                                        <span>
                                                            {s.evaluationAmount.toLocaleString()}원
                                                            ({s.ratioInTotal.toFixed(1)}%)
                                                        </span>
                                                    </div>
                                                ))
                                            )}
                                        </div>
                                    </>
                                )}

                                {detailTab === 'ORDERS' && (
                                    orders.length === 0 ? <EmptyRow /> : orders.map(o => (
                                        <div key={o.orderId} className="managed-modal__item-card">
                                            <div className="managed-modal__item-header">
                                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                                <span style={{ color: o.orderType === 'BUY' ? '#f87171' : '#60a5fa' }}>
                                                    {o.orderType === 'BUY' ? '매수' : '매도'}
                                                </span>
                                            </div>
                                            <div className="managed-modal__item-sub">
                                                {o.orderPrice.toLocaleString()}원 · {o.orderQuantity - o.remainingQuantity}/{o.orderQuantity}주 체결
                                                {o.leverageRatio && o.leverageRatio !== 'SPOT' && <> · {o.leverageRatio.replace('X', '').replace('_', '.')}배</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'AUTO_ORDERS' && (
                                    autoOrders.length === 0 ? <EmptyRow /> : autoOrders.map(o => (
                                        <div key={o.autoOrderId} className="managed-modal__item-card">
                                            <div className="managed-modal__item-header">
                                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                                <span style={{ color: o.autoOrderType === 'BUY' ? '#f87171' : '#60a5fa' }}>
                                                    {o.autoOrderType === 'BUY' ? '매수' : '매도'}
                                                </span>
                                            </div>
                                            <div className="managed-modal__item-sub">
                                                트리거 {o.triggerPrice.toLocaleString()}원 → {o.orderPrice.toLocaleString()}원 · {o.orderQuantity}주
                                                {o.leverageRatio && o.leverageRatio !== 'SPOT' && <> · {o.leverageRatio.replace('X', '').replace('_', '.')}배</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'OTOCO' && (
                                    otocos.length === 0 ? <EmptyRow /> : otocos.map(o => (
                                        <div key={o.otocoId} className="managed-modal__item-card">
                                            <div className="managed-modal__item-header">
                                                <span>{stockNameMap[o.stockCode] ?? o.stockCode}</span>
                                                <span className="managed-modal__item-status">
                                                    {OTOCO_STATUS_LABEL[o.otocoStatus] ?? o.otocoStatus}
                                                </span>
                                            </div>
                                            <div className="managed-modal__item-sub">
                                                진입 {o.entryDirection === 'ABOVE' ? '≥' : '≤'} {o.entryTriggerPrice.toLocaleString()}원 · {o.orderQuantity}주
                                                {o.tpTriggerPrice && <> · TP {o.tpTriggerPrice.toLocaleString()}원</>}
                                                {o.slTriggerPrice && <> · SL {o.slTriggerPrice.toLocaleString()}원</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'TRAILING' && (
                                    trailingStops.length === 0 ? <EmptyRow /> : trailingStops.map(t => (
                                        <div key={t.trailingStopId} className="managed-modal__item-card">
                                            <div className="managed-modal__item-header">
                                                <span>{stockNameMap[t.stockCode] ?? t.stockCode}</span>
                                                <span style={{ color: t.trailingStopType === 'BUY' ? '#f87171' : '#60a5fa' }}>
                                                    {t.trailingStopType === 'BUY' ? '매수' : '매도'}
                                                </span>
                                            </div>
                                            <div className="managed-modal__item-sub">
                                                기준가 {t.basePrice.toLocaleString()}원 · 추적 {t.stopPercent}% · {t.orderQuantity}주
                                                {t.triggerPrice != null && <> · 현재 감시가 {t.triggerPrice.toLocaleString()}원</>}
                                            </div>
                                        </div>
                                    ))
                                )}

                                {detailTab === 'ALERTS' && (
                                    alerts.length === 0 ? <EmptyRow /> : alerts.map(a => (
                                        <div key={a.alertId} className="managed-modal__item-card">
                                            <div className="managed-modal__item-header">
                                                <span>{stockNameMap[a.stockCode] ?? a.stockCode}</span>
                                                <span>
                                                    {a.triggerPrice.toLocaleString()}원 {a.direction === 'ABOVE' ? '이상' : '이하'}
                                                </span>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        )}
                    </>
                )}
            </div>
        </ModalV2>
    )
}
