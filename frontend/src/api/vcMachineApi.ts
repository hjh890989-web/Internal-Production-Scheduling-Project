import { apiFetch } from './client'

export interface VcMachineSummary {
  machineId: string
  machineType: 'LP' | 'IC'
  totalSlots: number
  dayRotations: number
  nightRotations: number
  active: boolean
  updatedAt: string
  updatedBy: string
}

export interface VcMachineCreatePayload {
  machineId: string
  machineType: 'LP' | 'IC'
  totalSlots: number
  dayRotations: number
  nightRotations: number
  active: boolean
}

export interface VcMachineUpdatePayload {
  totalSlots: number
  dayRotations: number
  nightRotations: number
  active: boolean
}

export const vcMachineApi = {
  list: () =>
    apiFetch<VcMachineSummary[]>('/api/v1/master/vc-machines'),

  create: (p: VcMachineCreatePayload) =>
    apiFetch<VcMachineSummary>('/api/v1/master/vc-machines', {
      method: 'POST', body: JSON.stringify(p),
    }),

  update: (machineId: string, p: VcMachineUpdatePayload) =>
    apiFetch<VcMachineSummary>(`/api/v1/master/vc-machines/${encodeURIComponent(machineId)}`, {
      method: 'PUT', body: JSON.stringify(p),
    }),

  delete: (machineId: string) =>
    apiFetch<void>(`/api/v1/master/vc-machines/${encodeURIComponent(machineId)}`, {
      method: 'DELETE',
    }),
}
