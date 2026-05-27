import { Button, Result } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

/**
 * Sprint 11 EP-RBAC ForbiddenPage (TK-RBAC-3-4, NFR-SEC-003).
 *
 * <p>{@link RoleGuard} 가 권한 미충족 시 redirect 대상. 사용자에게 현재 role + IT 운영팀 안내.
 */
export default function ForbiddenPage() {
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 48 }}>
      <Result
        status="403"
        title="403 — 접근 권한 없음"
        subTitle={
          user
            ? `사번 ${user.employeeId} (${user.role}) 는 이 페이지에 접근할 권한이 없습니다. IT 운영팀에 문의하세요.`
            : '권한이 없습니다. 로그인 후 다시 시도하세요.'
        }
        extra={
          <Button type="primary" onClick={() => navigate('/home', { replace: true })}>
            홈으로
          </Button>
        }
      />
    </div>
  )
}
