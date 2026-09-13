import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CategoryModal } from './CategoryModal';
import * as catalogApi from '../../api/catalog';
import type { CategoryDto } from '../../types/catalog';

describe('CategoryModal component', () => {
  const mockCategories = [
    { id: 1, name: 'Công nghệ thông tin', parentId: null, parentName: null, active: true },
    { id: 2, name: 'Khoa học máy tính', parentId: 1, parentName: 'Công nghệ thông tin', active: true },
  ] as unknown as CategoryDto[];

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders create category modal and submits new category', async () => {
    const createSpy = vi.spyOn(catalogApi, 'createCategory').mockResolvedValueOnce({
      id: 3,
      name: 'Trí tuệ nhân tạo',
      parentId: 1,
      parentName: 'Công nghệ thông tin',
      active: true,
    } as unknown as CategoryDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <CategoryModal
        isOpen={true}
        categoryToEdit={null}
        categories={mockCategories}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByText('Thêm Danh mục Mới')).toBeInTheDocument();

    const nameInput = screen.getByLabelText(/Tên danh mục/i);
    fireEvent.change(nameInput, { target: { value: 'Trí tuệ nhân tạo' } });

    const parentSelect = screen.getByLabelText(/Danh mục cha/i);
    fireEvent.change(parentSelect, { target: { value: '1' } });

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({
        name: 'Trí tuệ nhân tạo',
        parentId: 1,
      });
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('renders edit modal and updates existing category', async () => {
    const updateSpy = vi.spyOn(catalogApi, 'updateCategory').mockResolvedValueOnce({
      id: 2,
      name: 'Khoa học dữ liệu',
      parentId: 1,
      parentName: 'Công nghệ thông tin',
      active: true,
    } as unknown as CategoryDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <CategoryModal
        isOpen={true}
        categoryToEdit={mockCategories[1] ?? null}
        categories={mockCategories}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByText('Chỉnh sửa Danh mục')).toBeInTheDocument();

    const nameInput = screen.getByLabelText(/Tên danh mục/i);
    fireEvent.change(nameInput, { target: { value: 'Khoa học dữ liệu' } });

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(2, {
        name: 'Khoa học dữ liệu',
        parentId: 1,
      });
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('returns null when isOpen is false', () => {
    const { container } = render(
      <CategoryModal
        isOpen={false}
        categoryToEdit={null}
        categories={[]}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });
});
