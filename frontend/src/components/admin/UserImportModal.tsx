import { useState } from 'react';
import type { UserImportResult } from '../../types/admin';
import { importUsersExcel } from '../../api/admin';

interface UserImportModalProps {
  onImportComplete: (result: UserImportResult) => void;
  onClose: () => void;
}

export function UserImportModal({ onImportComplete, onClose }: UserImportModalProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importResult, setImportResult] = useState<UserImportResult | null>(null);
  const [importModalError, setImportModalError] = useState<string | null>(null);

  const handleImportSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) return;
    setIsImporting(true);
    setImportModalError(null);
    setImportResult(null);

    try {
      const res = await importUsersExcel(selectedFile);
      setImportResult(res);
      onImportComplete(res);
    } catch (err: unknown) {
      setImportModalError((err as Error).message || 'Nhập file Excel thất bại');
    } finally {
      setIsImporting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="w-full max-w-xl rounded-xl bg-white p-6 shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-bold text-slate-900">Nhập Người Dùng Từ Excel</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600"
          >
            ✕
          </button>
        </div>

        <div className="mb-4 rounded-lg bg-blue-50 p-3 text-xs text-blue-900 leading-relaxed">
          <p className="font-semibold mb-1">Quy cách định dạng file Excel (.xlsx):</p>
          <ul className="list-disc list-inside space-y-0.5">
            <li><strong>Cột 1:</strong> Email (bắt buộc, thuộc domain được cho phép)</li>
            <li><strong>Cột 2:</strong> Họ và tên (bắt buộc)</li>
            <li><strong>Cột 3:</strong> Mã SV / GV (tùy chọn)</li>
            <li><strong>Cột 4:</strong> Vai trò (STUDENT, LECTURER, LIBRARIAN, ADMIN - mặc định STUDENT)</li>
            <li><strong>Cột 5:</strong> Khoa/Phòng ban (tùy chọn, tên hoặc ID)</li>
          </ul>
        </div>

        {importModalError && (
          <div className="mb-4 rounded-lg bg-red-50 p-3 text-xs text-red-700 border border-red-200">
            {importModalError}
          </div>
        )}

        {!importResult ? (
          <form onSubmit={handleImportSubmit} className="space-y-4">
            <div>
              <label htmlFor="excel-file-input" className="block text-xs font-semibold text-slate-700 mb-1">
                Chọn tệp tin Excel (.xlsx, .xls)
              </label>
              <input
                id="excel-file-input"
                type="file"
                accept=".xlsx, .xls"
                onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                className="w-full rounded-lg border border-slate-300 p-2 text-sm text-slate-600"
              />
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                disabled={isImporting}
                onClick={onClose}
                className="rounded-lg border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Đóng
              </button>
              <button
                type="submit"
                disabled={!selectedFile || isImporting}
                className="rounded-lg bg-emerald-700 px-4 py-2 text-xs font-semibold text-white hover:bg-emerald-800 disabled:opacity-50"
              >
                {isImporting ? 'Đang xử lý...' : 'Tải Lên & Nhập Liệu'}
              </button>
            </div>
          </form>
        ) : (
          <div>
            {/* Result Summary */}
            <div className="grid grid-cols-3 gap-3 mb-4 text-center">
              <div className="rounded-lg bg-slate-100 p-3">
                <div className="text-xl font-bold text-slate-900">{importResult.totalRows}</div>
                <div className="text-[11px] text-slate-500 font-medium">Tổng số dòng</div>
              </div>
              <div className="rounded-lg bg-emerald-50 p-3 border border-emerald-200">
                <div className="text-xl font-bold text-emerald-700">{importResult.importedCount}</div>
                <div className="text-[11px] text-emerald-600 font-medium">Thành công</div>
              </div>
              <div className="rounded-lg bg-rose-50 p-3 border border-rose-200">
                <div className="text-xl font-bold text-rose-700">{importResult.failedCount}</div>
                <div className="text-[11px] text-rose-600 font-medium">Thất bại</div>
              </div>
            </div>

            {/* Errors List */}
            {importResult.errors.length > 0 && (
              <div className="mb-4">
                <h4 className="text-xs font-bold uppercase text-slate-600 mb-2">Chi tiết các dòng bị lỗi:</h4>
                <div className="max-h-48 overflow-y-auto rounded-lg border border-slate-200">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-500 border-b border-slate-200">
                      <tr>
                        <th className="px-3 py-2">Dòng</th>
                        <th className="px-3 py-2">Email</th>
                        <th className="px-3 py-2">Lý do</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {importResult.errors.map((err, i) => (
                        <tr key={i} className="hover:bg-slate-50">
                          <td className="px-3 py-2 font-mono font-bold text-slate-700">{err.rowNumber}</td>
                          <td className="px-3 py-2 text-slate-800">{err.email || '—'}</td>
                          <td className="px-3 py-2 text-rose-600">{err.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => {
                  setImportResult(null);
                  setSelectedFile(null);
                }}
                className="rounded-lg border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Nhập tệp khác
              </button>
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg bg-blue-900 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-800"
              >
                Hoàn tất
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
