import React, { useEffect, useMemo, useRef, useState } from 'react'
import { login, listFiles, uploadFile, deleteFile, downloadUrl, connectSSE, updateContent, logout as apiLogout } from '../api'
import type { Meta, Filter } from '../types'

function useSSE(onTick: ()=>void) {
  useEffect(() => {
    const disconnect = connectSSE(() => onTick());
    return () => disconnect();
  }, [onTick]);
}

function formatBytes(n:number){ if(n<1024) return n+" B"; const u=['KB','MB','GB','TB']; let i= -1; do { n/=1024; i++; } while(n>=1024 && i<u.length-1); return n.toFixed(1)+' '+u[i]; }

export default function App(){
  const [user, setUser] = useState<string | null>(localStorage.getItem('ld_token'))
  const [name, setName] = useState('demo');
  const [pass, setPass] = useState('demo');
  const [files, setFiles] = useState<Meta[]>([]);
  const [sel, setSel] = useState<Meta | null>(null);
  const [filter, setFilter] = useState<Filter>('ALL');
  const [sortAsc, setSortAsc] = useState(true);
  const [hiddenCols, setHidden] = useState<{[k:string]:boolean}>({Created:false, Modified:false, UploadedBy:false, EditedBy:false, Size:false});

  const refresh = async()=>{ const list = await listFiles(); setFiles(list); }
  useEffect(()=>{ if(user) refresh(); },[user]);
  useSSE(()=>{ if(user) refresh(); })

  const filtered = useMemo(()=>{
    let a = files.slice();
    if (filter==='HTML') a = a.filter(m=>m.name.toLowerCase().endsWith('.html'))
    if (filter==='PNG') a = a.filter(m=>m.name.toLowerCase().endsWith('.png'))
    a.sort((x,y)=>{
      const ux=x.uploadedBy?.toLowerCase()||''; const uy=y.uploadedBy?.toLowerCase()||'';
      return (sortAsc?1:-1) * ux.localeCompare(uy);
    })
    return a;
  },[files, filter, sortAsc])

  const onUpload = async (e: React.ChangeEvent<HTMLInputElement>)=>{
    const f = e.target.files?.[0]; if(!f) return; await uploadFile(f); await refresh();
  }

  const onDelete = async ()=>{ if(!sel) return; await deleteFile(sel.id); setSel(null); await refresh(); }

  if(!user) return (
    <div style={{maxWidth:380, margin:'10vh auto', fontFamily:'system-ui'}}>
      <h2>Light Drive — Sign in</h2>
      <label>User</label>
      <input value={name} onChange={e=>setName(e.target.value)} style={{width:'100%'}}/>
      <label>Password</label>
      <input type='password' value={pass} onChange={e=>setPass(e.target.value)} style={{width:'100%'}}/>
      <div style={{display:'flex', gap:8, marginTop:8}}>
        <button onClick={async()=>{ await login(name, pass); setUser(name); }}>Sign in</button>
        <button onClick={()=>{ localStorage.removeItem('ld_token'); setUser(null); }}>Clear token</button>
      </div>
    </div>
  )

  return (
    <div style={{display:'grid', gridTemplateRows:'auto 1fr', height:'100vh', fontFamily:'system-ui'}}>
      <Topbar user={user} onLogout={()=>{ apiLogout(); setUser(null); setFiles([]); setSel(null); }} />
      <Toolbar
        filter={filter} setFilter={setFilter}
        sortAsc={sortAsc} setSortAsc={setSortAsc}
        hiddenCols={hiddenCols} setHidden={setHidden}
        onUpload={onUpload}
        selected={sel}
        onDelete={onDelete}
      />
      <div style={{display:'grid', gridTemplateColumns:'1.2fr 1fr', gap:8, padding:8}}>
        <FilesTable data={filtered} hidden={hiddenCols} selected={sel} onSelect={setSel}/>
        <Preview meta={sel}/>
      </div>
      <SyncPanel files={files} onSynced={refresh} />
    </div>
  )
}

function Topbar({user, onLogout}:{user:string, onLogout:()=>void}){
  return (
    <div style={{display:'flex', alignItems:'center', justifyContent:'space-between', padding:'6px 10px', background:'#f7f7f7', borderBottom:'1px solid #ddd'}}>
      <div><b>Light Drive</b></div>
      <div style={{display:'flex', gap:12, alignItems:'center'}}>
        <span>Signed in as <code>{user}</code></span>
        <button onClick={onLogout}>Logout</button>
      </div>
    </div>
  )
}

