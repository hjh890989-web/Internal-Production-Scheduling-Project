import { apiFetch } from './client'

export interface SettingGroupSummary {
  groupId: number
  displayName: string
  active: boolean
}

export interface SettingGroupCreatePayload {
  groupId: number
  displayName: string
}

export interface SettingGroupUpdatePayload {
  displayName: string
}

export const settingGroupApi = {
  list: () => apiFetch<SettingGroupSummary[]>('/api/v1/master/setting-groups'),
  create: (p: SettingGroupCreatePayload) =>
    apiFetch<SettingGroupSummary>('/api/v1/master/setting-groups', {
      method: 'POST', body: JSON.stringify(p),
    }),
  update: (groupId: number, p: SettingGroupUpdatePayload) =>
    apiFetch<SettingGroupSummary>(`/api/v1/master/setting-groups/${groupId}`, {
      method: 'PUT', body: JSON.stringify(p),
    }),
  toggleActive: (groupId: number) =>
    apiFetch<void>(`/api/v1/master/setting-groups/${groupId}`, {
      method: 'DELETE',
    }),
}
