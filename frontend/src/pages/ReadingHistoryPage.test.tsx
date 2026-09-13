import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReadingHistoryPage } from './ReadingHistoryPage';
import * as useAuthModule from '../hooks/useAuth';
import * as readingApi from '../api/reading';
import type { ReadingHistoryItem } from '../types/reading';
import type { Page } from '../types/catalog';

describe('ReadingHistoryPage', () => {
  const mockUser: useAuthModule.UserDto = {
    id: 1,
    email: 'sinhvien@pxu.edu.vn',
    fullName: 'Nguyễn Văn A',
    studentCode: 'PXU001',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  const mockHistoryItems: ReadingHistoryItem[] = [
    {
      documentId: 10,
      title: 'Giáo trình Kiến trúc Máy tính',
      publisher: 'NXB Giáo Dục',
      activeSeconds: 1500, // 25p 00s
      sessionCount: 4,
      lastActiveAt: '2026-09-10T14:30:00Z',
    },
    {
      documentId: 20,
      title: 'Nhập môn Trí tuệ Nhân tạo',
      publisher: 'NXB Khoa học',
      activeSeconds: 4200, // 1g 10p 00s
      sessionCount: 7,
      lastActiveAt: '2026-09-12T09:15:00Z',
    },
  ];

  const mockPageResponse: Page<ReadingHistoryItem> = {
    content: mockHistoryItems,
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
    first: true,
    last: true,
    empty: false,
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders login prompt when unauthenticated', () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: null,
      status: 'unauthenticated',
      isAuthenticated: false,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    render(<ReadingHistoryPage />);
    expect(screen.getByText('Yêu cầu đăng nhập')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /đăng nhập google/i })).toBeInTheDocument();
  });

  it('renders reading history table with correct items and formatted times', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(readingApi, 'fetchMyReadingHistory').mockResolvedValueOnce(mockPageResponse);

    render(<ReadingHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Lịch sử Đọc Tài liệu Số')).toBeInTheDocument();
      expect(screen.getByText('Giáo trình Kiến trúc Máy tính')).toBeInTheDocument();
      expect(screen.getByText('Nhập môn Trí tuệ Nhân tạo')).toBeInTheDocument();
      expect(screen.getByText('⏱️ 25p 00s')).toBeInTheDocument();
      expect(screen.getByText('⏱️ 1g 10p 00s')).toBeInTheDocument();
      expect(screen.getByText('4 phiên')).toBeInTheDocument();
      expect(screen.getByText('7 phiên')).toBeInTheDocument();
    });
  });

  it('renders empty state when user has no reading history', async () => {
    vi.spyOn(useAuthModule, 'useAuth').mockReturnValue({
      user: mockUser,
      status: 'authenticated',
      isAuthenticated: true,
      isLoading: false,
      login: vi.fn(),
      logout: vi.fn(),
      refetch: vi.fn(),
    });

    vi.spyOn(readingApi, 'fetchMyReadingHistory').mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: true,
    });

    render(<ReadingHistoryPage />);

    await waitFor(() => {
      expect(screen.getByText('Chưa có lịch sử đọc')).toBeInTheDocument();
    });
  });
});
