import { useEffect, useState } from 'react'
import {
  Badge,
  Button,
  Calendar,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Typography,
  message,
} from 'antd'
import type { CalendarProps } from 'antd'
import dayjs, { Dayjs } from 'dayjs'
import 'dayjs/locale/ko'
import timezone from 'dayjs/plugin/timezone'
import utc from 'dayjs/plugin/utc'
import { holidayApi, type HolidaySummary, type HolidayPayload, type HolidayType } from '@/api/holidayApi'
import { HttpError } from '@/api/client'

dayjs.extend(utc)
dayjs.extend(timezone)
dayjs.locale('ko')

const { Title, Text } = Typography

const HOLIDAY_TYPE_OPTIONS: { label: string; value: HolidayType }[] = [
  { label: '법정공휴일', value: 'LEGAL' },
  { label: '사내 휴일', value: 'COMPANY' },
  { label: '정비일', value: 'MAINTENANCE' },
]

const HOLIDAY_TYPE_LABEL: Record<HolidayType, string> = {
  LEGAL: '법정공휴일',
  COMPANY: '사내 휴일',
  MAINTENANCE: '정비일',
}

interface AddFormValues {
  holidayName: string
  holidayType: HolidayType
  description?: string
}

/**
 * Sprint 21 ST-CRUD-5 — Holiday 마스터 관리 (Calendar UI + CRUD).
 *
 * <p>IT_OPS 만 추가·삭제 가능. GET 은 모든 인증 role.
 * endpoint: /api/v1/master/holiday (singular, BR-X04 KST 정합).
 */
export default function HolidayAdminPage() {
  const currentYear = dayjs().tz('Asia/Seoul').year()

  const [holidays, setHolidays] = useState<HolidaySummary[]>([])
  const [loading, setLoading] = useState(false)
  const [year, setYear] = useState<number>(currentYear)
  const [calendarValue, setCalendarValue] = useState<Dayjs>(
    dayjs().tz('Asia/Seoul'),
  )

  // Add modal state
  const [addModalOpen, setAddModalOpen] = useState(false)
  const [selectedDate, setSelectedDate] = useState<string | null>(null)
  const [addLoading, setAddLoading] = useState(false)
  const [addForm] = Form.useForm<AddFormValues>()

  const holidayMap = new Map<string, HolidaySummary>(
    holidays.map((h) => [h.holidayDate, h]),
  )

  const reload = async (targetYear: number) => {
    setLoading(true)
    try {
      setHolidays(await holidayApi.list(targetYear))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void reload(year)
  }, [year])

  const handleYearChange = (val: number) => {
    setYear(val)
    setCalendarValue(calendarValue.year(val))
  }

  const handleCellClick = (date: Dayjs) => {
    const dateStr = date.format('YYYY-MM-DD')
    setSelectedDate(dateStr)
    if (!holidayMap.has(dateStr)) {
      addForm.resetFields()
      setAddModalOpen(true)
    }
    // 기존 휴일은 Popconfirm 으로 처리 (cellRender 내부)
  }

  const handleAddSubmit = async (values: AddFormValues) => {
    if (!selectedDate) return
    const payload: HolidayPayload = {
      holidayDate: selectedDate,
      holidayName: values.holidayName,
      holidayType: values.holidayType,
      description: values.description,
    }
    setAddLoading(true)
    try {
      await holidayApi.create(payload)
      message.success(`추가 완료 — ${selectedDate} ${values.holidayName}`)
      setAddModalOpen(false)
      await reload(year)
    } catch (e) {
      if (e instanceof HttpError && e.status === 409) {
        message.error('이미 등록된 휴일입니다.')
      } else if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else {
        message.error('추가 실패')
      }
    } finally {
      setAddLoading(false)
    }
  }

  const handleDelete = async (dateStr: string) => {
    try {
      await holidayApi.delete(dateStr)
      message.success(`삭제 완료 — ${dateStr}`)
      await reload(year)
    } catch (e) {
      if (e instanceof HttpError && e.status === 403) {
        message.error('IT_OPS 권한 필요')
      } else {
        message.error('삭제 실패')
      }
    }
  }

  const cellRender: CalendarProps<Dayjs>['cellRender'] = (date, info) => {
    if (info.type !== 'date') return info.originNode
    const dateStr = date.format('YYYY-MM-DD')
    const holiday = holidayMap.get(dateStr)
    if (!holiday) return info.originNode

    return (
      <Popconfirm
        title={
          <span>
            <Text strong>{holiday.holidayName}</Text>
            <br />
            <Text type="secondary">{HOLIDAY_TYPE_LABEL[holiday.holidayType]}</Text>
            {holiday.description && (
              <>
                <br />
                <Text type="secondary">{holiday.description}</Text>
              </>
            )}
          </span>
        }
        description="이 휴일을 삭제하시겠습니까?"
        okText="삭제"
        okType="danger"
        cancelText="취소"
        onConfirm={() => handleDelete(dateStr)}
      >
        <div
          role="button"
          tabIndex={0}
          aria-label={`${dateStr} 휴일: ${holiday.holidayName} — 삭제하려면 클릭`}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') e.currentTarget.click()
          }}
          className="cursor-pointer"
        >
          <Badge status="error" text={holiday.holidayName} />
        </div>
      </Popconfirm>
    )
  }

  const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - 1 + i).map(
    (y) => ({ label: `${y}년`, value: y }),
  )

  return (
    <div className="p-4">
      <Title level={3}>휴일 관리 (Holiday Master)</Title>
      <Typography.Paragraph type="secondary">
        IT_OPS 만 추가·삭제 가능. 날짜 셀 클릭 — 빈 날짜는 추가, 휴일은 삭제 안내.
      </Typography.Paragraph>

      <Space className="mb-4">
        <Select
          aria-label="연도 선택"
          value={year}
          options={yearOptions}
          onChange={handleYearChange}
          style={{ width: 120 }}
        />
        <Button onClick={() => void reload(year)} loading={loading}>
          새로고침
        </Button>
        <Text type="secondary">{holidays.length}건 등록됨</Text>
      </Space>

      <Calendar
        value={calendarValue}
        onSelect={(date, { source }) => {
          if (source === 'date') handleCellClick(date)
        }}
        onPanelChange={(val) => {
          setCalendarValue(val)
          if (val.year() !== year) setYear(val.year())
        }}
        cellRender={cellRender}
        style={{ border: '1px solid #f0f0f0', borderRadius: 8 }}
      />

      <Modal
        title={`휴일 추가 — ${selectedDate ?? ''}`}
        open={addModalOpen}
        onCancel={() => setAddModalOpen(false)}
        footer={null}
        destroyOnHidden
      >
        <Form<AddFormValues>
          form={addForm}
          layout="vertical"
          onFinish={handleAddSubmit}
          initialValues={{ holidayType: 'LEGAL' }}
        >
          <Form.Item label="휴일명" name="holidayName" rules={[{ required: true, message: '휴일명을 입력하세요' }]}>
            <Input maxLength={100} data-testid="input-holiday-name" />
          </Form.Item>
          <Form.Item label="유형" name="holidayType" rules={[{ required: true }]}>
            <Select options={HOLIDAY_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item label="비고 (선택)" name="description">
            <Input.TextArea rows={2} maxLength={200} />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={addLoading}
              data-testid="btn-add-submit"
            >
              추가
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
