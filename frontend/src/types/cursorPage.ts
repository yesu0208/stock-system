export interface CursorPage<T> {
    items: T[]
    nextCursor: number | null
    hasNext: boolean
}
