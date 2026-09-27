import axios from 'axios'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { FiChevronLeft, FiChevronRight, FiTrash2, FiPlus, FiCalendar } from 'react-icons/fi'
import ModalV2 from '../../components/ModalV2'
import { useMsg } from '../context/MsgContext'
import {
    getMarketHolidaysAdmin, addMarketHolidayAdmin, removeMarketHolidayAdmin,
} from '../../api/marketHoliday'
import type { MarketHolidayResponse } from '../../types/marketHoliday'
import './HolidayModal.css'

interface Props {
    open: boolean
    onClose: () => void
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
const MEMO_MAX = 30

// 백엔드(stock-server)와 동일하게 KST 기준 "오늘"을 YYYY-MM-DD로 계산
function todayKst(): string {
    return new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Seoul' })
}

function toKey(y: number, m: number, d: number): string {
    return `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
}

function parseKey(key: string) {
    const [y, m, d] = key.split('-').map(Number)
    return { y, m: m - 1, d }
}

function weekdayOf(key: string): number {
    const { y, m, d } = parseKey(key)
    return new Date(y, m, d).getDay()
}

function formatFull(key: string): string {
    const { y, m, d } = parseKey(key)
    return `${y}. ${String(m + 1).padStart(2, '0')}. ${String(d).padStart(2, '0')} (${WEEKDAYS[weekdayOf(key)]})`
}

function diffDays(from: string, to: string): number {
    const a = parseKey(from)
    const b = parseKey(to)
    return Math.round((Date.UTC(b.y, b.m, b.d) - Date.UTC(a.y, a.m, a.d)) / 86_400_000)
}

function errorMessage(e: unknown, fallback: string): string {
    if (axios.isAxiosError<{ message?: string }>(e)) return e.response?.data?.message ?? fallback
    return fallback
}

export default function HolidayModal({ open, onClose }: Props) {
    const { success, error } = useMsg()

    const [today, setToday] = useState(todayKst)
    const [holidays, setHolidays] = useState<MarketHolidayResponse[]>([])
    const [loading, setLoading] = useState(false)

    const [viewYear, setViewYear] = useState(() => parseKey(todayKst()).y)
    const [viewMonth, setViewMonth] = useState(() => parseKey(todayKst()).m)
    const [selected, setSelected] = useState<string | null>(null)

    const [memo, setMemo] = useState('')
    const [submitting, setSubmitting] = useState(false)

    // 삭제는 2단계 확인 (휴지통 → "삭제" 한 번 더 클릭)
    const [confirmDelete, setConfirmDelete] = useState<string | null>(null)
    const confirmTimerRef = useRef<number | null>(null)

    const holidayMap = useMemo(
        () => new Map(holidays.map(h => [h.holidayDate, h])),
        [holidays],
    )

    const upcoming = useMemo(
        () => holidays.filter(h => h.holidayDate > today),
        [holidays, today],
    )

    const loadHolidays = useCallback(async () => {
        setLoading(true)
        try {
            const list = await getMarketHolidaysAdmin()
            setHolidays([...list].sort((a, b) => a.holidayDate.localeCompare(b.holidayDate)))
        } catch (e) {
            error(errorMessage(e, '휴장일 목록을 불러오지 못했습니다.'))
        } finally {
            setLoading(false)
        }
    }, [error])

    useEffect(() => {
        if (!open) return
        const t = todayKst()
        const { y, m } = parseKey(t)
        setToday(t)
        setViewYear(y)
        setViewMonth(m)
        setSelected(null)
        setMemo('')
        setConfirmDelete(null)
        loadHolidays()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [open])

    useEffect(() => () => {
        if (confirmTimerRef.current) window.clearTimeout(confirmTimerRef.current)
    }, [])

    // ── 달력 셀 계산 ──
    const cells = useMemo(() => {
        const firstDay = new Date(viewYear, viewMonth, 1).getDay()
        const lastDate = new Date(viewYear, viewMonth + 1, 0).getDate()
        const result: (string | null)[] = Array(firstDay).fill(null)
        for (let d = 1; d <= lastDate; d++) result.push(toKey(viewYear, viewMonth, d))
        while (result.length % 7 !== 0) result.push(null)
        return result
    }, [viewYear, viewMonth])

    const { y: todayY, m: todayM } = parseKey(today)
    const isCurrentMonth = viewYear === todayY && viewMonth === todayM

    const moveMonth = (delta: number) => {
        const next = new Date(viewYear, viewMonth + delta, 1)
        setViewYear(next.getFullYear())
        setViewMonth(next.getMonth())
    }

    const goToday = () => {
        setViewYear(todayY)
        setViewMonth(todayM)
    }

    const selectDate = (key: string) => {
        setSelected(key)
        setMemo('')
        setConfirmDelete(null)
        const { y, m } = parseKey(key)
        if (y !== viewYear || m !== viewMonth) {
            setViewYear(y)
            setViewMonth(m)
        }
    }

    // ── 등록 / 삭제 ──
    const handleAdd = async () => {
        if (!selected || submitting) return
        setSubmitting(true)
        try {
            await addMarketHolidayAdmin({ holidayDate: selected, memo: memo.trim() || undefined })
            success(`${formatFull(selected)} 휴장일로 지정되었습니다.`)
            setMemo('')
            await loadHolidays()
        } catch (e) {
            error(errorMessage(e, '휴장일 등록에 실패했습니다.'))
        } finally {
            setSubmitting(false)
        }
    }

    const handleRemove = async (key: string) => {
        if (confirmDelete !== key) {
            setConfirmDelete(key)
            if (confirmTimerRef.current) window.clearTimeout(confirmTimerRef.current)
            confirmTimerRef.current = window.setTimeout(() => setConfirmDelete(null), 3000)
            return
        }

        if (confirmTimerRef.current) window.clearTimeout(confirmTimerRef.current)
        setConfirmDelete(null)
        setSubmitting(true)
        try {
            await removeMarketHolidayAdmin(key)
            success(`${formatFull(key)} 휴장일이 해제되었습니다.`)
            await loadHolidays()
        } catch (e) {
            error(errorMessage(e, '휴장일 삭제에 실패했습니다.'))
        } finally {
            setSubmitting(false)
        }
    }

    // ── 선택된 날짜 상태 ──
    const selectedHoliday = selected ? holidayMap.get(selected) : undefined
    const selectedIsPast = !!selected && selected <= today
    const selectedIsWeekend = !!selected && [0, 6].includes(weekdayOf(selected))
    const dDay = selected ? diffDays(today, selected) : 0

    return (
        <ModalV2 open={open} title="휴무일 지정" onClose={onClose}>
            <div className="holiday-modal__content">
                {/* ════ 달력 ════ */}
                <section className="hm-calendar">
                    <div className="hm-calendar__head">
                        <button type="button" className="hm-icon-btn" onClick={() => moveMonth(-1)} aria-label="이전 달">
                            <FiChevronLeft />
                        </button>
                        <div className="hm-calendar__title">
                            <span className="hm-calendar__year">{viewYear}</span>
                            <span className="hm-calendar__month">{viewMonth + 1}월</span>
                        </div>
                        <button type="button" className="hm-icon-btn" onClick={() => moveMonth(1)} aria-label="다음 달">
                            <FiChevronRight />
                        </button>
                        <button
                            type="button"
                            className="hm-today-btn"
                            onClick={goToday}
                            disabled={isCurrentMonth}
                        >
                            오늘
                        </button>
                    </div>

                    <div className="hm-calendar__weekdays">
                        {WEEKDAYS.map((w, i) => (
                            <span key={w} className={i === 0 ? 'is-sun' : i === 6 ? 'is-sat' : ''}>{w}</span>
                        ))}
                    </div>

                    <div className="hm-calendar__grid">
                        {cells.map((key, idx) => {
                            if (!key) return <span key={`empty-${idx}`} className="hm-day hm-day--empty" />

                            const wd = idx % 7
                            const isPast = key < today
                            const isToday = key === today
                            const isHoliday = holidayMap.has(key)
                            const isSelected = key === selected

                            const cls = [
                                'hm-day',
                                wd === 0 && 'is-sun',
                                wd === 6 && 'is-sat',
                                isPast && 'is-past',
                                isToday && 'is-today',
                                isHoliday && 'is-holiday',
                                isSelected && 'is-selected',
                            ].filter(Boolean).join(' ')

                            return (
                                <button
                                    key={key}
                                    type="button"
                                    className={cls}
                                    onClick={() => selectDate(key)}
                                    title={isHoliday ? holidayMap.get(key)?.memo ?? '휴장일' : undefined}
                                >
                                    <span className="hm-day__num">{parseKey(key).d}</span>
                                    {isHoliday && <span className="hm-day__dot" />}
                                </button>
                            )
                        })}
                    </div>

                    <div className="hm-legend">
                        <span><i className="hm-legend__dot hm-legend__dot--holiday" />휴장일</span>
                        <span><i className="hm-legend__dot hm-legend__dot--today" />오늘</span>
                        <span className="hm-legend__note">오늘 이전 날짜는 지정할 수 없습니다</span>
                    </div>
                </section>

                {/* ════ 우측 패널 ════ */}
                <aside className="hm-side">
                    {/* 선택한 날짜 */}
                    <div className="hm-detail">
                        {!selected ? (
                            <div className="hm-detail__placeholder">
                                <FiCalendar />
                                <span>달력에서 날짜를 선택하세요</span>
                            </div>
                        ) : (
                            <>
                                <div className="hm-detail__head">
                                    <span className="hm-detail__label">선택한 날짜</span>
                                    {selectedHoliday ? (
                                        <span className="hm-badge hm-badge--holiday">휴장일</span>
                                    ) : selectedIsPast ? (
                                        <span className="hm-badge hm-badge--muted">지정 불가</span>
                                    ) : (
                                        <span className="hm-badge">D-{dDay}</span>
                                    )}
                                </div>
                                <div className="hm-detail__date">{formatFull(selected)}</div>

                                {selectedHoliday ? (
                                    <>
                                        <p className="hm-detail__memo">
                                            {selectedHoliday.memo || '메모 없음'}
                                        </p>
                                        {!selectedIsPast && (
                                            <button
                                                type="button"
                                                className={`hm-btn hm-btn--danger${confirmDelete === selected ? ' is-confirm' : ''}`}
                                                onClick={() => handleRemove(selected)}
                                                disabled={submitting}
                                            >
                                                <FiTrash2 />
                                                {confirmDelete === selected ? '한 번 더 누르면 해제됩니다' : '휴장일 해제'}
                                            </button>
                                        )}
                                    </>
                                ) : selectedIsPast ? (
                                    <p className="hm-detail__hint">오늘 이후의 날짜만 휴장일로 지정할 수 있습니다.</p>
                                ) : (
                                    <>
                                        <div className="hm-field">
                                            <input
                                                type="text"
                                                className="hm-input"
                                                value={memo}
                                                maxLength={MEMO_MAX}
                                                placeholder="메모 (예: 임시 휴장, 대체 공휴일)"
                                                onChange={e => setMemo(e.target.value)}
                                                onKeyDown={e => { if (e.key === 'Enter') handleAdd() }}
                                                disabled={submitting}
                                            />
                                            <span className="hm-field__count">{memo.length}/{MEMO_MAX}</span>
                                        </div>
                                        {selectedIsWeekend && (
                                            <p className="hm-detail__hint">주말은 원래 휴장일입니다.</p>
                                        )}
                                        <button
                                            type="button"
                                            className="hm-btn hm-btn--primary"
                                            onClick={handleAdd}
                                            disabled={submitting}
                                        >
                                            <FiPlus />
                                            {submitting ? '지정 중...' : '휴장일로 지정'}
                                        </button>
                                    </>
                                )}
                            </>
                        )}
                    </div>

                    {/* 예정된 휴장일 목록 */}
                    <div className="hm-list">
                        <div className="hm-list__head">
                            <span className="hm-list__title">예정된 휴장일</span>
                            <span className="hm-list__count">{upcoming.length}</span>
                        </div>

                        <div className="hm-list__body">
                            {loading && holidays.length === 0 ? (
                                <div className="hm-list__empty">불러오는 중...</div>
                            ) : upcoming.length === 0 ? (
                                <div className="hm-list__empty">예정된 휴장일이 없습니다</div>
                            ) : (
                                upcoming.map(h => (
                                    <div
                                        key={h.holidayDate}
                                        className={`hm-item${h.holidayDate === selected ? ' is-active' : ''}`}
                                        onClick={() => selectDate(h.holidayDate)}
                                    >
                                        <div className="hm-item__date">
                                            <span className="hm-item__md">
                                                {h.holidayDate.slice(5).replace('-', '.')}
                                            </span>
                                            <span className="hm-item__wd">{WEEKDAYS[weekdayOf(h.holidayDate)]}</span>
                                        </div>
                                        <div className="hm-item__info">
                                            <span className="hm-item__memo">{h.memo || '메모 없음'}</span>
                                            <span className="hm-item__meta">
                                                {h.holidayDate.slice(0, 4)} · D-{diffDays(today, h.holidayDate)}
                                            </span>
                                        </div>
                                        <button
                                            type="button"
                                            className={`hm-item__del${confirmDelete === h.holidayDate ? ' is-confirm' : ''}`}
                                            onClick={e => {
                                                e.stopPropagation()
                                                handleRemove(h.holidayDate)
                                            }}
                                            disabled={submitting}
                                            aria-label="휴장일 해제"
                                        >
                                            {confirmDelete === h.holidayDate ? '해제' : <FiTrash2 />}
                                        </button>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </aside>
            </div>
        </ModalV2>
    )
}
