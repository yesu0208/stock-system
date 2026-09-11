import instance from './axios'
import type {
    CursorPage, PostSummary, PostDetail, ReactionResponse, ReactionType,
    ScrapResponse, PostCreateRequest,
} from '../types/discussion'

/**
 * 커뮤니티/토론 기능도 트레이딩 액션이 아니므로 axios.ts(JWT + refresh)를 사용
 */

export async function getPostsByStock(stockCode: string, cursor?: number): Promise<CursorPage<PostSummary>> {
    const res = await instance.get<CursorPage<PostSummary>>(`/discussions/stocks/${stockCode}`, {
        params: cursor ? { cursor } : {},
    })
    return res.data
}

export async function getPost(postId: number): Promise<PostDetail> {
    const res = await instance.get<PostDetail>(`/discussions/${postId}`)
    return res.data
}

export async function createPost(req: PostCreateRequest): Promise<PostDetail> {
    const res = await instance.post<PostDetail>('/discussions', req)
    return res.data
}

export async function addComment(postId: number, content: string): Promise<void> {
    await instance.post(`/discussions/${postId}/comments`, { content })
}

export async function reactToPost(postId: number, reactionType: ReactionType): Promise<ReactionResponse> {
    const res = await instance.post<ReactionResponse>(`/discussions/${postId}/reactions`, { reactionType })
    return res.data
}

export async function reactToComment(postId: number, commentId: number, reactionType: ReactionType): Promise<ReactionResponse> {
    const res = await instance.post<ReactionResponse>(`/discussions/${postId}/comments/${commentId}/reactions`, { reactionType })
    return res.data
}

export async function toggleScrap(postId: number): Promise<ScrapResponse> {
    const res = await instance.post<ScrapResponse>(`/discussions/${postId}/scrap`)
    return res.data
}
