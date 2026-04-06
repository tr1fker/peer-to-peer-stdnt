import { Link, NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { AuthCallbackPage } from './pages/AuthCallbackPage'
import { OauthRegisterPage } from './pages/OauthRegisterPage'
import { VerifyEmailPage } from './pages/VerifyEmailPage'
import { HomePage } from './pages/HomePage'
import { LoanRequestsPage } from './pages/LoanRequestsPage'
import { LoanRequestDetailPage } from './pages/LoanRequestDetailPage'
import { CreateLoanPage } from './pages/CreateLoanPage'
import { ProfilePage } from './pages/ProfilePage'
import { AcademicPage } from './pages/AcademicPage'
import { AdminAcademicPage } from './pages/AdminAcademicPage'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { AdminAnalyticsPage } from './pages/AdminAnalyticsPage'
import { MyLoansPage } from './pages/MyLoansPage'

function Private({ children }: { children: React.ReactElement }) {
  const token = useAuthStore((s) => s.accessToken)
  if (!token) return <Navigate to="/login" replace />
  return children
}

function AdminOnly({ children }: { children: React.ReactElement }) {
  const roles = useAuthStore((s) => s.roles)
  if (!roles.includes('ROLE_ADMIN')) return <Navigate to="/" replace />
  return children
}

function LegacyOauthRegisterRedirect() {
  const { search } = useLocation()
  return <Navigate to={`/auth/oauth-register${search}`} replace />
}

function navClass({ isActive }: { isActive: boolean }) {
  return isActive ? 'nav-link active' : 'nav-link'
}

export default function App() {
  const token = useAuthStore((s) => s.accessToken)
  const roles = useAuthStore((s) => s.roles)
  const logout = useAuthStore((s) => s.logout)

  return (
    <div className="layout">
      <header className="app-nav">
        <nav className="app-nav-inner">
          <Link to="/" className="brand">
            Peer<span>Lend</span>
          </Link>
          <NavLink to="/" className={navClass} end>
            Главная
          </NavLink>
          <NavLink to="/loans" className={navClass} end>
            Заявки
          </NavLink>
          {token && (
            <NavLink to="/loans/favorites" className={navClass}>
              Избранное
            </NavLink>
          )}
          {token && roles.includes('ROLE_BORROWER') && (
            <NavLink to="/loans/new" className={navClass}>
              Новая заявка
            </NavLink>
          )}
          {token && (
            <NavLink to="/my-loans" className={navClass}>
              Мои займы
            </NavLink>
          )}
          {token && (
            <NavLink to="/profile" className={navClass}>
              Профиль
            </NavLink>
          )}
          {token && (
            <NavLink to="/academic" className={navClass}>
              Успеваемость
            </NavLink>
          )}
          {token && roles.includes('ROLE_ADMIN') && (
            <NavLink to="/admin/academic" className={navClass}>
              Админ: успеваемость
            </NavLink>
          )}
          {token && roles.includes('ROLE_ADMIN') && (
            <NavLink to="/admin/users" className={navClass}>
              Админ: пользователи
            </NavLink>
          )}
          {token && roles.includes('ROLE_ADMIN') && (
            <NavLink to="/admin/analytics" className={navClass}>
              Админ: аналитика
            </NavLink>
          )}
          <span className="nav-spacer" />
          <div className="nav-actions">
            {!token && (
              <NavLink to="/login" className={navClass}>
                Вход
              </NavLink>
            )}
            {!token && (
              <NavLink to="/register" className="btn primary">
                Регистрация
              </NavLink>
            )}
            {token && (
              <button type="button" onClick={logout}>
                Выход
              </button>
            )}
          </div>
        </nav>
      </header>

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/auth/callback" element={<AuthCallbackPage />} />
        <Route path="/auth/oauth-register" element={<OauthRegisterPage />} />
        <Route path="/auth/github-register" element={<LegacyOauthRegisterRedirect />} />
        <Route path="/auth/verify-email" element={<VerifyEmailPage />} />
        <Route path="/loans" element={<LoanRequestsPage />} />
        <Route
          path="/loans/favorites"
          element={
            <Private>
              <LoanRequestsPage />
            </Private>
          }
        />
        <Route path="/requests/:id" element={<LoanRequestDetailPage />} />
        <Route
          path="/loans/new"
          element={
            <Private>
              <CreateLoanPage />
            </Private>
          }
        />
        <Route
          path="/profile"
          element={
            <Private>
              <ProfilePage />
            </Private>
          }
        />
        <Route
          path="/academic"
          element={
            <Private>
              <AcademicPage />
            </Private>
          }
        />
        <Route
          path="/admin/academic"
          element={
            <Private>
              <AdminOnly>
                <AdminAcademicPage />
              </AdminOnly>
            </Private>
          }
        />
        <Route
          path="/admin/users"
          element={
            <Private>
              <AdminOnly>
                <AdminUsersPage />
              </AdminOnly>
            </Private>
          }
        />
        <Route
          path="/admin/analytics"
          element={
            <Private>
              <AdminOnly>
                <AdminAnalyticsPage />
              </AdminOnly>
            </Private>
          }
        />
        <Route
          path="/my-loans"
          element={
            <Private>
              <MyLoansPage />
            </Private>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}
