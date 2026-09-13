import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BookTitleModal } from './BookTitleModal';
import * as catalogApi from '../../api/catalog';

describe('BookTitleModal', () => {
  const mockCategories = [
    { id: 1, parentId: null, parentName: null, name: 'Công nghệ thông tin' },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <BookTitleModal
        isOpen={false}
        bookToEdit={null}
        categories={mockCategories}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('submits create book title form', async () => {
    const handleSuccess = vi.fn();
    const handleClose = vi.fn();

    const createSpy = vi.spyOn(catalogApi, 'createBookTitle').mockResolvedValue({
      id: 99,
      title: 'Học Máy Nâng Cao',
      author: 'Nguyễn Văn X',
      publisher: 'NXB Khoa Học',
      isbn: '978-604-0000',
      publicationYear: 2025,
      categoryId: 1,
      categoryName: 'Công nghệ thông tin',
      createdAt: '2026-09-01T00:00:00Z',
      updatedAt: '2026-09-01T00:00:00Z',
    });

    render(
      <BookTitleModal
        isOpen={true}
        bookToEdit={null}
        categories={mockCategories}
        onClose={handleClose}
        onSuccess={handleSuccess}
      />
    );

    expect(screen.getByText('Thêm Nhan đề Sách Mới')).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText(/Nhan đề sách \*/), 'Học Máy Nâng Cao');
    await userEvent.type(screen.getByLabelText(/Tác giả/), 'Nguyễn Văn X');
    await userEvent.click(screen.getByRole('button', { name: 'Tạo nhan đề mới' }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          title: 'Học Máy Nâng Cao',
          author: 'Nguyễn Văn X',
        })
      );
      expect(handleSuccess).toHaveBeenCalledTimes(1);
      expect(handleClose).toHaveBeenCalledTimes(1);
    });
  });
});
