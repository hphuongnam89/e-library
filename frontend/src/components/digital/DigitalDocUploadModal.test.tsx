import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DigitalDocUploadModal } from './DigitalDocUploadModal';
import * as digitalApi from '../../api/digital';
import * as orgApi from '../../api/organization';
import * as catalogApi from '../../api/catalog';
import type { LibraryDto } from '../../types/organization';
import type { CategoryDto } from '../../types/catalog';
import type { DigitalDocumentDto } from '../../types/digital';

describe('DigitalDocUploadModal component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(orgApi, 'fetchLibraries').mockResolvedValue({
      content: [{ id: 1, name: 'Thư viện Trung tâm', campusId: 1, address: 'HN' }] as unknown as LibraryDto[],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
      empty: false,
    });
    vi.spyOn(catalogApi, 'fetchCategories').mockResolvedValue({
      content: [{ id: 10, name: 'Công nghệ thông tin', parentId: null, active: true }] as unknown as CategoryDto[],
      totalPages: 1,
      totalElements: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
      empty: false,
    });
  });

  it('renders upload modal and submits PDF file', async () => {
    const uploadSpy = vi.spyOn(digitalApi, 'uploadDigitalDocument').mockResolvedValueOnce({
      id: 1,
      title: 'Giáo trình CSDL',
    } as unknown as DigitalDocumentDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <DigitalDocUploadModal
        isOpen={true}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByText(/Tải lên Tài liệu Số Mới \(PDF\)/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(orgApi.fetchLibraries).toHaveBeenCalled();
      expect(catalogApi.fetchCategories).toHaveBeenCalled();
    });

    const file = new File(['fake pdf data'], 'sample.pdf', { type: 'application/pdf' });
    const fileInput = screen.getByLabelText(/Tệp PDF/i);
    fireEvent.change(fileInput, { target: { files: [file] } });

    const titleInput = screen.getByLabelText(/Tiêu đề tài liệu/i);
    fireEvent.change(titleInput, { target: { value: 'Giáo trình CSDL' } });

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(uploadSpy).toHaveBeenCalledWith(expect.any(FormData));
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('returns null when isOpen is false', () => {
    const { container } = render(
      <DigitalDocUploadModal
        isOpen={false}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });
});
