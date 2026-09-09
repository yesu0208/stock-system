import axios from './axios'

export interface NaverNewsItem {
    title: string
    originallink: string
    link: string
    description: string
    pubDate: string
}

export const getNews = (keyword: string) =>
    axios.get<NaverNewsItem[]>('/news', { params: { keyword } }).then(r => r.data)
