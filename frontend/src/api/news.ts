import instance from './axios'
import type { NaverNewsItem } from '../types/news'

export async function searchNews(keyword: string): Promise<NaverNewsItem[]> {
    const res = await instance.get<NaverNewsItem[]>('/news', { params: { keyword } })
    return res.data
}
