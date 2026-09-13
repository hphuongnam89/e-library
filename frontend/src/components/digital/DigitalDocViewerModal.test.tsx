import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DigitalDocViewerModal } from './DigitalDocViewerModal';
import type { DigitalDocumentDto } from '../../types/digital';
import type { UserDto } from '../../hooks/useAuth';

describe('DigitalDocViewerModal', () => {
  const mockDoc: DigitalDocumentDto = {
    id: 1,
    libraryId: 1,
    libraryName: 'Thư viện Trung tâm',
    title: 'Giáo trình Cấu trúc dữ liệu & Giải thuật',
    description: 'Tài liệu chuẩn ngành CNTT',
    publisher: 'Đại học Phú Xuân',
    categoryId: 2,
    categoryName: 'Công nghệ thông tin',
    contentType: 'application/pdf',
    sizeBytes: 2097152, // 2MB
    status: 'PUBLISHED',
    permission: 'AUTHENTICATED',
    isActive: true,
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
  };

  const mockUser: UserDto = {
    id: 10,
    email: 'sinhvien@pxu.edu.vn',
    fullName: 'Nguyễn Văn An',
    studentCode: 'PXU2026001',
    role: 'STUDENT',
    status: 'ACTIVE',
  };

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('renders nothing when isOpen is false or document is null', () => {
    const { container: c1 } = render(
      <DigitalDocViewerModal isOpen={false} document={mockDoc} currentUser={mockUser} onClose={vi.fn()} />
    );
    expect(c1.firstChild).toBeNull();

    const { container: c2 } = render(
      <DigitalDocViewerModal isOpen={true} document={null} currentUser={mockUser} onClose={vi.fn()} />
    );
    expect(c2.firstChild).toBeNull();
  });

  it('renders document title, size, and reader watermark with user info', () => {
    render(
      <DigitalDocViewerModal isOpen={true} document={mockDoc} currentUser={mockUser} onClose={vi.fn()} />
    );

    expect(screen.getByText('Giáo trình Cấu trúc dữ liệu & Giải thuật')).toBeInTheDocument();
    expect(screen.getByText('2.00 MB')).toBeInTheDocument();
    expect(screen.getByText('Công nghệ thông tin')).toBeInTheDocument();

    const watermarkContainer = screen.getByTestId('reader-watermark');
    expect(watermarkContainer).toBeInTheDocument();
    expect(watermarkContainer.textContent).toContain('Nguyễn Văn An • sinhvien@pxu.edu.vn • PXU2026001');
  });

  it('calls onClose when close button is clicked', async () => {
    const user = userEvent.setup();
    const handleClose = vi.fn();

    render(
      <DigitalDocViewerModal isOpen={true} document={mockDoc} currentUser={mockUser} onClose={handleClose} />
    );

    const closeBtn = screen.getByRole('button', { name: 'Đóng trình đọc' });
    await user.click(closeBtn);

    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});
