import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BookCopyModal } from './BookCopyModal';
import * as catalogApi from '../../api/catalog';
import * as orgApi from '../../api/organization';
import type { BookCopyDto, BookTitleDto } from '../../types/catalog';
import type { LibraryDto } from '../../types/organization';

describe('BookCopyModal component', () => {
  const mockBook = {
    id: 10,
    isbn: '978-0132350884',
    title: 'Clean Code: A Handbook of Agile Software Craftsmanship',
    author: 'Robert C. Martin',
    publisher: 'Prentice Hall',
    publicationYear: 2008,
    categoryName: 'Công nghệ thông tin',
    totalCopies: 5,
    availableCopies: 3,
  } as unknown as BookTitleDto;

  const mockLibraries = [
    {
      id: 1,
      campusId: 1,
      name: 'Thư viện Trung tâm',
      code: 'LIB-MAIN',
      address: 'Cơ sở 1',
    },
  ];

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(orgApi, 'fetchLibraries').mockResolvedValue({
      content: mockLibraries as unknown as LibraryDto[],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
      empty: false,
    });
  });

  it('renders create copy modal when open', async () => {
    render(
      <BookCopyModal
        isOpen={true}
        book={mockBook}
        copyToEdit={null}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );

    expect(screen.getByText('Thêm Bản sao Vật lý Mới')).toBeInTheDocument();
    expect(screen.getByText(/Sách: Clean Code/i)).toBeInTheDocument();
  });

  it('submits create book copy successfully', async () => {
    const createSpy = vi.spyOn(catalogApi, 'createBookCopy').mockResolvedValueOnce({
      id: 99,
      bookTitleId: 10,
      libraryId: 1,
      barcode: 'PXU-BC-099',
      location: 'Kệ A1',
      status: 'AVAILABLE',
      libraryName: 'Thư viện Trung tâm',
    } as unknown as BookCopyDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <BookCopyModal
        isOpen={true}
        book={mockBook}
        copyToEdit={null}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    await waitFor(() => {
      expect(orgApi.fetchLibraries).toHaveBeenCalled();
    });

    const barcodeInput = screen.getByLabelText(/Mã vạch Barcode/i);
    fireEvent.change(barcodeInput, { target: { value: 'PXU-BC-099' } });

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          bookTitleId: 10,
          barcode: 'PXU-BC-099',
        })
      );
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('renders edit mode with existing copy data and updates copy', async () => {
    const existingCopy = {
      id: 55,
      bookTitleId: 10,
      libraryId: 1,
      barcode: 'PXU-BC-055',
      location: 'Kệ B2',
      status: 'AVAILABLE' as const,
      libraryName: 'Thư viện Trung tâm',
    } as unknown as BookCopyDto;

    const updateSpy = vi.spyOn(catalogApi, 'updateBookCopy').mockResolvedValueOnce({
      ...existingCopy,
      location: 'Kệ B3',
    } as unknown as BookCopyDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <BookCopyModal
        isOpen={true}
        book={mockBook}
        copyToEdit={existingCopy}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByText('Cập nhật Bản sao Sách')).toBeInTheDocument();

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(
        55,
        expect.objectContaining({
          libraryId: 1,
        })
      );
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('returns null when isOpen is false', () => {
    const { container } = render(
      <BookCopyModal
        isOpen={false}
        book={mockBook}
        copyToEdit={null}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );

    expect(container.firstChild).toBeNull();
  });
});