function Toolbar({filter,setFilter,sortAsc,setSortAsc,hiddenCols,setHidden,onUpload,selected,onDelete}:{
  filter:Filter; setFilter:(f:Filter)=>void;
  sortAsc:boolean; setSortAsc:(b:boolean)=>void;
  hiddenCols:{[k:string]:boolean}; setHidden:(v:any)=>void;
  onUpload:(e:any)=>void; selected:Meta|null; onDelete:()=>void; }){
  return (
    <div style={{display:'flex', gap:8, padding:8, alignItems:'center', borderBottom:'1px solid #ddd'}}>
      <label><b>Filter</b></label>
      <select value={filter} onChange={e=>setFilter(e.target.value as Filter)}>
        <option value='ALL'>All</option>
        <option value='HTML'>.html</option>
        <option value='PNG'>.png</option>
      </select>
      <button onClick={()=>setSortAsc(true)}>Uploader ↑</button>
      <button onClick={()=>setSortAsc(false)}>Uploader ↓</button>
      <input type='file' onChange={onUpload} />
      <button disabled={!selected} onClick={onDelete}>Delete</button>
      <details>
        <summary>Columns</summary>
        <ColumnToggle name='Created' hidden={hiddenCols.Created} setHidden={setHidden} />
        <ColumnToggle name='Modified' hidden={hiddenCols.Modified} setHidden={setHidden} />
        <ColumnToggle name='UploadedBy' hidden={hiddenCols.UploadedBy} setHidden={setHidden} />
        <ColumnToggle name='EditedBy' hidden={hiddenCols.EditedBy} setHidden={setHidden} />
        <ColumnToggle name='Size' hidden={hiddenCols.Size} setHidden={setHidden} />
      </details>
    </div>
  )
}

function ColumnToggle({name, hidden, setHidden}:{name:string, hidden:boolean, setHidden:(v:any)=>void}){
  return (
    <label style={{marginLeft:8, display:'inline-flex', gap:4}}>
      <input type='checkbox' checked={!hidden} onChange={e=>setHidden((h:any)=>({...h,[name]:!e.target.checked}))} />
      {name}
    </label>
  )
}

