import { useState, useEffect } from 'react';
import type { FormEvent } from 'react';
import { fetchDocumentGrants, updateDocumentGrants } from '../../api/digital';
import { fetchDepartments, fetchCampuses, fetchInstitutions } from '../../api/organization';
import type { DigitalDocumentDto, DocumentGrantDto, GrantTargetRequest } from '../../types/digital';
import type { DepartmentDto, CampusDto, InstitutionDto } from '../../types/organization';

interface DigitalDocGrantModalProps {
  isOpen: boolean;
  document: DigitalDocumentDto | null;
  onClose: () => void;
  onSuccess: () => void;
}

export function DigitalDocGrantModal({
  isOpen,
  document: doc,
  onClose,
  onSuccess,
}: DigitalDocGrantModalProps) {
  const [grants, setGrants] = useState<DocumentGrantDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // New grant form state
  const [grantType, setGrantType] = useState<'DEPARTMENT' | 'CAMPUS' | 'INSTITUTION' | 'USER'>('DEPARTMENT');
  const [targetId, setTargetId] = useState<string>('');

  const [departments, setDepartments] = useState<DepartmentDto[]>([]);
  const [campuses, setCampuses] = useState<CampusDto[]>([]);
  const [institutions, setInstitutions] = useState<InstitutionDto[]>([]);

  useEffect(() => {
    if (!isOpen || !doc) return;

    setIsLoading(true);
    setError(null);

    Promise.all([
      fetchDocumentGrants(doc.id),
      fetchDepartments(undefined, 0, 100),
      fetchCampuses(undefined, 0, 100),
      fetchInstitutions(0, 100),
    ])
      .then(([grantList, deptPage, campusPage, instPage]) => {
        setGrants(grantList);
        setDepartments(deptPage.content);
        setCampuses(campusPage.content);
        setInstitutions(instPage.content);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Lỗi tải danh sách quyền');
      })
      .finally(() => setIsLoading(false));
  }, [isOpen, doc]);

  if (!isOpen || !doc) return null;

  const handleAddGrant = async (e: FormEvent) => {
    e.preventDefault();
    if (!targetId) {
      setError('Vui lòng chọn hoặc nhập đối tượng được cấp quyền');
      return;
    }

    const newTarget: GrantTargetRequest = {};
    const numericId = Number(targetId);

    if (grantType === 'DEPARTMENT') newTarget.departmentId = numericId;
    else if (grantType === 'CAMPUS') newTarget.campusId = numericId;
    else if (grantType === 'INSTITUTION') newTarget.institutionId = numericId;
    else if (grantType === 'USER') newTarget.userId = numericId;

    // Build complete grants payload
    const payloadGrants: GrantTargetRequest[] = grants.map((g) => ({
      institutionId: g.institutionId,
      campusId: g.campusId,
      departmentId: g.departmentId,
      userId: g.userId,
    }));
    payloadGrants.push(newTarget);

    setIsSubmitting(true);
    setError(null);

    try {
      const updated = await updateDocumentGrants(doc.id, { grants: payloadGrants });
      setGrants(updated);
      setTargetId('');
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Lỗi thêm quyền');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRemoveGrant = async (index: number) => {
    const remaining = grants.filter((_, i) => i !== index);
    const payloadGrants: GrantTargetRequest[] = remaining.map((g) => ({
      institutionId: g.institutionId,
      campusId: g.campusId,
      departmentId: g.departmentId,
      userId: g.userId,
    }));

    setIsSubmitting(true);
    setError(null);

    try {
      const updated = await updateDocumentGrants(doc.id, { grants: payloadGrants });
      setGrants(updated);
      onSuccess();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Lỗi xóa quyền');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="grant-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-xs"
    >
      <div className="relative w-full max-w-lg rounded-2xl bg-white p-6 shadow-2xl transition-all max-h-[90vh] flex flex-col">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4">
          <div>
            <h3 id="grant-modal-title" className="text-lg font-bold text-slate-900">
              Phân quyền Tài liệu Giới hạn
            </h3>
            <p className="text-xs text-slate-500 mt-0.5 truncate max-w-sm">{doc.title}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            <span className="sr-only">Đóng</span>
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {error && (
          <div role="alert" className="mt-4 rounded-lg bg-rose-50 p-3 text-sm text-rose-700">
            {error}
          </div>
        )}

        {/* Current Grants List */}
        <div className="mt-4 flex-1 overflow-y-auto min-h-[120px]">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
            Danh sách đối tượng được cấp quyền ({grants.length})
          </h4>
          {isLoading ? (
            <p className="text-sm text-slate-500 py-4 text-center">Đang tải danh sách quyền...</p>
          ) : grants.length === 0 ? (
            <div className="rounded-lg border border-dashed border-slate-200 p-4 text-center text-sm text-slate-400">
              Chưa có đối tượng nào được cấp quyền. Chỉ Thủ thư/Admin mới xem được tài liệu này.
            </div>
          ) : (
            <ul className="space-y-2">
              {grants.map((g, idx) => {
                let label = '';
                let typeBadge = '';
                if (g.departmentId) {
                  typeBadge = 'Khoa';
                  label = g.departmentName || `Khoa #${g.departmentId}`;
                } else if (g.campusId) {
                  typeBadge = 'Cơ sở';
                  label = g.campusName || `Cơ sở #${g.campusId}`;
                } else if (g.institutionId) {
                  typeBadge = 'Trường';
                  label = g.institutionName || `Trường #${g.institutionId}`;
                } else if (g.userId) {
                  typeBadge = 'Cá nhân';
                  label = g.userFullName ? `${g.userFullName} (${g.userEmail})` : `User #${g.userId}`;
                }

                return (
                  <li
                    key={g.id || idx}
                    className="flex items-center justify-between rounded-lg bg-slate-50 px-3.5 py-2 text-sm"
                  >
                    <div className="flex items-center space-x-2 truncate">
                      <span className="rounded-md bg-blue-100 px-2 py-0.5 text-xs font-semibold text-blue-800">
                        {typeBadge}
                      </span>
                      <span className="font-medium text-slate-800 truncate">{label}</span>
                    </div>
                    <button
                      type="button"
                      disabled={isSubmitting}
                      onClick={() => handleRemoveGrant(idx)}
                      className="ml-2 text-xs text-rose-600 hover:text-rose-800 font-semibold"
                    >
                      Xóa
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* Add New Grant Form */}
        <form onSubmit={handleAddGrant} className="mt-4 border-t border-slate-100 pt-4 space-y-3">
          <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700">
            Thêm quyền truy cập mới
          </h4>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="grant-type-select" className="block text-xs text-slate-600 mb-1">Loại đối tượng</label>
              <select
                id="grant-type-select"
                value={grantType}
                onChange={(e) => {
                  setGrantType(e.target.value as 'DEPARTMENT' | 'CAMPUS' | 'INSTITUTION' | 'USER');
                  setTargetId('');
                }}
                className="w-full rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900"
              >
                <option value="DEPARTMENT">Khoa / Phòng ban</option>
                <option value="CAMPUS">Cơ sở (Campus)</option>
                <option value="INSTITUTION">Toàn trường</option>
                <option value="USER">Cá nhân (User ID)</option>
              </select>
            </div>

            <div>
              <label htmlFor="grant-target-select" className="block text-xs text-slate-600 mb-1">Đối tượng cụ thể</label>
              {grantType === 'DEPARTMENT' && (
                <select
                  id="grant-target-select"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900"
                >
                  <option value="">-- Chọn khoa --</option>
                  {departments.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.name}
                    </option>
                  ))}
                </select>
              )}

              {grantType === 'CAMPUS' && (
                <select
                  id="grant-target-select"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900"
                >
                  <option value="">-- Chọn cơ sở --</option>
                  {campuses.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              )}

              {grantType === 'INSTITUTION' && (
                <select
                  id="grant-target-select"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900"
                >
                  <option value="">-- Chọn trường --</option>
                  {institutions.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.name}
                    </option>
                  ))}
                </select>
              )}

              {grantType === 'USER' && (
                <input
                  id="grant-target-select"
                  type="number"
                  value={targetId}
                  onChange={(e) => setTargetId(e.target.value)}
                  placeholder="Nhập ID người dùng..."
                  className="w-full rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-900"
                />
              )}
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-blue-900 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-800 disabled:opacity-50"
            >
              {isSubmitting ? 'Đang lưu...' : '+ Thêm quyền'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
