import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useAuth } from './useAuth';

describe('useAuth hook', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('starts in loading state then transitions to unauthenticated when 401', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(null, { status: 401 }),
    );

    const { result } = renderHook(() => useAuth());

    expect(result.current.isLoading).toBe(true);
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
  });

  it('transitions to authenticated state when /api/v1/auth/me returns user', async () => {
    const mockUser = {
      id: 42,
      email: 'user@phuxuan.edu.vn',
      fullName: 'Trần Thị B',
      studentCode: null,
      role: 'LECTURER',
      status: 'ACTIVE',
    };

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify(mockUser), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));
    expect(result.current.user).toEqual(mockUser);
    expect(result.current.isLoading).toBe(false);
  });

  it('logout sends POST request and resets state to unauthenticated', async () => {
    const mockUser = {
      id: 1,
      email: 'user@phuxuan.edu.vn',
      fullName: 'Nguyễn Văn A',
      studentCode: 'PXU001',
      role: 'STUDENT',
      status: 'ACTIVE',
    };

    const fetchSpy = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(
        new Response(JSON.stringify(mockUser), { status: 200 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'test-csrf-token' }), { status: 200 }),
      )
      .mockResolvedValueOnce(
        new Response(null, { status: 204 }),
      );

    const { result } = renderHook(() => useAuth());

    await waitFor(() => expect(result.current.isAuthenticated).toBe(true));

    await act(async () => {
      await result.current.logout();
    });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.user).toBeNull();
    expect(fetchSpy).toHaveBeenCalledWith(
      '/api/v1/auth/logout',
      expect.objectContaining({ method: 'POST' }),
    );
  });
});
