export type { CursorPage } from './cursorPage'

export type ReactionType = 'LIKE' | 'DISLIKE'

export interface PostSummary {
    postId: number
    stockCode: string
    stockName: string
    title: string
    authorId: string
    authorNickname: string
    authorProfileImageUrl: string | null
    createdDateTime: string
    contentPreview: string
    likes: number
    dislikes: number
    commentCount: number
    scraps: number
}

export interface CommentResponse {
    commentId: number
    authorId: string
    authorNickname: string
    authorProfileImageUrl: string | null
    createdDateTime: string
    updatedDateTime: string
    content: string
    likes: number
    dislikes: number
}

export interface PostDetail {
    postId: number
    stockCode: string
    stockName: string
    title: string
    authorId: string
    authorNickname: string
    authorProfileImageUrl: string | null
    createdDateTime: string
    updatedDateTime: string
    content: string
    likes: number
    dislikes: number
    scraps: number
    comments: CommentResponse[]
}

export interface ReactionResponse {
    likes: number
    dislikes: number
}

export interface ScrapResponse {
    scraps: number
    scrapped: boolean
}

export interface PostCreateRequest {
    stockCode: string
    stockName: string
    title: string
    content: string
}
