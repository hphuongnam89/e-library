import { useEffect, useState } from 'react';
import {
  createCampus,
  createDepartment,
  createInstitution,
  createLibrary,
  updateCampus,
  updateDepartment,
  updateInstitution,
  updateLibrary,
} from '../../api/organization';

export type OrgType = 'INSTITUTION' | 'CAMPUS' | 'LIBRARY' | 'DEPARTMENT';

interface OrgModalProps {
  isOpen: boolean;
  type: OrgType;
  parentId?: number; // institutionId for Campus, campusId for Library, libraryId for Department
  itemToEdit?: { id: number; name: string; address?: string } | null;
  onClose: () => void;
  onSuccess: () => void;
}

const TYPE_NAMES: Record<OrgType, string> = {
  INSTITUTION: 'Cơ quan / Trường',
  CAMPUS: 'Cơ sở',
  LIBRARY: 'Thư viện',
  DEPARTMENT: 'Khoa / Phòng ban',
};

export function OrgModal({
  isOpen,
  type,
  parentId,
  itemToEdit,
  onClose,
  onSuccess,
}: OrgModalProps) {
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (itemToEdit) {
      setName(itemToEdit.name);
      setAddress(itemToEdit.address || '');
    } else {
      setName('');
      setAddress('');
    }
    setError(null);
  }, [itemToEdit, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanName = name.trim();
    if (!cleanName) {
      setError('Vui lòng nhập tên đơn vị.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      if (itemToEdit) {
        if (type === 'INSTITUTION') await updateInstitution(itemToEdit.id, { name: cleanName });
        else if (type === 'CAMPUS') await updateCampus(itemToEdit.id, { name: cleanName });
        else if (type === 'LIBRARY') await updateLibrary(itemToEdit.id, { name: cleanName, address: address.trim() });
        else if (type === 'DEPARTMENT') await updateDepartment(itemToEdit.id, { name: cleanName });
      } else {
        if (type === 'INSTITUTION') await createInstitution({ name: cleanName });
        else if (type === 'CAMPUS') {
          if (!parentId) throw new Error('Thiếu ID Cơ quan.');
          await createCampus({ institutionId: parentId, name: cleanName });
        } else if (type === 'LIBRARY') {
          if (!parentId) throw new Error('Thiếu ID Cơ sở.');
          await createLibrary({ campusId: parentId, name: cleanName, address: address.trim() });
        } else if (type === 'DEPARTMENT') {
          if (!parentId) throw new Error('Thiếu ID Thư viện.');
          await createDepartment({ libraryId: parentId, name: cleanName });
        }
      }
      onSuccess();
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể lưu đơn vị tổ chức.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="org-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-xs p-4 sm:p-6"
    >
      <div className="relative w-full max-w-md rounded-2xl bg-white p-6 sm:p-8 shadow-2xl">
        <button
          type="button"
          onClick={onClose}
          aria-label="Đóng"
          className="absolute right-4 top-4 rounded-full p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
        >
          ✕
        </button>

        <h2 id="org-modal-title" className="text-xl font-bold text-slate-900">
          {itemToEdit ? `Chỉnh sửa ${TYPE_NAMES[type]}` : `Thêm ${TYPE_NAMES[type]} Mới`}
        </h2>

        {error && (
          <div className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="org-name-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
              Tên {TYPE_NAMES[type]} *
            </label>
            <input
              id="org-name-input"
              type="text"
              required
              maxLength={255}
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={`Nhập tên ${TYPE_NAMES[type].toLowerCase()}`}
              className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
            />
          </div>

          {type === 'LIBRARY' && (
            <div>
              <label htmlFor="org-address-input" className="block text-xs font-bold uppercase tracking-wider text-slate-700 mb-1">
                Địa chỉ thư viện
              </label>
              <input
                id="org-address-input"
                type="text"
                maxLength={255}
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Ví dụ: 176 Trần Phú, TP. Huế"
                className="w-full rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 focus:border-blue-900 focus:outline-hidden focus:ring-1 focus:ring-blue-900"
              />
            </div>
          )}

          <div className="mt-6 flex justify-end gap-3 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-blue-900 px-5 py-2 text-xs font-semibold text-white hover:bg-blue-800 disabled:opacity-50 transition-colors shadow-xs"
            >
              {isSubmitting ? 'Đang lưu…' : itemToEdit ? 'Lưu thay đổi' : 'Tạo mới'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
