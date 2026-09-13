import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BorrowTable } from './BorrowTable';
import * as circulationApi from '../../api/circulation';
import type { BorrowDto } from '../../types/circulation';

describe('BorrowTable Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders borrow records correctly after fetching data', async () => {
    const mockBorrows = [
      {
        id: 1,
        barcode: 'BC-999',
        bookTitle: 'Clean Code',
        userFullName: 'Nguyễn Văn A',
        studentCode: 'PXU001',
        borrowedAt: '2026-03-01T10:00:00Z',
        dueAt: '2026-03-15T10:00:00Z',
        returnedAt: null,
        status: 'BORROWED',
        overdue: false,
        fineAmount: 0,
        libraryName: 'Thư viện Trung tâm',
      },
    ] as unknown as BorrowDto[];

    vi.spyOn(circulationApi, 'fetchBorrows').mockResolvedValueOnce({
      content: mockBorrows,
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: false,
    });

    render(<BorrowTable />);

    await waitFor(() => {
      expect(screen.getByText('BC-999')).toBeInTheDocument();
      expect(screen.getByText('Clean Code')).toBeInTheDocument();
      expect(screen.getByText('Nguyễn Văn A')).toBeInTheDocument();
    });
  });

  it('renders empty state when no borrow records exist', async () => {
    vi.spyOn(circulationApi, 'fetchBorrows').mockResolvedValueOnce({
      content: [],
      totalPages: 0,
      totalElements: 0,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: true,
    });

    render(<BorrowTable />);

    await waitFor(() => {
      expect(
        screen.getByText(/Không có dữ liệu mượn trả nào phù hợp/i)
      ).toBeInTheDocument();
    });
  });

  it('handles status filter change and overdue toggle', async () => {
    const fetchSpy = vi.spyOn(circulationApi, 'fetchBorrows').mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      number: 0,
      size: 10,
      first: true,
      last: true,
      empty: true,
    });

    render(<BorrowTable />);

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'BORROWED' } });

    const checkbox = screen.getByRole('checkbox');
    fireEvent.click(checkbox);

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          status: 'BORROWED',
          overdue: true,
        })
      );
    });
  });
});
