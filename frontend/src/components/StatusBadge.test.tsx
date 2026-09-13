import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { BookCopyStatus } from '../types/catalog';
import { BookCopyStatusBadge, BorrowStatusBadge } from './StatusBadge';

describe('StatusBadge components', () => {
  it('renders all book copy status badge variants', () => {
    const { rerender } = render(<BookCopyStatusBadge status="AVAILABLE" />);
    expect(screen.getByText('Có sẵn')).toBeInTheDocument();

    rerender(<BookCopyStatusBadge status="BORROWED" />);
    expect(screen.getByText('Đang mượn')).toBeInTheDocument();

    rerender(<BookCopyStatusBadge status="LOST" />);
    expect(screen.getByText('Thất lạc')).toBeInTheDocument();

    rerender(<BookCopyStatusBadge status="DAMAGED" />);
    expect(screen.getByText('Hư hỏng')).toBeInTheDocument();

    rerender(<BookCopyStatusBadge status="MAINTENANCE" />);
    expect(screen.getByText('Bảo trì')).toBeInTheDocument();

    rerender(<BookCopyStatusBadge status={'CUSTOM' as unknown as BookCopyStatus} />);
    expect(screen.getByText('CUSTOM')).toBeInTheDocument();
  });

  it('renders borrow status badge variants', () => {
    const { rerender } = render(<BorrowStatusBadge status="BORROWED" overdue={false} />);
    expect(screen.getByText('Đang mượn')).toBeInTheDocument();

    rerender(<BorrowStatusBadge status="BORROWED" overdue={true} />);
    expect(screen.getByText('Quá hạn')).toBeInTheDocument();

    rerender(<BorrowStatusBadge status="RETURNED" />);
    expect(screen.getByText('Đã trả')).toBeInTheDocument();
  });
});
