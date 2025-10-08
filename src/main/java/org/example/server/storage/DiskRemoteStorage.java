package org.example.server.storage;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@Component
public class DiskRemoteStorage {
    private final Path root;
    public static final String META_FILE = "meta.properties";

    public DiskRemoteStorage() throws IOException {
        String dir = System.getProperty("server.dir", System.getProperty("user.home")+"/.light-drive-server");
        this.root = Paths.get(dir);
        Files.createDirectories(root);
    }

    public List<RemoteFileMetadata> list() throws IOException {
        if (!Files.exists(root)) return Collections.emptyList();
        try {
            return Files.list(root)
                    .filter(Files::isDirectory)
                    .map(this::readMetaSafe)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(RemoteFileMetadata::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        } catch (IOException e) { throw e; }
    }

    private RemoteFileMetadata readMetaSafe(Path dir) { try { return readMeta(dir); } catch (Exception e) { return null; } }

    public RemoteFileMetadata upload(Path localFile, String user) throws IOException {
        String id = UUID.randomUUID().toString();
        Path dir = root.resolve(id); Files.createDirectories(dir);
        String name = localFile.getFileName().toString();
        Path dst = dir.resolve(name); Files.copy(localFile, dst, REPLACE_EXISTING);
        long size = Files.size(dst); Instant now = Instant.now();
        RemoteFileMetadata meta = new RemoteFileMetadata(id, name, now, now, user, user, size);
        writeMeta(dir, meta); return meta;
    }

    public RemoteFileMetadata upload(InputStream content, String originalName, String user) throws IOException {
        String id = UUID.randomUUID().toString();
        Path dir = root.resolve(id); Files.createDirectories(dir);
        Path dst = dir.resolve(originalName); Files.copy(content, dst, REPLACE_EXISTING);
        long size = Files.size(dst); Instant now = Instant.now();
        RemoteFileMetadata meta = new RemoteFileMetadata(id, originalName, now, now, user, user, size);
        writeMeta(dir, meta); return meta;
    }

    public RemoteFileMetadata updateContent(String id, InputStream content, String editor) throws IOException {
        Path dir = idDir(id); RemoteFileMetadata meta = readMeta(dir);
        Path dst = dir.resolve(meta.getName()); Files.copy(content, dst, REPLACE_EXISTING);
        long size = Files.size(dst); meta.setModifiedAt(Instant.now()); meta.setEditedBy(editor); meta.setSize(size);
        writeMeta(dir, meta); return meta;
    }

    public InputStream download(String id) throws IOException {
        Path dir = idDir(id); RemoteFileMetadata meta = readMeta(dir);
        Path p = dir.resolve(meta.getName()); return Files.newInputStream(p);
    }

    public void delete(String id) throws IOException {
        Path dir = idDir(id); if (!Files.exists(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) { for (Path p: ds) Files.deleteIfExists(p); }
        Files.deleteIfExists(dir);
    }

    public RemoteFileMetadata findByName(String name) throws IOException {
        for (RemoteFileMetadata m : list()) if (m.getName().equals(name)) return m; return null;
    }

    private Path idDir(String id) throws IOException { Path d = root.resolve(id); if (!Files.isDirectory(d)) throw new FileNotFoundException(id); return d; }

    private void writeMeta(Path dir, RemoteFileMetadata meta) throws IOException {
        Properties props = new Properties();
        props.setProperty("id", meta.getId());
        props.setProperty("name", meta.getName());
        props.setProperty("createdAt", String.valueOf(meta.getCreatedAt().toEpochMilli()));
        props.setProperty("modifiedAt", String.valueOf(meta.getModifiedAt().toEpochMilli()));
        props.setProperty("uploadedBy", meta.getUploadedBy());
        props.setProperty("editedBy", meta.getEditedBy());
        props.setProperty("size", String.valueOf(meta.getSize()));
        try (OutputStream os = Files.newOutputStream(dir.resolve(META_FILE))) { props.store(os, "meta"); }
    }

    private RemoteFileMetadata readMeta(Path dir) throws IOException {
        Properties p = new Properties(); try (InputStream is = Files.newInputStream(dir.resolve(META_FILE))) { p.load(is); }
        String id = p.getProperty("id"), name = p.getProperty("name");
        Instant c = Instant.ofEpochMilli(Long.parseLong(p.getProperty("createdAt")));
        Instant m = Instant.ofEpochMilli(Long.parseLong(p.getProperty("modifiedAt")));
        String up = p.getProperty("uploadedBy"), ed = p.getProperty("editedBy");
        long size = Long.parseLong(p.getProperty("size"));
        return new RemoteFileMetadata(id, name, c, m, up, ed, size);
    }
}