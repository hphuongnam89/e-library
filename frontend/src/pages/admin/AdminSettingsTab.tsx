import { useState, useCallback, useEffect } from 'react';
import type { SystemSetting } from '../../types/admin';
import { fetchSystemSettings, updateSystemSetting } from '../../api/admin';
import { SettingEditModal } from '../../components/admin/SettingEditModal';

interface AdminSettingsTabProps {
  onSuccess: (message: string) => void;
  onError: (message: string) => void;
}

export function AdminSettingsTab({ onSuccess, onError }: AdminSettingsTabProps) {
  const [settings, setSettings] = useState<SystemSetting[]>([]);
  const [isSettingsLoading, setIsSettingsLoading] = useState(false);
  const [editingSetting, setEditingSetting] = useState<SystemSetting | null>(null);

  const loadSettings = useCallback((signal?: AbortSignal) => {
    setIsSettingsLoading(true);
    fetchSystemSettings(signal)
      .then((res) => setSettings(res))
      .catch((err) => {
        if (!signal?.aborted) onError(err.message || 'Không thể tải danh sách cấu hình');
      })
      .finally(() => {
        if (!signal?.aborted) setIsSettingsLoading(false);
      });
  }, [onError]);

  useEffect(() => {
    const controller = new AbortController();
    loadSettings(controller.signal);
    return () => controller.abort();
  }, [loadSettings]);

  const handleSaveSetting = async (key: string, value: string) => {
    await updateSystemSetting(key, value);
    onSuccess(`Đã cập nhật cấu hình ${key}`);
    loadSettings();
  };

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-xs">
      <div className="mb-4">
        <h2 className="text-lg font-bold text-slate-900">Cấu Hình Toàn Cục</h2>
        <p className="text-xs text-slate-500">
          Các thông số vận hành của thư viện số E-LIB. Các khóa bí mật sẽ tự động được che giấu.
        </p>
      </div>

      {isSettingsLoading ? (
        <div className="py-8 text-center text-slate-400">Đang tải cấu hình...</div>
      ) : settings.length === 0 ? (
        <div className="py-8 text-center text-slate-400">Chưa có cấu hình nào.</div>
      ) : (
        <div className="divide-y divide-slate-100">
          {settings.map((s) => (
            <div key={s.id} className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-sm font-semibold text-blue-950">{s.key}</span>
                  {s.isSecret && (
                    <span className="rounded-full bg-rose-100 text-rose-800 text-[10px] font-bold px-2 py-0.5">
                      🔒 Bảo mật
                    </span>
                  )}
                </div>
                {s.description && (
                  <p className="mt-1 text-xs text-slate-500">{s.description}</p>
                )}
                <div className="mt-2 text-xs">
                  <span className="text-slate-400">Giá trị: </span>
                  <code className="rounded-sm bg-slate-100 px-2 py-0.5 font-mono text-xs font-semibold text-slate-800">
                    {s.value}
                  </code>
                </div>
                {s.updatedAt && (
                  <div className="mt-1 text-[11px] text-slate-400">
                    Cập nhật: {new Date(s.updatedAt).toLocaleString('vi-VN')} {s.updatedByEmail ? `bởi ${s.updatedByEmail}` : ''}
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => setEditingSetting(s)}
                className="self-start sm:self-auto rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors shadow-xs"
              >
                Thay đổi
              </button>
            </div>
          ))}
        </div>
      )}

      {editingSetting && (
        <SettingEditModal
          setting={editingSetting}
          onSave={handleSaveSetting}
          onClose={() => setEditingSetting(null)}
        />
      )}
    </div>
  );
}
