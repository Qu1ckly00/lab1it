package org.example.desktop.impl;

import org.example.desktop.api.RemoteStorageClient;
import org.example.desktop.model.RemoteFileMetadata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory demo backend (kept for tests). For persistence, see DiskRemoteStorage.
 */
public class InMemoryRemoteStorage implements RemoteStorageClient {
    private final Map<String, Entry> files = new ConcurrentHashMap<>();
    private volatile boolean authenticated = false;
    private String currentUser;

    private static class Entry {
        RemoteFileMetadata meta;
        byte[] data;
    }

    @Override
    public boolean login(String username, String password) {
        this.authenticated = username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
        this.currentUser = username;
        return authenticated;
    }

    private void ensureAuth() throws IOException {
        if (!authenticated) throw new IOException("Not authenticated");
    }

    @Override
    public List<RemoteFileMetadata> listFiles() throws IOException {
        ensureAuth();
        return files.values().stream().map(e -> e.meta)
                .sorted(Comparator.comparing(RemoteFileMetadata::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Override
    public RemoteFileMetadata upload(Path localFile, String uploadedBy) throws IOException {
        ensureAuth();
        byte[] data = Files.readAllBytes(localFile);
        String id = UUID.randomUUID().toString();
        String name = localFile.getFileName().toString();
        Instant now = Instant.now();
        RemoteFileMetadata meta = new RemoteFileMetadata(id, name, now, now,
                uploadedBy, uploadedBy, data.length);
        Entry e = new Entry();
        e.meta = meta; e.data = data;
        files.put(id, e);
        return meta;
    }

    @Override
    public RemoteFileMetadata updateContent(String fileId, Path localFile, String editorName) throws IOException {
        ensureAuth();
        Entry e = files.get(fileId);
        if (e == null) throw new FileNotFoundException("Remote file not found: " + fileId);
        byte[] data = Files.readAllBytes(localFile);
        e.data = data;
        e.meta.setModifiedAt(Instant.now());
        e.meta.setEditedBy(editorName);
        e.meta.setSize(data.length);
        return e.meta;
    }

    @Override
    public InputStream download(String fileId) throws IOException {
        ensureAuth();
        Entry e = files.get(fileId);
        if (e == null) throw new FileNotFoundException("Remote file not found: " + fileId);
        return new ByteArrayInputStream(e.data);
    }

    @Override
    public void delete(String fileId) throws IOException {
        ensureAuth();
        files.remove(fileId);
    }

    // Demo seed remains (used only if someone explicitly switches to this impl)
    public void seedDemoFiles() {
        try {
            if (!authenticated) { login("demo","demo"); }
            putDemo("hello.html", "<html><body><h1>Hello</h1></body></html>");
            putDemo("script.js", "console.log('Hi from .js'); function sum(a,b){return a+b;}");
        } catch (IOException ignored) {}
    }

    private void putDemo(String name, String content) throws IOException {
        File tmp = File.createTempFile("seed",".txt");
        try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(content.getBytes("UTF-8")); }
        upload(tmp.toPath(), currentUser != null ? currentUser : "demo");
        tmp.delete();
    }
}
