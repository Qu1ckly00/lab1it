package org.example.desktop.sync;

import org.example.desktop.api.RemoteStorageClient;
import org.example.desktop.model.RemoteFileMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class SyncManager implements Runnable {
    private final RemoteStorageClient client;
    private final Path localDir;
    private final String username;
    private final long remotePollMillis;
    private volatile boolean running = false;

    // avoid echo re-uploads for files we just downloaded
    private final Set<Path> ignoreUploads = Collections.synchronizedSet(new HashSet<>());

    public SyncManager(RemoteStorageClient client, Path localDir, String username) {
        this(client, localDir, username, 3000);
    }

    public SyncManager(RemoteStorageClient client, Path localDir, String username, long remotePollMillis) {
        this.client = client;
        this.localDir = localDir;
        this.username = username;
        this.remotePollMillis = remotePollMillis;
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this, "SyncManager");
        t.setDaemon(true);
        t.start();
    }

    public void stop() { running = false; }

    @Override
    public void run() {
        try { Files.createDirectories(localDir); } catch (IOException ignored) {}
        long lastPoll = 0;
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            localDir.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            // initial full pull
            try { pullRemoteUpdates(); } catch (IOException ignored) {}

            while (running) {
                long now = System.currentTimeMillis();
                if (now - lastPoll >= remotePollMillis) {
                    try { pullRemoteUpdates(); } catch (IOException ignored) {}
                    lastPoll = now;
                }

                WatchKey key = ws.poll(1, TimeUnit.SECONDS);
                if (key != null) {
                    for (WatchEvent<?> e : key.pollEvents()) {
                        if (e.kind() == StandardWatchEventKinds.OVERFLOW) continue;
                        Path rel = (Path) e.context();
                        Path p = localDir.resolve(rel);
                        if (!Files.isRegularFile(p)) continue;
                        if (ignoreUploads.remove(p)) continue; // skip echo
                        try {
                            // if file exists on server by name, update, else upload
                            Map<String, RemoteFileMetadata> remoteByName = new HashMap<>();
                            for (RemoteFileMetadata m : client.listFiles()) remoteByName.put(m.getName(), m);
                            RemoteFileMetadata remote = remoteByName.get(p.getFileName().toString());
                            if (remote == null) client.upload(p, username);
                            else client.updateContent(remote.getId(), p, username);
                        } catch (IOException ignored) {}
                    }
                    key.reset();
                }
            }
        } catch (Exception ignored) {}
    }

    /** Pull-only: download new/updated from remote to local (last-modified wins). */
    private void pullRemoteUpdates() throws IOException {
        List<RemoteFileMetadata> list = client.listFiles();
        for (RemoteFileMetadata m : list) {
            Path target = localDir.resolve(m.getName());
            boolean exists = Files.exists(target);
            Instant lmLocal = exists ? Files.getLastModifiedTime(target).toInstant() : Instant.EPOCH;
            if (!exists || m.getModifiedAt().isAfter(lmLocal)) {
                try (InputStream is = client.download(m.getId())) {
                    ignoreUploads.add(target);
                    Files.copy(is, target, REPLACE_EXISTING);
                    try { Files.setLastModifiedTime(target, FileTime.from(m.getModifiedAt())); } catch (Exception ignored) {}
                }
            }
        }
    }

    /** Manual two-way reconcile: push local newer, then pull remote newer. */
    public void syncNow() throws IOException {
        Map<String, RemoteFileMetadata> remoteByName = new HashMap<>();
        for (RemoteFileMetadata m : client.listFiles()) remoteByName.put(m.getName(), m);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(localDir, Files::isRegularFile)) {
            for (Path p : ds) {
                String name = p.getFileName().toString();
                RemoteFileMetadata remote = remoteByName.get(name);
                Instant lmLocal = Files.getLastModifiedTime(p).toInstant();
                if (remote == null) client.upload(p, username);
                else if (lmLocal.isAfter(remote.getModifiedAt())) client.updateContent(remote.getId(), p, username);
            }
        }
        pullRemoteUpdates();
    }
}