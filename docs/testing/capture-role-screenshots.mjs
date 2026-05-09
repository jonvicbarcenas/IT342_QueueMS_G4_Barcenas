import { chromium } from 'playwright';
import fs from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();
const screenshotDir = path.join(root, 'docs/testing/evidence/screenshots');
await fs.mkdir(screenshotDir, { recursive: true });

async function loginToken(email) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password: 'Gwaps123' })
  });
  const body = await response.text();
  if (!response.ok) throw new Error(`${email} login failed ${response.status}: ${body}`);
  return JSON.parse(body).backendToken;
}

const roles = [
  { role: 'user', email: 'humbamanok1@gmail.com', expected: /My Requests|Request Service|QueueMS/i },
  { role: 'teller', email: 'teller1@gmail.com', expected: /Assigned Requests|Assigned Counter|Registrar Window/i },
  { role: 'admin', email: 'humbamanok2@gmail.com', expected: /Superadmin|Admin|Manage/i },
];

const browser = await chromium.launch({ headless: true });
const results = [];
try {
  for (const item of roles) {
    const token = await loginToken(item.email);
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });
    await page.goto(`http://localhost:5173/auth/callback?token=${encodeURIComponent(token)}`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(3000);
    await page.waitForURL(/dashboard/, { timeout: 15000 }).catch(() => {});
    const body = await page.locator('body').innerText({ timeout: 10000 });
    const passed = item.expected.test(body);
    await fs.writeFile(path.join(screenshotDir, `${item.role}-playwright-dashboard.txt`), body);
    await page.screenshot({ path: path.join(screenshotDir, `${item.role}-dashboard.png`), fullPage: true });
    results.push({ role: item.role, passed, url: page.url(), screenshot: `${item.role}-dashboard.png` });
    await page.close();
  }
} finally {
  await browser.close();
}

await fs.writeFile(path.join(root, 'docs/testing/evidence/logs/playwright-role-screenshots.json'), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results, null, 2));
