import { Component, type ReactNode } from 'react';
export class ErrorBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  render() {
    if (this.state.failed) return <main className="mx-auto max-w-xl px-6 py-16" role="alert">
      <h1 className="text-2xl font-semibold">Không thể hiển thị trang</h1>
      <p className="my-4">Vui lòng tải lại trang để thử lại.</p>
      <button className="button" onClick={() => window.location.reload()}>Tải lại trang</button>
    </main>;
    return this.props.children;
  }
}
