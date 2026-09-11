import type { CursorPage } from './cursorPage'

export type { CursorPage }

export interface NoticeSummary {
    noticeId: number
    title: string
    authorId: string
    createdDateTime: string
    contentPreview: string
}

export interface NoticeDetail {
    noticeId: number
    title: string
    content: string
    authorId: string
    createdDateTime: string
    updatedDateTime: string
}
