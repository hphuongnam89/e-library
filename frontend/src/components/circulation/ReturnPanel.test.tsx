import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReturnPanel } from './ReturnPanel';
import * as circulationApi from '../../api/circulation';
import type { BorrowDto } from '../../types/circulation';

describe('ReturnPanel component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('searches for borrow record by barcode and displays info', async () => {
    const mockBorrow = {
      id: 50,
      barcode: 'PXU-BC-001',
      bookTitle: 'Thiết kế Vi kiến trúc Spring',
      libraryName: 'Thư viện Trung tâm',
      userFullName: 'Lê Văn C',
      studentCode: 'PXU003',
      userEmail: 'c@phuxuan.edu.vn',
      borrowedAt: '2026-03-01T10:00:00Z',
      dueAt: '2026-03-10T10:00:00Z',
      returnedAt: null,
      status: 'BORROWED',
      overdue: true,
      fineAmount: 25000,
      finePaidAt: null,
    } as unknown as BorrowDto;

    vi.spyOn(circulationApi, 'fetchBorrows').mockResolvedValueOnce({
      content: [mockBorrow],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
      empty: false,
    });

    render(<ReturnPanel />);

    const input = screen.getByPlaceholderText(/Quét hoặc nhập mã Barcode/i);
    fireEvent.change(input, { target: { value: 'PXU-BC-001' } });

    const searchBtn = screen.getByRole('button', { name: /tra cứu/i });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(screen.getByText('Thiết kế Vi kiến trúc Spring')).toBeInTheDocument();
      expect(screen.getByText(/Lê Văn C/i)).toBeInTheDocument();
      expect(screen.getByText('25.000 đ')).toBeInTheDocument();
      expect(screen.getByText('⚠ Chưa thanh toán tiền phạt')).toBeInTheDocument();
    });
  });

  it('handles returning a book and paying fine successfully', async () => {
    const mockBorrow = {
      id: 50,
      barcode: 'PXU-BC-001',
      bookTitle: 'Thiết kế Vi kiến trúc Spring',
      libraryName: 'Thư viện Trung tâm',
      userFullName: 'Lê Văn C',
      studentCode: 'PXU003',
      userEmail: 'c@phuxuan.edu.vn',
      borrowedAt: '2026-03-01T10:00:00Z',
      dueAt: '2026-03-10T10:00:00Z',
      returnedAt: null,
      status: 'BORROWED',
      overdue: true,
      fineAmount: 25000,
      finePaidAt: null,
    } as unknown as BorrowDto;

    vi.spyOn(circulationApi, 'fetchBorrows').mockResolvedValueOnce({
      content: [mockBorrow],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
      empty: false,
    });

    const returnSpy = vi.spyOn(circulationApi, 'returnBorrow').mockResolvedValueOnce({
      ...mockBorrow,
      status: 'RETURNED',
      returnedAt: '2026-03-13T10:00:00Z',
    });

    const payFineSpy = vi.spyOn(circulationApi, 'payFine').mockResolvedValueOnce({
      ...mockBorrow,
      finePaidAt: '2026-03-13T10:05:00Z',
    });

    render(<ReturnPanel />);

    const input = screen.getByPlaceholderText(/Quét hoặc nhập mã Barcode/i);
    fireEvent.change(input, { target: { value: 'PXU-BC-001' } });
    fireEvent.click(screen.getByRole('button', { name: /tra cứu/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /xác nhận trả sách ngay/i })).toBeInTheDocument();
    });

    // Pay fine
    const payBtn = screen.getByRole('button', { name: /xác nhận thu tiền phạt/i });
    fireEvent.click(payBtn);
    await waitFor(() => {
      expect(payFineSpy).toHaveBeenCalledWith(50);
      expect(screen.getByText(/Đã ghi nhận thu tiền phạt thành công!/i)).toBeInTheDocument();
    });

    // Return book
    const returnBtn = screen.getByRole('button', { name: /xác nhận trả sách ngay/i });
    fireEvent.click(returnBtn);
    await waitFor(() => {
      expect(returnSpy).toHaveBeenCalledWith(50);
      expect(screen.getByText(/Trả sách thành công!/i)).toBeInTheDocument();
    });
  });

  it('shows error when barcode is not found', async () => {
    vi.spyOn(circulationApi, 'fetchBorrows').mockResolvedValueOnce({
      content: [],
      totalPages: 0,
      totalElements: 0,
      number: 0,
      size: 100,
      first: true,
      last: true,
      empty: true,
    });

    render(<ReturnPanel />);

    const input = screen.getByPlaceholderText(/Quét hoặc nhập mã Barcode/i);
    fireEvent.change(input, { target: { value: 'NON-EXISTENT' } });
    fireEvent.click(screen.getByRole('button', { name: /tra cứu/i }));

    await waitFor(() => {
      expect(
        screen.getByText(/Không tìm thấy khoản mượn nào đang hoạt động cho mã barcode "NON-EXISTENT"/i)
      ).toBeInTheDocument();
    });
  });
});
