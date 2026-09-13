import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BookDetailModal } from './BookDetailModal';
import type { BookTitleDto } from '../../types/catalog';
import * as catalogApi from '../../api/catalog';

describe('BookDetailModal', () => {
  const mockBook: BookTitleDto = {
    id: 10,
    title: 'Lập trình Java Căn Bản',
    author: 'Nguyễn Văn Nam',
    publisher: 'NXB Giáo Dục',
    isbn: '978-604-0-12345-6',
    publicationYear: 2024,
    categoryId: 1,
    categoryName: 'Công nghệ thông tin',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders nothing when book is null', () => {
    const { container } = render(<BookDetailModal book={null} onClose={vi.fn()} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders book metadata and loads physical copies', async () => {
    vi.spyOn(catalogApi, 'fetchBookCopies').mockResolvedValue({
      content: [
        {
          id: 101,
          bookTitleId: 10,
          bookTitle: 'Lập trình Java Căn Bản',
          libraryId: 1,
          libraryName: 'Thư viện Trung tâm',
          barcode: 'PXU-BC-100001',
          location: 'Kệ A1',
          status: 'AVAILABLE',
          version: 0,
          createdAt: '2026-09-01T00:00:00Z',
          updatedAt: '2026-09-01T00:00:00Z',
        },
        {
          id: 102,
          bookTitleId: 10,
          bookTitle: 'Lập trình Java Căn Bản',
          libraryId: 1,
          libraryName: 'Thư viện Trung tâm',
          barcode: 'PXU-BC-100002',
          location: 'Kệ A1',
          status: 'BORROWED',
          version: 1,
          createdAt: '2026-09-01T00:00:00Z',
          updatedAt: '2026-09-01T00:00:00Z',
        },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 100,
      number: 0,
      first: true,
      last: true,
      empty: false,
    });

    render(<BookDetailModal book={mockBook} onClose={vi.fn()} />);

    expect(screen.getByText('Lập trình Java Căn Bản')).toBeInTheDocument();
    expect(screen.getByText('Nguyễn Văn Nam')).toBeInTheDocument();
    expect(screen.getByText('NXB Giáo Dục')).toBeInTheDocument();
    expect(screen.getByText('978-604-0-12345-6')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('PXU-BC-100001')).toBeInTheDocument();
      expect(screen.getByText('PXU-BC-100002')).toBeInTheDocument();
      expect(screen.getByText('Có sẵn 1 / 2 cuốn')).toBeInTheDocument();
    });
  });

  it('calls onClose when close button is clicked', async () => {
    vi.spyOn(catalogApi, 'fetchBookCopies').mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 100,
      number: 0,
      first: true,
      last: true,
      empty: true,
    });

    const handleClose = vi.fn();
    render(<BookDetailModal book={mockBook} onClose={handleClose} />);

    const closeBtn = screen.getByRole('button', { name: 'Đóng chi tiết' });
    await userEvent.click(closeBtn);

    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});
