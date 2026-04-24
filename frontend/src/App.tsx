import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from 'next-themes'
import { Toaster } from '@/components/ui/sonner'
import LoginPage from '@/pages/LoginPage'
import AuthCallbackPage from '@/pages/AuthCallbackPage'
import EmailVerifyPage from '@/pages/EmailVerifyPage'
import AllClubsPage from '@/pages/AllClubsPage'
import ClubsPage from '@/pages/ClubsPage'
import MujePage from '@/pages/MujePage'
import BookRequestPage from '@/pages/BookRequestPage'
import BookReportPage from '@/pages/BookReportPage'
import OwnedBooksPage from '@/pages/OwnedBooksPage'
import AdminBookRequestsPage from '@/pages/AdminBookRequestsPage'
import AdminBookReportsPage from '@/pages/AdminBookReportsPage'
import AdminArrivalsPage from '@/pages/AdminArrivalsPage'
import AdminClubMembersPage from '@/pages/AdminClubMembersPage'
import AdminBookExemptionsPage from '@/pages/AdminBookExemptionsPage'
import AdminExemptBooksPage from '@/pages/AdminExemptBooksPage'
import DeveloperWhitelistPage from '@/pages/DeveloperWhitelistPage'
import ProtectedRoute from '@/components/ProtectedRoute'

function App() {
  return (
    <ThemeProvider attribute="class" defaultTheme="dark" disableTransitionOnChange>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
          <Route path="/auth/email-verify" element={<EmailVerifyPage />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/home" element={<AllClubsPage />} />
            <Route path="/clubs" element={<ClubsPage />} />
            <Route path="/muje" element={<MujePage />} />
            <Route path="/muje/book-requests" element={<BookRequestPage />} />
            <Route path="/muje/books" element={<OwnedBooksPage />} />
            <Route path="/muje/book-reports" element={<BookReportPage />} />
            <Route path="/muje/admin/book-requests" element={<AdminBookRequestsPage />} />
            <Route path="/muje/admin/book-reports" element={<AdminBookReportsPage />} />
            <Route path="/muje/admin/arrivals" element={<AdminArrivalsPage />} />
            <Route path="/muje/admin/members" element={<AdminClubMembersPage />} />
            <Route path="/muje/admin/book-exemptions" element={<AdminBookExemptionsPage />} />
            <Route path="/muje/admin/exempt-books" element={<AdminExemptBooksPage />} />
            <Route path="/developer/whitelist" element={<DeveloperWhitelistPage />} />
          </Route>
        </Routes>
        <Toaster />
      </BrowserRouter>
    </ThemeProvider>
  )
}

export default App
