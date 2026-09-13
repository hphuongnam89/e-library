import { useState } from 'react';
import type { SystemSetting } from '../../types/admin';

interface SettingEditModalProps {
  setting: SystemSetting;
  onSave: (key: string, value: string) => Promise<void>;
  onClose: () => void;
}

export function SettingEditModal({ setting, onSave, onClose }: SettingEditModalProps) {
  const [settingNewValue, setSettingNewValue] = useState(setting.isSecret ? '' : setting.value);
  const [isUpdating, setIsUpdating] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsUpdating(true);
    setModalError(null);

    try {
      await onSave(setting.key, settingNewValue);
      onClose();
    } catch (err: unknown) {
      setModalError((err as Error).message || 'Cập nhật cấu hình thất bại');
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        <h3 className="text-lg font-bold text-slate-900 mb-2">Thay Đổi Cấu Hình</h3>
        <p className="font-mono text-xs text-blue-900 mb-1 font-semibold">{setting.key}</p>
        {setting.description && (
          <p className="text-xs text-slate-500 mb-4">{setting.description}</p>
        )}

        {modalError && (
          <div className="mb-4 rounded-lg bg-red-50 p-3 text-xs text-red-700 border border-red-200">
            {modalError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="modal-setting-value" className="block text-xs font-semibold text-slate-700 mb-1">
              Giá trị mới {setting.isSecret && '(Nhập mật mã / khóa mới)'}
            </label>
            <input
              id="modal-setting-value"
              type={setting.isSecret ? 'password' : 'text'}
              value={settingNewValue}
              onChange={(e) => setSettingNewValue(e.target.value)}
              placeholder={setting.isSecret ? 'Nhập giá trị bí mật mới' : 'Giá trị cấu hình...'}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-blue-800 focus:outline-hidden"
              required
            />
          </div>

          <div className="mt-6 flex justify-end gap-3">
            <button
              type="button"
              disabled={isUpdating}
              onClick={onClose}
              className="rounded-lg border border-slate-300 px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isUpdating}
              className="rounded-lg bg-blue-900 px-4 py-2 text-xs font-semibold text-white hover:bg-blue-800 disabled:opacity-50"
            >
              {isUpdating ? 'Đang lưu...' : 'Cập Nhật'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
