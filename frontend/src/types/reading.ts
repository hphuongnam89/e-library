export interface StartReadingSessionRequest {
  documentId: number;
}

export interface StartReadingSessionResponse {
  sessionId: string;
  heartbeatIntervalSeconds: number;
  idleTimeoutSeconds: number;
}

export interface HeartbeatRequest {
  sequenceNumber: number;
  active: boolean;
  visible: boolean;
}

export interface HeartbeatResponse {
  accepted: boolean;
  activeSeconds: number;
  sessionEnded: boolean;
}

export interface EndReadingSessionResponse {
  success: boolean;
  totalActiveSeconds: number;
}

export interface ReadingHistoryItem {
  documentId: number;
  title: string;
  publisher?: string;
  activeSeconds: number;
  sessionCount: number;
  lastActiveAt?: string;
}
