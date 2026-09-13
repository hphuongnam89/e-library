import { useCallback, useEffect, useState } from 'react';

export type UserRole = 'STUDENT' | 'LECTURER' | 'LIBRARIAN' | 'ADMIN';
export type UserStatus = 'ACTIVE' | 'INACTIVE';

export interface UserDto {
  id: number;
  email: string;
  fullName: string;
  studentCode: string | null;
  role: UserRole;
  status: UserStatus;
}

export type AuthState =
  | { status: 'loading'; user: null }
  | { status: 'authenticated'; user: UserDto }
  | { status: 'unauthenticated'; user: null };

function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(^|;\\s*)(' + name + ')=([^;]*)'));
  return match && match[3] ? decodeURIComponent(match[3]) : null;
}

export function useAuth() {
  const [state, setState] = useState<AuthState>({ status: 'loading', user: null });

  const fetchUser = useCallback(async (signal?: AbortSignal) => {
    try {
      const res = await fetch('/api/v1/auth/me', {
        signal,
        cache: 'no-store',
        headers: { Accept: 'application/json' },
      });

      if (res.ok) {
        const user: UserDto = await res.json();
        setState({ status: 'authenticated', user });
      } else {
        setState({ status: 'unauthenticated', user: null });
      }
    } catch (err: unknown) {
      if (err instanceof Error && err.name === 'AbortError') return;
      setState({ status: 'unauthenticated', user: null });
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void fetchUser(controller.signal);
    return () => controller.abort();
  }, [fetchUser]);

  const login = useCallback(() => {
    window.location.href = '/oauth2/authorization/google';
  }, []);

  const logout = useCallback(async () => {
    try {
      let xsrfToken = getCookie('XSRF-TOKEN');
      if (!xsrfToken) {
        const csrfRes = await fetch('/api/v1/auth/csrf', { cache: 'no-store' });
        if (csrfRes.ok) {
          const data = await csrfRes.json();
          xsrfToken = data.token;
        }
      }

      await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: {
          ...(xsrfToken ? { 'X-XSRF-TOKEN': xsrfToken } : {}),
        },
      });
    } finally {
      setState({ status: 'unauthenticated', user: null });
    }
  }, []);

  return {
    ...state,
    isLoading: state.status === 'loading',
    isAuthenticated: state.status === 'authenticated',
    login,
    logout,
    refetch: fetchUser,
  };
}
