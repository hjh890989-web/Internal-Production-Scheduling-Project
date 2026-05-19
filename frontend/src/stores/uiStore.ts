import { create } from 'zustand'

/**
 * UI prefs — 사이드바 collapsed / 다크모드 등.
 * persist 불필요 — 세션 단위로 충분.
 */
interface UIState {
  sidebarCollapsed: boolean
  toggleSidebar: () => void
}

export const useUIStore = create<UIState>((set) => ({
  sidebarCollapsed: false,
  toggleSidebar: () => set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
}))
