package org.example.desktop.api;

import org.example.desktop.model.RemoteFileMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface RemoteStorageClient {
    boolean login(String username, String password) throws IOException;
    List<RemoteFileMetadata> listFiles() throws IOException;
    RemoteFileMetadata upload(Path localFile, String uploadedBy) throws IOException;
    RemoteFileMetadata updateContent(String fileId, Path localFile, String editorName) throws IOException;
    InputStream download(String fileId) throws IOException;
    void delete(String fileId) throws IOException;
}
