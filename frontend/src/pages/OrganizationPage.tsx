import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type {
  CampusDto,
  DepartmentDto,
  InstitutionDto,
  LibraryDto,
} from '../types/organization';
import {
  deleteCampus,
  deleteDepartment,
  deleteInstitution,
  deleteLibrary,
  fetchCampuses,
  fetchDepartments,
  fetchInstitutions,
  fetchLibraries,
} from '../api/organization';
import { OrgModal, type OrgType } from '../components/organization/OrgModal';
import { AssignUserModal } from '../components/organization/AssignUserModal';

type Tab = 'INSTITUTION' | 'CAMPUS' | 'LIBRARY' | 'DEPARTMENT';

export function OrganizationPage() {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const isAdmin = isAuthenticated && user?.role === 'ADMIN';
  const isLibrarianOrAdmin =
    isAuthenticated && (user?.role === 'LIBRARIAN' || user?.role === 'ADMIN');

  const [activeTab, setActiveTab] = useState<Tab>('INSTITUTION');

  const [institutions, setInstitutions] = useState<InstitutionDto[]>([]);
  const [campuses, setCampuses] = useState<CampusDto[]>([]);
  const [libraries, setLibraries] = useState<LibraryDto[]>([]);
  const [departments, setDepartments] = useState<DepartmentDto[]>([]);

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modal State
  const [modalConfig, setModalConfig] = useState<{
    isOpen: boolean;
    type: OrgType;
    parentId?: number;
    itemToEdit?: { id: number; name: string; address?: string } | null;
  }>({
    isOpen: false,
    type: 'INSTITUTION',
  });

  const [isAssignModalOpen, setIsAssignModalOpen] = useState(false);

  const loadAllOrgData = useCallback((signal?: AbortSignal) => {
    setIsLoading(true);
    setError(null);

    Promise.all([
      fetchInstitutions(0, 100, signal),
      fetchCampuses(undefined, 0, 100, signal),
      fetchLibraries(undefined, 0, 100, signal),
      fetchDepartments(undefined, 0, 100, signal),
    ])
      .then(([instRes, campRes, libRes, deptRes]) => {
        setInstitutions(instRes.content ?? []);
        setCampuses(campRes.content ?? []);
        setLibraries(libRes.content ?? []);
        setDepartments(deptRes.content ?? []);
      })
      .catch((err: unknown) => {
        if (err instanceof Error && err.name === 'AbortError') return;
        setError(err instanceof Error ? err.message : 'Không thể tải dữ liệu cơ cấu tổ chức.');
      })
      .finally(() => setIsLoading(false));
  }, []);

  useEffect(() => {
    if (!isLibrarianOrAdmin) return;
    const controller = new AbortController();
    loadAllOrgData(controller.signal);
    return () => controller.abort();
  }, [isLibrarianOrAdmin, loadAllOrgData]);

  if (isAuthLoading) {
    return <div className="py-20 text-center text-sm text-slate-500">Đang kiểm tra quyền truy cập…</div>;
  }

  if (!isLibrarianOrAdmin) {
    return (
      <section className="max-w-xl py-16 sm:py-24 mx-auto text-center">
        <div className="rounded-2xl border border-rose-200 bg-white p-8 shadow-xs">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-rose-100 text-2xl text-rose-700">
            ⛔
          </div>
          <h2 className="mt-4 text-2xl font-bold text-slate-900">Truy cập bị từ chối</h2>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Chức năng quản trị cơ cấu tổ chức chỉ dành cho Quản trị viên và Thủ thư.
          </p>
          <div className="mt-6">
            <Link
              to="/"
              className="rounded-lg bg-slate-900 px-5 py-2.5 text-xs font-semibold text-white hover:bg-slate-800 transition-colors"
            >
              ← Quay về Trang chủ
            </Link>
          </div>
        </div>
      </section>
    );
  }

  const handleDelete = async (type: OrgType, id: number, name: string) => {
    if (!window.confirm(`Bạn có chắc muốn xóa "${name}"? Không thể xóa nếu còn dữ liệu cấp con.`)) {
      return;
    }
    setError(null);
    setSuccessMsg(null);
    try {
      if (type === 'INSTITUTION') await deleteInstitution(id);
      else if (type === 'CAMPUS') await deleteCampus(id);
      else if (type === 'LIBRARY') await deleteLibrary(id);
      else if (type === 'DEPARTMENT') await deleteDepartment(id);

      setSuccessMsg(`Đã xóa thành công "${name}".`);
      loadAllOrgData();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Không thể xóa đơn vị này (có thể do ràng buộc toàn vẹn 409 Conflict).');
    }
  };

  return (
    <div className="py-8">
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="rounded-md bg-purple-100 px-2 py-0.5 text-xs font-bold text-purple-900">
              Quản trị Cơ cấu (Phase 3)
            </span>
            <p className="text-xs font-bold uppercase tracking-wider text-slate-500">
              Cơ quan · Cơ sở · Thư viện · Khoa
            </p>
          </div>
          <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
            Cơ Cấu Tổ Chức & Phòng Ban
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Quản lý cây phả hệ tổ chức và phân bổ người dùng vào khoa/phòng ban theo quyết định D017.
          </p>
        </div>

        {isAdmin && (
          <div className="flex flex-wrap items-center gap-2">
            <button
              type="button"
              onClick={() => setIsAssignModalOpen(true)}
              className="rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-xs font-bold text-slate-700 hover:bg-slate-50 transition-colors shadow-xs"
            >
              👤 Gán User vào Khoa
            </button>
            <button
              type="button"
              onClick={() => {
                if (activeTab === 'INSTITUTION') {
                  setModalConfig({ isOpen: true, type: 'INSTITUTION', itemToEdit: null });
                } else if (activeTab === 'CAMPUS') {
                  const firstInst = institutions[0];
                  if (!firstInst) {
                    alert('Vui lòng tạo Cơ quan trước khi tạo Cơ sở.');
                    return;
                  }
                  setModalConfig({ isOpen: true, type: 'CAMPUS', parentId: firstInst.id, itemToEdit: null });
                } else if (activeTab === 'LIBRARY') {
                  const firstCampus = campuses[0];
                  if (!firstCampus) {
                    alert('Vui lòng tạo Cơ sở trước khi tạo Thư viện.');
                    return;
                  }
                  setModalConfig({ isOpen: true, type: 'LIBRARY', parentId: firstCampus.id, itemToEdit: null });
                } else if (activeTab === 'DEPARTMENT') {
                  const firstLib = libraries[0];
                  if (!firstLib) {
                    alert('Vui lòng tạo Thư viện trước khi tạo Khoa/Phòng ban.');
                    return;
                  }
                  setModalConfig({ isOpen: true, type: 'DEPARTMENT', parentId: firstLib.id, itemToEdit: null });
                }
              }}
              className="rounded-xl bg-blue-900 px-4 py-2.5 text-xs font-bold text-white hover:bg-blue-800 transition-colors shadow-xs"
            >
              + Thêm đơn vị mới
            </button>
          </div>
        )}
      </div>

      {error && (
        <div className="mb-6 rounded-xl border border-rose-200 bg-rose-50 p-4 text-xs text-rose-700">
          <strong>Lỗi:</strong> {error}
        </div>
      )}

      {successMsg && (
        <div className="mb-6 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-xs font-semibold text-emerald-800">
          ✓ {successMsg}
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-slate-200 flex flex-wrap gap-2 mb-6">
        <button
          type="button"
          onClick={() => setActiveTab('INSTITUTION')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'INSTITUTION'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          1. Cơ quan / Trường ({institutions.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('CAMPUS')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'CAMPUS'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          2. Cơ sở ({campuses.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('LIBRARY')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'LIBRARY'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          3. Thư viện ({libraries.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('DEPARTMENT')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors ${
            activeTab === 'DEPARTMENT'
              ? 'border-blue-900 text-blue-900'
              : 'border-transparent text-slate-600 hover:text-slate-900'
          }`}
        >
          4. Khoa / Phòng ban ({departments.length})
        </button>
      </div>

      {/* Tables Content */}
      <main className="rounded-2xl border border-slate-200 bg-white p-6 shadow-xs">
        {isLoading ? (
          <div className="space-y-3 py-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-10 rounded-lg bg-slate-100 animate-pulse" />
            ))}
          </div>
        ) : (
          <>
            {activeTab === 'INSTITUTION' && (
              <div>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700">
                    Danh sách Cơ quan / Trường học (Cấp 1)
                  </h3>
                </div>
                {institutions.length === 0 ? (
                  <div className="p-8 text-center text-xs text-slate-500">Chưa có cơ quan nào được tạo.</div>
                ) : (
                  <div className="overflow-x-auto rounded-xl border border-slate-200">
                    <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold">
                        <tr>
                          <th className="px-4 py-3">ID</th>
                          <th className="px-4 py-3">Tên Cơ quan / Trường</th>
                          <th className="px-4 py-3">Ngày tạo</th>
                          {isAdmin && <th className="px-4 py-3 text-right">Thao tác</th>}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
                        {institutions.map((item) => (
                          <tr key={item.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-mono font-bold text-slate-900">{item.id}</td>
                            <td className="px-4 py-3 font-semibold text-slate-900">{item.name}</td>
                            <td className="px-4 py-3 text-slate-500">{new Date(item.createdAt).toLocaleDateString('vi-VN')}</td>
                            {isAdmin && (
                              <td className="px-4 py-3 text-right space-x-3">
                                <button
                                  type="button"
                                  onClick={() => setModalConfig({ isOpen: true, type: 'INSTITUTION', itemToEdit: item })}
                                  className="text-xs font-semibold text-blue-700 hover:underline"
                                >
                                  Sửa
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDelete('INSTITUTION', item.id, item.name)}
                                  className="text-xs font-semibold text-rose-600 hover:underline"
                                >
                                  Xóa
                                </button>
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'CAMPUS' && (
              <div>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700">
                    Danh sách Cơ sở (Cấp 2)
                  </h3>
                </div>
                {campuses.length === 0 ? (
                  <div className="p-8 text-center text-xs text-slate-500">Chưa có cơ sở nào.</div>
                ) : (
                  <div className="overflow-x-auto rounded-xl border border-slate-200">
                    <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold">
                        <tr>
                          <th className="px-4 py-3">ID</th>
                          <th className="px-4 py-3">Tên Cơ sở</th>
                          <th className="px-4 py-3">Thuộc Cơ quan</th>
                          {isAdmin && <th className="px-4 py-3 text-right">Thao tác</th>}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
                        {campuses.map((item) => (
                          <tr key={item.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-mono font-bold text-slate-900">{item.id}</td>
                            <td className="px-4 py-3 font-semibold text-slate-900">{item.name}</td>
                            <td className="px-4 py-3 text-slate-500">{item.institutionName}</td>
                            {isAdmin && (
                              <td className="px-4 py-3 text-right space-x-3">
                                <button
                                  type="button"
                                  onClick={() => setModalConfig({ isOpen: true, type: 'CAMPUS', itemToEdit: item })}
                                  className="text-xs font-semibold text-blue-700 hover:underline"
                                >
                                  Sửa
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDelete('CAMPUS', item.id, item.name)}
                                  className="text-xs font-semibold text-rose-600 hover:underline"
                                >
                                  Xóa
                                </button>
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'LIBRARY' && (
              <div>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700">
                    Danh sách Thư viện (Cấp 3)
                  </h3>
                </div>
                {libraries.length === 0 ? (
                  <div className="p-8 text-center text-xs text-slate-500">Chưa có thư viện nào.</div>
                ) : (
                  <div className="overflow-x-auto rounded-xl border border-slate-200">
                    <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold">
                        <tr>
                          <th className="px-4 py-3">ID</th>
                          <th className="px-4 py-3">Tên Thư viện</th>
                          <th className="px-4 py-3">Cơ sở</th>
                          <th className="px-4 py-3">Địa chỉ</th>
                          {isAdmin && <th className="px-4 py-3 text-right">Thao tác</th>}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
                        {libraries.map((item) => (
                          <tr key={item.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-mono font-bold text-slate-900">{item.id}</td>
                            <td className="px-4 py-3 font-semibold text-slate-900">{item.name}</td>
                            <td className="px-4 py-3 text-slate-500">{item.campusName}</td>
                            <td className="px-4 py-3 text-slate-500">{item.address || '—'}</td>
                            {isAdmin && (
                              <td className="px-4 py-3 text-right space-x-3">
                                <button
                                  type="button"
                                  onClick={() => setModalConfig({ isOpen: true, type: 'LIBRARY', itemToEdit: item })}
                                  className="text-xs font-semibold text-blue-700 hover:underline"
                                >
                                  Sửa
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDelete('LIBRARY', item.id, item.name)}
                                  className="text-xs font-semibold text-rose-600 hover:underline"
                                >
                                  Xóa
                                </button>
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}

            {activeTab === 'DEPARTMENT' && (
              <div>
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-slate-700">
                    Danh sách Khoa / Phòng ban (Cấp 4)
                  </h3>
                </div>
                {departments.length === 0 ? (
                  <div className="p-8 text-center text-xs text-slate-500">Chưa có khoa/phòng ban nào.</div>
                ) : (
                  <div className="overflow-x-auto rounded-xl border border-slate-200">
                    <table className="min-w-full divide-y divide-slate-200 text-xs text-left">
                      <thead className="bg-slate-50 text-slate-600 font-semibold">
                        <tr>
                          <th className="px-4 py-3">ID</th>
                          <th className="px-4 py-3">Tên Khoa / Phòng ban</th>
                          <th className="px-4 py-3">Thư viện liên kết</th>
                          {isAdmin && <th className="px-4 py-3 text-right">Thao tác</th>}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 bg-white text-slate-700">
                        {departments.map((item) => (
                          <tr key={item.id} className="hover:bg-slate-50">
                            <td className="px-4 py-3 font-mono font-bold text-slate-900">{item.id}</td>
                            <td className="px-4 py-3 font-semibold text-slate-900">{item.name}</td>
                            <td className="px-4 py-3 text-slate-500">{item.libraryName}</td>
                            {isAdmin && (
                              <td className="px-4 py-3 text-right space-x-3">
                                <button
                                  type="button"
                                  onClick={() => setModalConfig({ isOpen: true, type: 'DEPARTMENT', itemToEdit: item })}
                                  className="text-xs font-semibold text-blue-700 hover:underline"
                                >
                                  Sửa
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDelete('DEPARTMENT', item.id, item.name)}
                                  className="text-xs font-semibold text-rose-600 hover:underline"
                                >
                                  Xóa
                                </button>
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </main>

      {/* Org Modal */}
      <OrgModal
        isOpen={modalConfig.isOpen}
        type={modalConfig.type}
        parentId={modalConfig.parentId}
        itemToEdit={modalConfig.itemToEdit}
        onClose={() => setModalConfig((prev) => ({ ...prev, isOpen: false }))}
        onSuccess={() => loadAllOrgData()}
      />

      {/* Assign User to Department Modal */}
      <AssignUserModal
        isOpen={isAssignModalOpen}
        departments={departments}
        onClose={() => setIsAssignModalOpen(false)}
        onSuccess={(msg) => setSuccessMsg(msg)}
      />
    </div>
  );
}
