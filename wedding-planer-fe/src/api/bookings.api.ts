import apiClient from './client'
import type { Booking } from '@/types/lead.types'

export const bookingsApi = {
  getByLead: (leadId: string) =>
    apiClient.get<Booking>(`/bookings/lead/${leadId}`),
  list: () => apiClient.get<Booking[]>('/bookings/my'),
  recordDeposit: (id: string, amount: number) =>
    apiClient.post<Booking>(`/bookings/${id}/deposit`, { amount }),
}
