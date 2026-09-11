import api from '../lib/api'
import type {
    TrailingStopRequest,
    TrailingStopResponse,
    TrailingStopCancelRequest,
    TrailingStopCancelResponse,
} from '../types/trailingStop'

export async function placeTrailingStop(req: TrailingStopRequest): Promise<TrailingStopResponse> {
    const res = await api.post<TrailingStopResponse>('/trailing-stops', req)
    return res.data
}

export async function cancelTrailingStop(req: TrailingStopCancelRequest): Promise<TrailingStopCancelResponse> {
    const res = await api.post<TrailingStopCancelResponse>('/trailing-stops/cancel', req)
    return res.data
}
