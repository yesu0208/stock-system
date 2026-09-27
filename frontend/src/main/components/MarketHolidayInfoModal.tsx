import { useEffect, useMemo, useRef, useState } from 'react'
import axios from 'axios'
import { FiChevronLeft, FiChevronRight, FiCalendar, FiRefreshCw } from 'react-icons/fi'
import ModalV2 from '../../components/ModalV2'
import { getMarketHolidays } from '../../api/marketHoliday'
import type { MarketHolidayCalendarResponse } from '../../types/marketHoliday'
import './MarketHolidayInfoModal.css'

interface Props {
    open: boolean
    onClose: () => void
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']

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

function dDayLabel(today: string, key: string): string {
    const diff = diffDays(today, key)
    return diff === 0 ? 'D-DAY' : `D-${diff}`
}

// 연*12+월 로 비교하기 위한 값
function monthIndex(y: number, m: number): number {
    return y * 12 + m
}

function errorMessage(e: unknown): string {
    if (axios.isAxiosError<{ message?: string }>(e)) {
        return e.response?.data?.message ?? '휴장 정보를 불러오지 못했습니다.'
    }
    return '휴장 정보를 불러오지 못했습니다.'
}

export default function MarketHolidayInfoModal({ open, onClose }: Props) {
    const [data, setData] = useState<MarketHolidayCalendarResponse | null>(null)
    const [loading, setLoading] = useState(false)
    const [errorMsg, setErrorMsg] = useState<string | null>(null)

    const [viewYear, setViewYear] = useState(0)
    const [viewMonth, setViewMonth] = useState(0)
    const [selected, setSelected] = useState<string | null>(null)

    const itemRefs = useRef<Record<string, HTMLDivElement | null>>({})

    const load = async () => {
        setLoading(true)
        setErrorMsg(null)
        try {
            const res = await getMarketHolidays()
            setData(res)
            const { y, m } = parseKey(res.today)
            setViewYear(y)
            setViewMonth(m)
        } catch (e) {
            setErrorMsg(errorMessage(e))
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (!open) return
        setSelected(null)
        load()
    }, [open])

    const today = data?.today ?? null
    const lastDate = data?.lastHolidayDate ?? null

    const holidayMap = useMemo(
        () => new Map((data?.holidays ?? []).map(h => [h.holidayDate, h])),
        [data],
    )

    // ── 조회 가능한 월 범위: 오늘이 속한 달 ~ 마지막 휴장일이 속한 달 ──
    const minMonth = today ? monthIndex(parseKey(today).y, parseKey(today).m) : 0
    const maxMonth = lastDate ? monthIndex(parseKey(lastDate).y, parseKey(lastDate).m) : minMonth
    const curMonth = monthIndex(viewYear, viewMonth)

    const cells = useMemo(() => {
        if (!today) return []
        const firstDay = new Date(viewYear, viewMonth, 1).getDay()
        const lastDateOfMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
        const result: (string | null)[] = Array(firstDay).fill(null)
        for (let d = 1; d <= lastDateOfMonth; d++) result.push(toKey(viewYear, viewMonth, d))
        while (result.length % 7 !== 0) result.push(null)
        return result
    }, [today, viewYear, viewMonth])

    const moveMonth = (delta: number) => {
        const next = curMonth + delta
        if (next < minMonth || next > maxMonth) return
        setViewYear(Math.floor(next / 12))
        setViewMonth(next % 12)
    }

    const selectHoliday = (key: string) => {
        setSelected(key)
        const { y, m } = parseKey(key)
        setViewYear(y)
        setViewMonth(m)
        itemRefs.current[key]?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
    }

    const holidays = data?.holidays ?? []
    const next = holidays[0]
    const isTodayHoliday = !!today && holidayMap.has(today)

    return (
        <ModalV2 open={open} title="휴장 정보" onClose={onClose}>
            <div className="holiday-info-modal__content">
                {loading && !data ? (
                    <div className="hi-state">불러오는 중...</div>
                ) : errorMsg && !data ? (
                    <div className="hi-state">
                        <span>{errorMsg}</span>
                        <button type="button" className="hi-retry" onClick={load}>
                            <FiRefreshCw />
                            다시 시도
                        </button>
                    </div>
                ) : data && today && (
                    <>
                        {/* ════ 달력 ════ */}
                        <section className="hi-calendar">
                            <div className="hi-calendar__head">
                                <button
                                    type="button"
                                    className="hi-icon-btn"
                                    onClick={() => moveMonth(-1)}
                                    disabled={curMonth <= minMonth}
                                    aria-label="이전 달"
                                >
                                    <FiChevronLeft />
                                </button>
                                <div className="hi-calendar__title">
                                    <span className="hi-calendar__year">{viewYear}</span>
                                    <span className="hi-calendar__month">{viewMonth + 1}월</span>
                                </div>
                                <button
                                    type="button"
                                    className="hi-icon-btn"
                                    onClick={() => moveMonth(1)}
                                    disabled={curMonth >= maxMonth}
                                    aria-label="다음 달"
                                >
                                    <FiChevronRight />
                                </button>
                            </div>

                            <div className="hi-calendar__weekdays">
                                {WEEKDAYS.map((w, i) => (
                                    <span key={w} className={i === 0 ? 'is-sun' : i === 6 ? 'is-sat' : ''}>{w}</span>
                                ))}
                            </div>

                            <div className="hi-calendar__grid">
                                {cells.map((key, idx) => {
                                    if (!key) return <span key={`empty-${idx}`} className="hi-day hi-day--empty" />

                                    const wd = idx % 7
                                    // 오늘 이전, 마지막 휴장일 이후는 조회 범위 밖
                                    const outOfRange = key < today || key > (lastDate ?? today)
                                    const isHoliday = holidayMap.has(key)

                                    const cls = [
                                        'hi-day',
                                        wd === 0 && 'is-sun',
                                        wd === 6 && 'is-sat',
                                        outOfRange && 'is-out',
                                        key === today && 'is-today',
                                        isHoliday && 'is-holiday',
                                        key === selected && 'is-selected',
                                    ].filter(Boolean).join(' ')

                                    if (outOfRange) {
                                        return <span key={key} className={cls} />
                                    }

                                    return isHoliday ? (
                                        <button
                                            key={key}
                                            type="button"
                                            className={cls}
                                            onClick={() => selectHoliday(key)}
                                            title={holidayMap.get(key)?.memo ?? '휴장일'}
                                        >
                                            <span className="hi-day__num">{parseKey(key).d}</span>
                                            <span className="hi-day__dot" />
                                        </button>
                                    ) : (
                                        <span key={key} className={cls}>
                                            <span className="hi-day__num">{parseKey(key).d}</span>
                                        </span>
                                    )
                                })}
                            </div>

                            <div className="hi-legend">
                                <span><i className="hi-legend__dot hi-legend__dot--holiday" />휴장일</span>
                                <span><i className="hi-legend__dot hi-legend__dot--today" />오늘</span>
                                <span className="hi-legend__note">
                                    {lastDate ? `${formatFull(lastDate)}까지 공지됨` : '공지된 휴장일 없음'}
                                </span>
                            </div>
                        </section>

                        {/* ════ 우측 패널 ════ */}
                        <aside className="hi-side">
                            {/* 다음 휴장일 */}
                            <section className={`hi-next${isTodayHoliday ? ' is-today' : ''}`}>
                                {next ? (
                                    <>
                                        <div className="hi-next__head">
                                            <span className="hi-next__label">
                                                {isTodayHoliday ? '오늘은 휴장일입니다' : '다음 휴장일'}
                                            </span>
                                            <span className="hi-badge">{dDayLabel(today, next.holidayDate)}</span>
                                        </div>
                                        <div className="hi-next__date">{formatFull(next.holidayDate)}</div>
                                        <div className="hi-next__memo">{next.memo || '임시 휴장'}</div>
                                    </>
                                ) : (
                                    <div className="hi-next__empty">
                                        <FiCalendar />
                                        <span>예정된 휴장일이 없습니다</span>
                                        <span className="hi-next__sub">주말을 제외한 모든 평일에 정상 개장합니다</span>
                                    </div>
                                )}
                            </section>

                            {/* 휴장일 목록 */}
                            <section className="hi-list">
                                <div className="hi-list__head">
                                    <span className="hi-list__title">예정된 휴장일</span>
                                    <span className="hi-list__count">{holidays.length}</span>
                                </div>
                                <div className="hi-list__body">
                                    {holidays.length === 0 ? (
                                        <div className="hi-list__empty">공지된 휴장일이 없습니다</div>
                                    ) : holidays.map(h => (
                                        <div
                                            key={h.holidayDate}
                                            ref={el => { itemRefs.current[h.holidayDate] = el }}
                                            className={`hi-item${h.holidayDate === selected ? ' is-active' : ''}`}
                                            onClick={() => selectHoliday(h.holidayDate)}
                                        >
                                            <div className="hi-item__date">
                                                <span className="hi-item__md">
                                                    {h.holidayDate.slice(5).replace('-', '.')}
                                                </span>
                                                <span className="hi-item__wd">{WEEKDAYS[weekdayOf(h.holidayDate)]}</span>
                                            </div>
                                            <div className="hi-item__info">
                                                <span className="hi-item__memo">{h.memo || '임시 휴장'}</span>
                                                <span className="hi-item__meta">{h.holidayDate.slice(0, 4)}</span>
                                            </div>
                                            <span className="hi-item__dday">{dDayLabel(today, h.holidayDate)}</span>
                                        </div>
                                    ))}
                                </div>
                            </section>
                        </aside>
                    </>
                )}
            </div>
        </ModalV2>
    )
}
