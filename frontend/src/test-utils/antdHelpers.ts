/**
 * antdHelpers.ts — AntD v5 jsdom 호환 테스트 헬퍼
 *
 * AntD v5 Modal/Popconfirm/Drawer 는 document.body portal 에 렌더됨.
 * jsdom 환경에서는 getByRole 등이 scope 를 놓칠 수 있어
 * document.body 전역 또는 popup container 내 query 를 사용한다.
 *
 * Sprint 24 ST-FB-2 (EP-OPS-FEEDBACK) — skip → it 활성화 지원
 */

import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { fireEvent } from '@testing-library/react'

/**
 * Popconfirm 트리거를 클릭하고, 팝업 내 OK 버튼을 클릭한다.
 *
 * @param triggerLabel  - 트리거 버튼의 accessible name (screen.getByRole 검색용)
 * @param okLabel       - Popconfirm okText (팝업 내 확인 버튼 텍스트)
 *
 * AntD v5 Popconfirm 은 .ant-popover 혹은 .ant-popconfirm 클래스 popup 을
 * document.body 에 portal 렌더한다.
 * 트리거 클릭 후 popup 내부에서 okLabel 버튼을 찾는다.
 */
export const openPopconfirmAndConfirm = async (
  triggerLabel: string,
  okLabel: string,
): Promise<void> => {
  // 트리거 버튼 클릭 (첫 번째 매칭)
  const triggerButtons = screen.getAllByRole('button', { name: triggerLabel })
  fireEvent.click(triggerButtons[0]!)

  // AntD v5 popup 이 document.body 에 마운트될 때까지 대기
  await waitFor(() => {
    // ant-popover 또는 ant-popconfirm popup container
    const popup =
      document.querySelector('.ant-popover') ??
      document.querySelector('.ant-popconfirm') ??
      document.querySelector('[role="tooltip"]')

    if (!popup) throw new Error('Popconfirm popup not found in document.body')

    const okBtn = within(popup as HTMLElement).getByRole('button', { name: okLabel })
    if (!okBtn) throw new Error(`Popconfirm OK button "${okLabel}" not found in popup`)
  })

  // popup 내 OK 버튼 클릭
  const popup =
    document.querySelector('.ant-popover') ??
    document.querySelector('.ant-popconfirm') ??
    document.querySelector('[role="tooltip"]')!

  const okBtn = within(popup as HTMLElement).getByRole('button', { name: okLabel })
  fireEvent.click(okBtn)
}

/**
 * Modal/Drawer 트리거 버튼을 클릭한 뒤 data-testid 로 input 을 찾아 반환한다.
 *
 * @param triggerButtonName - 트리거 버튼의 accessible name
 * @param inputTestId       - 찾을 input 의 data-testid
 * @returns HTMLElement (input)
 *
 * AntD v5 Modal/Drawer 는 document.body portal 에 렌더됨.
 * within(document.body) 로 scope 하여 testid 검색.
 */
export const openModalAndFindByTestId = async (
  triggerButtonName: string,
  inputTestId: string,
): Promise<HTMLElement> => {
  const trigger = screen.getByRole('button', { name: triggerButtonName })
  fireEvent.click(trigger)

  let inputEl: HTMLElement | null = null
  await waitFor(() => {
    inputEl = within(document.body).getByTestId(inputTestId)
    if (!inputEl) throw new Error(`testid "${inputTestId}" not found in document.body`)
  })

  return inputEl!
}

/**
 * userEvent instance (setup) — 복잡한 입력 시나리오용.
 * 호출 측에서 `const user = setupUserEvent()` 로 사용.
 */
export const setupUserEvent = () => userEvent.setup()
