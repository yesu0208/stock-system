import instance from './axios'
import type {
    CursorPage, PostSummary, PostDetail, CommentResponse, ReactionResponse, ReactionType,
    ScrapResponse, PostCreateRequest,
} from '../types/discussion'

export async function getPostsByStock(stockCode: string, cursor?: number): Promise<CursorPage<PostSummary>> {
    const res = await instance.get<CursorPage<PostSummary>>(`/discussions/stocks/${stockCode}`, {
        params: cursor ? { cursor } : {},
    })
    return res.data
}

export async function getMyPosts(cursor?: number): Promise<CursorPage<PostSummary>> {
    const res = await instance.get<CursorPage<PostSummary>>('/discussions/my/posts', {
        params: cursor ? { cursor } : {},
    })
    return res.data
}

export async function getPostsICommentedOn(cursor?: number): Promise<CursorPage<PostSummary>> {
    const res = await instance.get<CursorPage<PostSummary>>('/discussions/my/commented', {
        params: cursor ? { cursor } : {},
    })
    return res.data
}

export async function getScrappedPosts(cursor?: number): Promise<CursorPage<PostSummary>> {
    const res = await instance.get<CursorPage<PostSummary>>('/discussions/my/scraps', {
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

export async function editPost(postId: number, title: string, content: string): Promise<PostDetail> {
    const res = await instance.patch<PostDetail>(`/discussions/${postId}`, { title, content })
    return res.data
}

export async function deletePost(postId: number): Promise<void> {
    await instance.delete(`/discussions/${postId}`)
}

export async function addComment(postId: number, content: string): Promise<CommentResponse> {
    const res = await instance.post<CommentResponse>(`/discussions/${postId}/comments`, { content })
    return res.data
}

export async function editComment(postId: number, commentId: number, content: string): Promise<CommentResponse> {
    const res = await instance.patch<CommentResponse>(`/discussions/${postId}/comments/${commentId}`, { content })
    return res.data
}

export async function deleteComment(postId: number, commentId: number): Promise<void> {
    await instance.delete(`/discussions/${postId}/comments/${commentId}`)
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