function FilesTable({data, hidden, selected, onSelect}:{data:Meta[]; hidden:any; selected:Meta|null; onSelect:(m:Meta)=>void}){
  return (
    <table style={{width:'100%', borderCollapse:'collapse'}}>
      <thead>
        <tr>
          <th align='left'>Name</th>
          {!hidden.Created && <th>Created</th>}
          {!hidden.Modified && <th>Modified</th>}
          {!hidden.UploadedBy && <th>Uploaded By</th>}
          {!hidden.EditedBy && <th>Edited By</th>}
          {!hidden.Size && <th>Size</th>}
          <th></th>
        </tr>
      </thead>
      <tbody>
        {data.map(m=> (
          <tr key={m.id} style={{background:selected?.id===m.id?'#eef':'transparent', cursor:'pointer'}} onClick={()=>onSelect(m)}>
            <td>{m.name}</td>
            {!hidden.Created && <td>{new Date(m.createdAt).toLocaleString()}</td>}
            {!hidden.Modified && <td>{new Date(m.modifiedAt).toLocaleString()}</td>}
            {!hidden.UploadedBy && <td>{m.uploadedBy}</td>}
            {!hidden.EditedBy && <td>{m.editedBy}</td>}
            {!hidden.Size && <td>{formatBytes(m.size)}</td>}
            <td><a href={downloadUrl(m.id)} download>Download</a></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function Preview({meta}:{meta:Meta|null}){
  const [text,setText] = useState('');
  useEffect(()=>{ setText(''); },[meta]);
  if(!meta) return <div style={{padding:12, border:'1px solid #eee'}}>No selection</div>
  const ext = meta.name.toLowerCase().split('.').pop();
  if (ext === 'png') return <div style={{padding:12, border:'1px solid #eee'}}><img src={downloadUrl(meta.id)} style={{maxWidth:'100%'}}/></div>
  if (ext === 'js') {
    fetch(downloadUrl(meta.id)).then(r=>r.text()).then(setText);
    return <pre style={{padding:12, border:'1px solid #eee', overflow:'auto', whiteSpace:'pre-wrap'}}>{text}</pre>
  }
  return <div style={{padding:12, border:'1px solid #eee'}}>No preview</div>
}

// ---- Sync (File System Access API) with anti-echo index + two-way delete ----

type IndexEntry = { id: string; serverModifiedAt: number; localModifiedAt: number; size: number };

const IGNORE_NAMES = new Set(['.light-drive-index.json']);
function isIgnored(name: string) {
  const lower = name.toLowerCase();
  return name.startsWith('.') || lower.endsWith('.crswap') || IGNORE_NAMES.has(name);
}

async function readIndex(dir: FileSystemDirectoryHandle): Promise<Record<string, IndexEntry>> {
  try {
    const h = await dir.getFileHandle('.light-drive-index.json');
    const f = await h.getFile();
    const t = await f.text();
    return JSON.parse(t);
  } catch { return {}; }
}

async function writeIndex(dir: FileSystemDirectoryHandle, idx: Record<string, IndexEntry>) {
  const h = await dir.getFileHandle('.light-drive-index.json', { create: true });
  // @ts-ignore
  const w = await h.createWritable();
  await w.write(new Blob([JSON.stringify(idx)], { type: 'application/json' }));
  await w.close();
}

async function* iterateDir(handle: FileSystemDirectoryHandle): AsyncGenerator<FileSystemFileHandle> {
  // @ts-ignore
  for await (const entry of handle.values()) {
    if (entry.kind === 'file') yield entry as FileSystemFileHandle;
  }
}

function SyncPanel({files, onSynced}:{files: Meta[]; onSynced: ()=>void}){
  const [status, setStatus] = useState('No folder');
  const dirRef = useRef<FileSystemDirectoryHandle|null>(null);
  const timerRef = useRef<number|undefined>(undefined);
  const recentPull = useRef<Map<string, number>>(new Map());

  const markPulled = (name:string)=>{ recentPull.current.set(name, Date.now()); };
  const isRecentlyPulled = (name:string)=>{ const t = recentPull.current.get(name); return !!t && Date.now()-t < 10000; };

  // Pull: download remote files, delete locally missing-on-remote
  const pullOnce = async ()=>{
    if (!dirRef.current) return;
    const dir = dirRef.current;
    const idx = await readIndex(dir);

    const remoteNames = new Set<string>(files.map(f=>f.name));

    // 1) Download/refresh remote files
    for (const m of files){
      if (isIgnored(m.name)) continue;
      try {
        const fh = await dir.getFileHandle(m.name, { create: true });
        // @ts-ignore
        const w = await fh.createWritable();
        const r = await fetch(downloadUrl(m.id));
        await r.body!.pipeTo(w);
        const f2 = await fh.getFile();
        idx[m.name] = { id: m.id, serverModifiedAt: new Date(m.modifiedAt).getTime(), localModifiedAt: f2.lastModified, size: f2.size };
        markPulled(m.name);
      } catch (e) { /* ignore single file errors */ }
    }

    // 2) Remove local files that no longer exist on server
    for await (const fh of iterateDir(dir)){
      const name = fh.name;
      if (isIgnored(name)) continue;
      if (!remoteNames.has(name)) {
        try { await dir.removeEntry(name); } catch {}
        delete idx[name];
      }
    }

    await writeIndex(dir, idx);
    setStatus('Synced ' + files.length + ' files');
    onSynced();
  }

  useEffect(()=>{
    const stop = connectSSE(()=>{ pullOnce(); });
    return ()=> stop();
  },[files])

  // Push: upload new/changed local files; delete on server if user deleted locally
  const scanAndPush = async ()=>{
    if (!dirRef.current) return;
    const dir = dirRef.current;
    const idx = await readIndex(dir);

    // Build remote map by name
    let remoteByName: Record<string, Meta> = {};
    for (const m of files) remoteByName[m.name] = m;

    const presentLocal = new Set<string>();

    for await (const fh of iterateDir(dir)){
      const name = fh.name;
      if (isIgnored(name)) continue; // skip temp/system
      presentLocal.add(name);
      if (isRecentlyPulled(name)) continue; // anti-echo window
      const file = await fh.getFile();
      const localLM = file.lastModified;
      const localSize = file.size;
      const entry = idx[name];
      const remote = remoteByName[name];

      // unchanged vs last known local
      if (entry && entry.localModifiedAt === localLM && entry.size === localSize) continue;

      if (remote) {
        const remoteLM = new Date(remote.modifiedAt).getTime();
        if (remoteLM >= localLM) continue; // server newer/equal → leave to pull
        const meta = await updateContent(remote.id, file); // local newer → push update
        idx[name] = { id: meta.id, serverModifiedAt: new Date(meta.modifiedAt).getTime(), localModifiedAt: localLM, size: localSize };
      } else {
        // new local file → upload (server upsert-by-name handles duplicates)
        const meta = await uploadFile(file);
        if (meta) idx[name] = { id: meta.id, serverModifiedAt: new Date(meta.modifiedAt).getTime(), localModifiedAt: localLM, size: localSize };
      }
    }

    // Local deletions → delete on server (only for files known in index)
    for (const [name, entry] of Object.entries(idx)){
      if (isIgnored(name)) continue;
      if (!presentLocal.has(name)) {
        try { await deleteFile(entry.id); } catch {}
        delete idx[name];
      }
    }

    await writeIndex(dir, idx);
  }

  const choose = async ()=>{
    // @ts-ignore
    const dir = await (window as any).showDirectoryPicker();
    dirRef.current = dir; setStatus('Folder connected');
    await pullOnce();
    await scanAndPush();
    timerRef.current = window.setInterval(scanAndPush, 5000);
  }

  return (
    <div style={{position:'fixed', bottom:8, left:8, padding:8, background:'#fff', border:'1px solid #ddd', borderRadius:6}}>
      <b>Sync</b>: {status} <button onClick={choose}>Choose folder</button>
    </div>
  )
}