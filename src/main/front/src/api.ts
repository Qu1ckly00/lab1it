const BASE = import.meta.env.VITE_API ?? '';
let token: string | null = localStorage.getItem('ld_token');

export function setToken(t: string) {
  token = t; localStorage.setItem('ld_token', t);
}

export function logout() {
  token = null; localStorage.removeItem('ld_token');
}

function headers(extra: Record<string,string> = {}) {
  const h: Record<string,string> = { ...extra };
  if (token) h['X-Auth'] = token;
  return h;
}

export async function login(username: string, password: string) {
  const r = await fetch(`${BASE}/api/auth/login`, {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify({username, password})});
  if (!r.ok) throw new Error(await r.text());
  const j = await r.json(); setToken(j.token); return j;
}

export async function listFiles() {
  const r = await fetch(`${BASE}/api/files`, { headers: headers() });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function uploadFile(f: File) {
  const fd = new FormData(); fd.append('file', f);
  const r = await fetch(`${BASE}/api/files`, { method:'POST', headers: headers(), body: fd });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function updateContent(id: string, f: File) {
  const fd = new FormData(); fd.append('file', f);
  const r = await fetch(`${BASE}/api/files/${id}/content`, { method:'PUT', headers: headers(), body: fd });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function deleteFile(id: string) {
  const r = await fetch(`${BASE}/api/files/${id}`, { method:'DELETE', headers: headers() });
  if (!r.ok) throw new Error(await r.text());
}

export function downloadUrl(id: string) { return `${BASE}/api/files/${id}/content`; }

export function connectSSE(onChange: (msg: string)=>void) {
  const ev = new EventSource(`${BASE}/api/events`);
  ev.addEventListener('change', (e: MessageEvent) => onChange(e.data as string));
  return () => ev.close();
}