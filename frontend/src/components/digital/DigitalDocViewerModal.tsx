import { useEffect } from 'react';
import type { DigitalDocumentDto } from '../../types/digital';
import { getDocumentStreamUrl } from '../../api/digital';
import type { UserDto } from '../../hooks/useAuth';
import { useReadingHeartbeat } from '../../hooks/useReadingHeartbeat';

interface DigitalDocViewerModalProps {
  isOpen: boolean;
  document: DigitalDocumentDto | null;
  currentUser: UserDto | null;
  onClose: () => void;
}

export function DigitalDocViewerModal({
  isOpen,
  document: doc,
  currentUser,
  onClose,
}: DigitalDocViewerModalProps) {
  const { formattedTime, isIdle, isVisible } = useReadingHeartbeat({
    documentId: doc?.id,
    enabled: isOpen && !!doc,
  });
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !doc) return null;

  const streamUrl = getDocumentStreamUrl(doc.id) + '#toolbar=0&navpanes=0';
  const sizeMb = (doc.sizeBytes / (1024 * 1024)).toFixed(2);
  const watermarkText = currentUser
    ? `${currentUser.fullName} • ${currentUser.email}${currentUser.studentCode ? ` • ${currentUser.studentCode}` : ''}`
    : 'E-LIB READER';

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="reader-modal-title"
      className="fixed inset-0 z-50 flex flex-col bg-slate-900/90 backdrop-blur-xs text-white"
    >
      {/* Top Header Bar */}
      <header className="flex h-14 shrink-0 items-center justify-between border-b border-slate-700 bg-slate-900 px-6">
        <div className="flex items-center space-x-3 overflow-hidden">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white font-bold text-xs">
            PDF
          </span>
          <div className="truncate">
            <h2 id="reader-modal-title" className="text-sm font-semibold truncate text-white">
              {doc.title}
            </h2>
            <div className="flex items-center space-x-2 text-xs text-slate-400">
              {doc.publisher && <span>{doc.publisher}</span>}
              <span>•</span>
              <span>{sizeMb} MB</span>
              {doc.categoryName && (
                <>
                  <span>•</span>
                  <span className="text-blue-400">{doc.categoryName}</span>
                </>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <div
            data-testid="reading-time-badge"
            className="flex items-center rounded-md bg-slate-800 px-3 py-1 text-xs text-slate-300 font-mono"
            title={isIdle ? 'Tạm dừng đo do không có tương tác' : !isVisible ? 'Tab đang ẩn' : 'Đang đo thời gian đọc'}
          >
            <span
              className={`inline-block h-2 w-2 rounded-full mr-2 ${
                isIdle ? 'bg-amber-400' : !isVisible ? 'bg-slate-500' : 'bg-emerald-400 animate-pulse'
              }`}
            />
            <span>⏱️ {formattedTime}</span>
            {isIdle && <span className="ml-1 text-amber-400 text-[10px]">(Nhàn rỗi)</span>}
          </div>

          <div className="hidden sm:flex items-center rounded-md bg-slate-800 px-3 py-1 text-xs text-slate-300">
            <span className="inline-block h-2 w-2 rounded-full bg-blue-400 mr-2" />
            Bản quyền E-LIB
          </div>

          <button
            type="button"
            onClick={onClose}
            className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 hover:text-white transition"
            aria-label="Đóng trình đọc"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </header>

      {/* Main Reader Container */}
      <div className="relative flex-1 bg-slate-950 overflow-hidden select-none">
        {/* Anti-piracy Watermark Overlay */}
        <div
          data-testid="reader-watermark"
          className="pointer-events-none absolute inset-0 z-20 flex flex-wrap items-center justify-around opacity-15 overflow-hidden p-6"
          aria-hidden="true"
        >
          {Array.from({ length: 12 }).map((_, i) => (
            <div
              key={i}
              className="m-8 rotate-[-25deg] text-base font-mono font-bold tracking-widest text-slate-400 select-none"
            >
              {watermarkText}
            </div>
          ))}
        </div>

        {/* PDF Stream Object */}
        <object
          data={streamUrl}
          type="application/pdf"
          className="w-full h-full border-none"
          title={doc.title}
        >
          <div className="flex h-full flex-col items-center justify-center p-6 text-center text-slate-300">
            <p className="mb-4 text-base">Trình duyệt không thể nhúng trực tiếp tài liệu PDF.</p>
            <a
              href={getDocumentStreamUrl(doc.id)}
              target="_blank"
              rel="noreferrer"
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-500"
            >
              Mở tài liệu trong tab mới
            </a>
          </div>
        </object>
      </div>
    </div>
  );
}
