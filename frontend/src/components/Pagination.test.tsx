import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from './Pagination';

describe('Pagination Component', () => {
  it('renders null when totalPages <= 1', () => {
    const { container } = render(
      <Pagination currentPage={0} totalPages={1} onPageChange={vi.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders pagination controls and triggers onPageChange when clicking next and prev', () => {
    const onPageChange = vi.fn();
    render(
      <Pagination currentPage={1} totalPages={5} onPageChange={onPageChange} />
    );

    expect(screen.getByRole('button', { name: '2' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '5' })).toBeInTheDocument();

    const nextButtons = screen.getAllByRole('button', { name: /sau/i });
    if (nextButtons[0]) fireEvent.click(nextButtons[0]);
    expect(onPageChange).toHaveBeenCalledWith(2);

    const prevButtons = screen.getAllByRole('button', { name: /trước/i });
    if (prevButtons[0]) fireEvent.click(prevButtons[0]);
    expect(onPageChange).toHaveBeenCalledWith(0);
  });

  it('disables previous button on first page', () => {
    const onPageChange = vi.fn();
    render(
      <Pagination currentPage={0} totalPages={3} onPageChange={onPageChange} />
    );

    const prevButtons = screen.getAllByRole('button', { name: /trước/i });
    expect(prevButtons[0]).toBeDisabled();
  });

  it('disables next button on last page', () => {
    const onPageChange = vi.fn();
    render(
      <Pagination currentPage={2} totalPages={3} onPageChange={onPageChange} />
    );

    const nextButtons = screen.getAllByRole('button', { name: /sau/i });
    expect(nextButtons[0]).toBeDisabled();
  });
});
