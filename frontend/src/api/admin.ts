import instance from './axios'
import type { UserDto } from '../types/user'
import type { AccountResponse } from '../types/account'
import type { PortfolioResponse } from '../types/portfolio'
import type { OrderResponseMessage } from '../types/order'
import type { AutoOrderResponseMessage } from '../types/autoOrder'
import type { OtocoResponseMessage } from '../types/otoco'
import type { TrailingStopResponseMessage } from '../types/trailingStop'
import type { AlertResponseMessage } from '../types/alert'

export async function getAllUsersAdmin(): Promise<UserDto[]> {
    const res = await instance.get<UserDto[]>('/admin/users')
    return res.data
}

export async function getUserAdmin(username: string): Promise<UserDto> {
    const res = await instance.get<UserDto>(`/admin/users/${username}`)
    return res.data
}

export async function getUserAccountAdmin(username: string): Promise<AccountResponse> {
    const res = await instance.get<AccountResponse>(`/admin/users/${username}/account`)
    return res.data
}

export async function getUserPortfolioAdmin(username: string): Promise<PortfolioResponse> {
    const res = await instance.get<PortfolioResponse>(`/admin/users/${username}/portfolio`)
    return res.data
}

export async function getUserOrdersAdmin(username: string): Promise<OrderResponseMessage[]> {
    const res = await instance.get<OrderResponseMessage[]>(`/admin/users/${username}/orders`)
    return res.data
}

export async function getUserAutoOrdersAdmin(username: string): Promise<AutoOrderResponseMessage[]> {
    const res = await instance.get<AutoOrderResponseMessage[]>(`/admin/users/${username}/auto-orders`)
    return res.data
}

export async function getUserOtocosAdmin(username: string): Promise<OtocoResponseMessage[]> {
    const res = await instance.get<OtocoResponseMessage[]>(`/admin/users/${username}/otocos`)
    return res.data
}

export async function getUserTrailingStopsAdmin(username: string): Promise<TrailingStopResponseMessage[]> {
    const res = await instance.get<TrailingStopResponseMessage[]>(`/admin/users/${username}/trailing-stops`)
    return res.data
}

export async function getUserAlertsAdmin(username: string): Promise<AlertResponseMessage[]> {
    const res = await instance.get<AlertResponseMessage[]>(`/admin/users/${username}/alerts`)
    return res.data
}
