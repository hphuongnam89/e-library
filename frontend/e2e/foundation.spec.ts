import { expect, test } from '@playwright/test';

test('live readiness, navigation and deep-link refresh', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Kết nối tri thức');
  await page.getByRole('link', { name: 'Xem trạng thái hệ thống' }).click();
  await expect(page.getByRole('status')).toContainText('Dịch vụ nền tảng sẵn sàng');
  await page.reload();
  await expect(page.getByRole('status')).toContainText('Dịch vụ nền tảng sẵn sàng');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  expect(errors).toEqual([]);
});

test('API outage is visible and retry recovers', async ({ page }) => {
  await page.route('**/api/system/health/readiness', route => route.fulfill({
    status: 503, contentType: 'application/json', body: '{"status":"DOWN"}',
  }));
  await page.goto('/system');
  await expect(page.getByRole('status')).toContainText('Chưa kết nối được dịch vụ');
  await page.unroute('**/api/system/health/readiness');
  await page.getByRole('button', { name: 'Kiểm tra lại' }).click();
  await expect(page.getByRole('status')).toContainText('Dịch vụ nền tảng sẵn sàng');
});

test('unknown page provides a way home', async ({ page }) => {
  await page.goto('/not-a-real-page');
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Không tìm thấy trang');
  await page.getByRole('link', { name: 'Về trang chủ' }).click();
  await expect(page).toHaveURL('/');
});

test('API paths cannot fall through to the SPA and do not leak diagnostics', async ({ request }) => {
  const health = await request.get('/api/system/health/readiness');
  expect(health.status()).toBe(200);
  expect(await health.json()).toEqual({ status: 'UP' });
  const denied = await request.get('/api/admin/users');
  expect(denied.status()).toBe(401);
  expect(denied.headers()['content-type']).toContain('application/problem+json');
  const problem = await denied.json();
  expect(problem.status).toBe(401);
  expect(problem.requestId).toBe(denied.headers()['x-request-id']);
});

test('shows login button when unauthenticated and auth endpoint denies access', async ({ page, request }) => {
  await page.goto('/');
  await expect(page.getByRole('button', { name: 'Đăng nhập Google' })).toBeVisible();

  const me = await request.get('/api/v1/auth/me');
  expect(me.status()).toBe(401);
});

