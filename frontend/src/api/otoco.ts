import api from '../lib/api'
import type { OtocoRequest, OtocoResponse, OtocoCancelRequest, OtocoCancelResponse } from '../types/otoco'

export async function placeOtocoOrder(req: OtocoRequest): Promise<OtocoResponse> {
    const res = await api.post<OtocoResponse>('/otocos', req)
    return res.data
}

export async function cancelOtocoOrder(req: OtocoCancelRequest): Promise<OtocoCancelResponse> {
    const res = await api.post<OtocoCancelResponse>('/otocos/cancel', req)
    return res.data
}
