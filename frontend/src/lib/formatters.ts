export function formatDate(dateStr: string | null | undefined, options?: { includeTime?: boolean }): string {
  if (!dateStr) return '—';
  if (options?.includeTime) {
    return new Date(dateStr).toLocaleString('vi-VN');
  }
  return new Date(dateStr).toLocaleDateString('vi-VN');
}

export function formatCurrency(amount: number | null | undefined): string {
  if (!amount || amount <= 0) return '—';
  return `${amount.toLocaleString('vi-VN')} đ`;
}
