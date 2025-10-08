package org.example.desktop.impl;

import org.example.desktop.api.RemoteStorageClient;
import org.example.desktop.model.RemoteFileMetadata;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Persistent storage on disk. Each file lives in a folder: <root>/<id>/{meta.properties,<name>}
 */
public class DiskRemoteStorage implements RemoteStorageClient {
    private final Path root;
    private volatile boolean authenticated = false;
    private String currentUser;

    private static final String META_FILE = "meta.properties";

    public DiskRemoteStorage(Path root) throws IOException {
        this.root = root;
        Files.createDirectories(root);
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
        if (!Files.exists(root)) return Collections.emptyList();
        try {
            return Files.list(root)
                    .filter(Files::isDirectory)
                    .map(this::readMetaSafe)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(RemoteFileMetadata::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw e;
        }
    }

    private RemoteFileMetadata readMetaSafe(Path dir) {
        try { return readMeta(dir); } catch (Exception e) { return null; }
    }

    @Override
    public RemoteFileMetadata upload(Path localFile, String uploadedBy) throws IOException {
        ensureAuth();
        String id = UUID.randomUUID().toString();
        Path dir = root.resolve(id);
        Files.createDirectories(dir);
        String name = localFile.getFileName().toString();
        Path dst = dir.resolve(name);
        Files.copy(localFile, dst, REPLACE_EXISTING);
        long size = Files.size(dst);
        Instant now = Instant.now();
        RemoteFileMetadata meta = new RemoteFileMetadata(id, name, now, now, uploadedBy, uploadedBy, size);
        writeMeta(dir, meta);
        return meta;
    }

    @Override
    public RemoteFileMetadata updateContent(String fileId, Path localFile, String editorName) throws IOException {
        ensureAuth();
        Path dir = idDir(fileId);
        RemoteFileMetadata meta = readMeta(dir);
        Path dst = dir.resolve(meta.getName());
        Files.copy(localFile, dst, REPLACE_EXISTING);
        long size = Files.size(dst);
        meta.setModifiedAt(Instant.now());
        meta.setEditedBy(editorName);
        meta.setSize(size);
        writeMeta(dir, meta);
        return meta;
    }

    @Override
    public InputStream download(String fileId) throws IOException {
        ensureAuth();
        Path dir = idDir(fileId);
        RemoteFileMetadata meta = readMeta(dir);
        Path p = dir.resolve(meta.getName());
        return Files.newInputStream(p);
    }

    @Override
    public void delete(String fileId) throws IOException {
        ensureAuth();
        Path dir = idDir(fileId);
        if (!Files.exists(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) Files.deleteIfExists(p);
        }
        Files.deleteIfExists(dir);
    }

    private Path idDir(String id) throws IOException {
        Path d = root.resolve(id);
        if (!Files.isDirectory(d)) throw new FileNotFoundException("No such id: " + id);
        return d;
    }

    private void writeMeta(Path dir, RemoteFileMetadata meta) throws IOException {
        Properties props = new Properties();
        props.setProperty("id", meta.getId());
        props.setProperty("name", meta.getName());
        props.setProperty("createdAt", String.valueOf(meta.getCreatedAt().toEpochMilli()));
        props.setProperty("modifiedAt", String.valueOf(meta.getModifiedAt().toEpochMilli()));
        props.setProperty("uploadedBy", meta.getUploadedBy());
        props.setProperty("editedBy", meta.getEditedBy());
        props.setProperty("size", String.valueOf(meta.getSize()));
        try (OutputStream os = Files.newOutputStream(dir.resolve(META_FILE))) {
            props.store(os, "remote-file-metadata");
        }
    }

    private RemoteFileMetadata readMeta(Path dir) throws IOException {
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(dir.resolve(META_FILE))) {
            props.load(is);
        }
        String id = props.getProperty("id");
        String name = props.getProperty("name");
        Instant created = Instant.ofEpochMilli(Long.parseLong(props.getProperty("createdAt")));
        Instant modified = Instant.ofEpochMilli(Long.parseLong(props.getProperty("modifiedAt")));
        String uploadedBy = props.getProperty("uploadedBy");
        String editedBy = props.getProperty("editedBy");
        long size = Long.parseLong(props.getProperty("size"));
        return new RemoteFileMetadata(id, name, created, modified, uploadedBy, editedBy, size);
    }
}
