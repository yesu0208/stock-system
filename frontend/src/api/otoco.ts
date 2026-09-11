import api from '../lib/api'
import type { OtocoRequest, OtocoResponse } from '../types/otoco'

export async function placeOtocoOrder(req: OtocoRequest): Promise<OtocoResponse> {
    const res = await api.post<OtocoResponse>('/otocos', req)
    return res.data
}
