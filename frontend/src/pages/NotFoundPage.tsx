import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <section className="py-16 text-center">
      <p className="text-sm font-semibold text-blue-700">404</p>
      <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">Không tìm thấy trang</h1>
      <p className="mt-2 text-sm text-slate-500">Đường dẫn bạn yêu cầu không tồn tại hoặc đã được thay đổi.</p>
      <Link className="button mt-6 inline-flex" to="/">
        Về trang chủ
      </Link>
    </section>
  );
}
