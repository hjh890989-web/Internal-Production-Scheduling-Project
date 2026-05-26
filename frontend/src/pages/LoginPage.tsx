import { useState } from 'react'
import { Button, Card, Form, Input, Typography, Alert } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import { login, HttpError } from '@/api/client'
import { useAuthStore } from '@/stores/authStore'

const { Title, Text } = Typography

interface LoginFormValues {
  employeeId: string
  pin: string
}

interface LocationState {
  from?: string
}

/**
 * Sprint 10 EP-AUTH 로그인 페이지 — 사번 8자리 + PIN 4자리 (NFR-SEC-007).
 *
 * <p>FCB 패턴 정합 — Card 중앙 정렬 + 로고 + 사번/PIN input + 로그인 버튼.
 * 성공 시 setSession + 원래 진입하려던 path (또는 /home) 으로 navigate.
 * 401 = 사번/PIN 불일치, 423 = 5회 실패 잠금 — Alert 표시.
 */
export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useAuthStore((s) => s.setSession)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const from = (location.state as LocationState)?.from ?? '/home'

  const handleSubmit = async (values: LoginFormValues) => {
    setError(null)
    setSubmitting(true)
    try {
      const res = await login(values.employeeId, values.pin)
      setSession(res.token, {
        employeeId: res.employeeId,
        role: res.role,
        expiresAt: res.expiresAt,
      })
      navigate(from, { replace: true })
    } catch (e) {
      if (e instanceof HttpError) {
        if (e.status === 423) {
          setError('계정이 잠겼습니다 — 5회 실패 후 10분 잠금. 잠시 후 다시 시도해 주세요.')
        } else if (e.status === 401) {
          setError('사번 또는 PIN 이 일치하지 않습니다.')
        } else if (e.status === 400) {
          setError('사번은 숫자 8자리, PIN 은 숫자 4자리로 입력해 주세요.')
        } else {
          setError(`로그인 실패 — HTTP ${e.status}`)
        }
      } else {
        setError('네트워크 오류 — 서버 응답 없음')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#f5f5f5',
        padding: 16,
      }}
    >
      <Card style={{ width: 400, padding: 16 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <img
            src="/logos/cheek-in-main-logo.svg"
            alt="Check In"
            style={{ height: 70, marginBottom: 8 }}
          />
          <div>
            <Text type="secondary" style={{ fontSize: 12 }}>
              송우산업 사내 업무 자동화 플랫폼
            </Text>
          </div>
          <Title level={4} style={{ marginTop: 16 }}>
            사내 공정 스케줄링 시스템
          </Title>
        </div>

        {error && (
          <Alert
            type="error"
            message={error}
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <Form<LoginFormValues>
          layout="vertical"
          onFinish={handleSubmit}
          autoComplete="off"
        >
          <Form.Item
            label="사번"
            name="employeeId"
            rules={[
              { required: true, message: '사번 입력 필수' },
              { pattern: /^[0-9]{8}$/, message: '사번 8자리 숫자' },
            ]}
          >
            <Input
              size="large"
              maxLength={8}
              inputMode="numeric"
              placeholder="12345678"
              autoFocus
            />
          </Form.Item>

          <Form.Item
            label="PIN"
            name="pin"
            rules={[
              { required: true, message: 'PIN 입력 필수' },
              { pattern: /^[0-9]{4}$/, message: 'PIN 4자리 숫자' },
            ]}
          >
            <Input.Password
              size="large"
              maxLength={4}
              inputMode="numeric"
              placeholder="••••"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              block
              size="large"
            >
              로그인
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
