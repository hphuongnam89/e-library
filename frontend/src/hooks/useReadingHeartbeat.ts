import { useEffect, useRef, useState, useCallback } from 'react';
import { endReadingSession, sendReadingHeartbeat, startReadingSession } from '../api/reading';

export interface UseReadingHeartbeatOptions {
  documentId?: number | null;
  enabled?: boolean;
}

export interface UseReadingHeartbeatReturn {
  sessionId: string | null;
  activeSeconds: number;
  isIdle: boolean;
  isVisible: boolean;
  formattedTime: string;
}

export function formatReadingTime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) {
    return `${h}g ${m.toString().padStart(2, '0')}p ${s.toString().padStart(2, '0')}s`;
  }
  return `${m.toString().padStart(2, '0')}p ${s.toString().padStart(2, '0')}s`;
}

export function useReadingHeartbeat({
  documentId,
  enabled = true,
}: UseReadingHeartbeatOptions): UseReadingHeartbeatReturn {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [activeSeconds, setActiveSeconds] = useState<number>(0);
  const [isIdle, setIsIdle] = useState<boolean>(false);
  const [isVisible, setIsVisible] = useState<boolean>(
    typeof document !== 'undefined' ? document.visibilityState === 'visible' : true
  );

  const sessionIdRef = useRef<string | null>(null);
  const sequenceRef = useRef<number>(0);
  const lastActivityTimeRef = useRef<number>(Date.now());
  const isIdleRef = useRef<boolean>(false);

  // Synchronize ref with state
  sessionIdRef.current = sessionId;
  isIdleRef.current = isIdle;

  const handleUserActivity = useCallback(() => {
    lastActivityTimeRef.current = Date.now();
    if (isIdleRef.current) {
      isIdleRef.current = false;
      setIsIdle(false);
    }
  }, []);

  // Track user interaction for 60s idle detection
  useEffect(() => {
    if (!enabled || !documentId) return;

    const events = ['mousemove', 'keydown', 'scroll', 'touchstart', 'click'];
    const onActivity = () => handleUserActivity();

    events.forEach((evt) => window.addEventListener(evt, onActivity, { passive: true }));

    const idleChecker = window.setInterval(() => {
      const elapsed = Date.now() - lastActivityTimeRef.current;
      if (elapsed >= 60000 && !isIdleRef.current) {
        isIdleRef.current = true;
        setIsIdle(true);
      }
    }, 2000);

    return () => {
      events.forEach((evt) => window.removeEventListener(evt, onActivity));
      window.clearInterval(idleChecker);
    };
  }, [enabled, documentId, handleUserActivity]);

  // Track browser visibility
  useEffect(() => {
    if (!enabled || !documentId) return;

    const onVisibilityChange = () => {
      const visible = document.visibilityState === 'visible';
      setIsVisible(visible);
      if (visible) {
        handleUserActivity();
      }
    };

    document.addEventListener('visibilitychange', onVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [enabled, documentId, handleUserActivity]);

  // Session lifecycle and heartbeat loop
  useEffect(() => {
    if (!enabled || !documentId) {
      setSessionId(null);
      setActiveSeconds(0);
      return;
    }

    let isMounted = true;
    let heartbeatTimer: number | null = null;

    async function initSession() {
      try {
        const res = await startReadingSession(documentId!);
        if (!isMounted) return;

        setSessionId(res.sessionId);
        sessionIdRef.current = res.sessionId;
        sequenceRef.current = 0;
        lastActivityTimeRef.current = Date.now();

        // Start heartbeat interval (15s)
        const intervalMs = (res.heartbeatIntervalSeconds || 15) * 1000;
        heartbeatTimer = window.setInterval(async () => {
          const currentSid = sessionIdRef.current;
          if (!currentSid) return;

          const seq = ++sequenceRef.current;
          const active = !isIdleRef.current;
          const visible = document.visibilityState === 'visible';

          try {
            const hbRes = await sendReadingHeartbeat(currentSid, {
              sequenceNumber: seq,
              active,
              visible,
            });
            if (isMounted && hbRes.accepted) {
              setActiveSeconds(hbRes.activeSeconds);
            }
            if (hbRes.sessionEnded && heartbeatTimer != null) {
              window.clearInterval(heartbeatTimer);
            }
          } catch {
            // Heartbeat network error will be retried on next cadence
          }
        }, intervalMs);
      } catch {
        // Handle session initialization failure
      }
    }

    initSession();

    // Close session gracefully on beforeunload
    const handleBeforeUnload = () => {
      const currentSid = sessionIdRef.current;
      if (currentSid) {
        fetch(`/api/v1/reading/sessions/${currentSid}/end`, {
          method: 'POST',
          keepalive: true,
        }).catch(() => {});
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      isMounted = false;
      window.removeEventListener('beforeunload', handleBeforeUnload);

      if (heartbeatTimer != null) {
        window.clearInterval(heartbeatTimer);
      }

      const currentSid = sessionIdRef.current;
      if (currentSid) {
        Promise.resolve(endReadingSession(currentSid)).catch(() => {});
      }
    };
  }, [enabled, documentId]);

  return {
    sessionId,
    activeSeconds,
    isIdle,
    isVisible,
    formattedTime: formatReadingTime(activeSeconds),
  };
}
