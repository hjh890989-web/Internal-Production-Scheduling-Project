import { apiFetch } from './client'

export type HolidayType = 'LEGAL' | 'COMPANY' | 'MAINTENANCE'

export interface HolidaySummary {
  holidayDate: string
  holidayName: string
  holidayType: HolidayType
  description: string | null
  createdBy: string
}

export interface HolidayPayload {
  holidayDate: string
  holidayName: string
  holidayType: HolidayType
  description?: string
}

export const holidayApi = {
  list: (year?: number) =>
    apiFetch<HolidaySummary[]>(
      year != null
        ? `/api/v1/master/holiday?year=${year}`
        : '/api/v1/master/holiday',
    ),
  create: (p: HolidayPayload) =>
    apiFetch<HolidaySummary>('/api/v1/master/holiday', {
      method: 'POST',
      body: JSON.stringify(p),
    }),
  delete: (date: string) =>
    apiFetch<void>(`/api/v1/master/holiday/${encodeURIComponent(date)}`, {
      method: 'DELETE',
    }),
}
