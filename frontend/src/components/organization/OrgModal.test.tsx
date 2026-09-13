import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OrgModal } from './OrgModal';
import * as orgApi from '../../api/organization';
import type { CampusDto, InstitutionDto } from '../../types/organization';

describe('OrgModal component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('creates institution successfully', async () => {
    const createSpy = vi.spyOn(orgApi, 'createInstitution').mockResolvedValueOnce({
      id: 1,
      name: 'Đại học Phú Xuân',
    } as unknown as InstitutionDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <OrgModal
        isOpen={true}
        type="INSTITUTION"
        itemToEdit={null}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByText('Thêm Cơ quan / Trường Mới')).toBeInTheDocument();

    const nameInput = screen.getByPlaceholderText(/Nhập tên/i);
    fireEvent.change(nameInput, { target: { value: 'Đại học Phú Xuân' } });

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith({ name: 'Đại học Phú Xuân' });
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('updates campus successfully', async () => {
    const updateSpy = vi.spyOn(orgApi, 'updateCampus').mockResolvedValueOnce({
      id: 2,
      institutionId: 1,
      name: 'Cơ sở 2 - An Tây',
    } as unknown as CampusDto);

    const onClose = vi.fn();
    const onSuccess = vi.fn();

    render(
      <OrgModal
        isOpen={true}
        type="CAMPUS"
        parentId={1}
        itemToEdit={{ id: 2, name: 'Cơ sở 2' }}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    );

    expect(screen.getByText('Chỉnh sửa Cơ sở')).toBeInTheDocument();

    const nameInput = screen.getByPlaceholderText(/Nhập tên/i);
    fireEvent.change(nameInput, { target: { value: 'Cơ sở 2 - An Tây' } });

    const form = screen.getByRole('dialog').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(updateSpy).toHaveBeenCalledWith(2, { name: 'Cơ sở 2 - An Tây' });
      expect(onSuccess).toHaveBeenCalled();
      expect(onClose).toHaveBeenCalled();
    });
  });

  it('returns null when isOpen is false', () => {
    const { container } = render(
      <OrgModal
        isOpen={false}
        type="LIBRARY"
        itemToEdit={null}
        onClose={vi.fn()}
        onSuccess={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });
});
