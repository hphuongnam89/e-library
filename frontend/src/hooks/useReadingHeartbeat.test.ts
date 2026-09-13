import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { formatReadingTime, useReadingHeartbeat } from './useReadingHeartbeat';
import * as readingApi from '../api/reading';

vi.mock('../api/reading', () => ({
  startReadingSession: vi.fn().mockResolvedValue({
    sessionId: 'default-session-id',
    heartbeatIntervalSeconds: 15,
    idleTimeoutSeconds: 60,
  }),
  sendReadingHeartbeat: vi.fn().mockResolvedValue({ accepted: true, activeSeconds: 15, sessionEnded: false }),
  endReadingSession: vi.fn().mockResolvedValue({ success: true, totalActiveSeconds: 15 }),
}));

describe('useReadingHeartbeat', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('formatReadingTime formats seconds correctly', () => {
    expect(formatReadingTime(45)).toBe('00p 45s');
    expect(formatReadingTime(125)).toBe('02p 05s');
    expect(formatReadingTime(3665)).toBe('1g 01p 05s');
  });

  it('initializes reading session when enabled with documentId', async () => {
    const mockStart = vi.mocked(readingApi.startReadingSession).mockResolvedValueOnce({
      sessionId: 'session-uuid-123',
      heartbeatIntervalSeconds: 15,
      idleTimeoutSeconds: 60,
    });

    const { result } = renderHook(() =>
      useReadingHeartbeat({ documentId: 1, enabled: true })
    );

    await waitFor(() => {
      expect(mockStart).toHaveBeenCalledWith(1);
      expect(result.current.sessionId).toBe('session-uuid-123');
    });
  });

  it('calls endReadingSession on unmount', async () => {
    vi.mocked(readingApi.startReadingSession).mockResolvedValueOnce({
      sessionId: 'session-uuid-456',
      heartbeatIntervalSeconds: 15,
      idleTimeoutSeconds: 60,
    });
    const mockEnd = vi.mocked(readingApi.endReadingSession).mockResolvedValueOnce({
      success: true,
      totalActiveSeconds: 30,
    });

    const { unmount } = renderHook(() =>
      useReadingHeartbeat({ documentId: 1, enabled: true })
    );

    await waitFor(() => {
      expect(readingApi.startReadingSession).toHaveBeenCalledWith(1);
    });

    unmount();

    await waitFor(() => {
      expect(mockEnd).toHaveBeenCalledWith('session-uuid-456');
    });
  });
});
