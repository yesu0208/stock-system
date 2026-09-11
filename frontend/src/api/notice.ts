import instance from './axios'
import type { CursorPage, NoticeSummary, NoticeDetail } from '../types/notice'

export async function getNotices(cursor?: number): Promise<CursorPage<NoticeSummary>> {
    const res = await instance.get<CursorPage<NoticeSummary>>('/notices', {
        params: cursor ? { cursor } : {},
    })
    return res.data
}

export async function getNotice(noticeId: number): Promise<NoticeDetail> {
    const res = await instance.get<NoticeDetail>(`/notices/${noticeId}`)
    return res.data
}
