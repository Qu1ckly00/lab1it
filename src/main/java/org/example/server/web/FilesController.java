package org.example.server.web;

import org.example.server.storage.DiskRemoteStorage;
import org.example.server.storage.RemoteFileMetadata;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FilesController {
    private final DiskRemoteStorage storage;
    private final ServerSentEvents events;

    public FilesController(DiskRemoteStorage storage, ServerSentEvents events) { this.storage = storage; this.events = events; }

    private String userFrom(@RequestHeader(value="X-Auth", required=false) String token) {
        return token == null ? "anonymous" : token; // token is username
    }

    @GetMapping
    public List<RemoteFileMetadata> list() throws Exception { return storage.list(); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RemoteFileMetadata upload(@RequestPart("file") MultipartFile file, @RequestHeader(value="X-Auth", required=false) String token) throws Exception {
        String user = userFrom(token);
        RemoteFileMetadata existing = storage.findByName(file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            RemoteFileMetadata m;
            if (existing != null) {
                // UPSERT by name -> prevents duplicate rows (echo) when client re-uploads same file name
                m = storage.updateContent(existing.getId(), is, user);
                events.broadcast("updated:" + m.getId());
            } else {
                m = storage.upload(is, file.getOriginalFilename(), user);
                events.broadcast("uploaded:" + m.getId());
            }
            return m;
        }
    }

    @PutMapping(path="/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RemoteFileMetadata update(@PathVariable String id, @RequestPart("file") MultipartFile file, @RequestHeader(value="X-Auth", required=false) String token) throws Exception {
        try (InputStream is = file.getInputStream()) {
            RemoteFileMetadata m = storage.updateContent(id, is, userFrom(token));
            events.broadcast("updated:" + m.getId());
            return m;
        }
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) throws Exception { storage.delete(id); events.broadcast("deleted:"+id); }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> download(@PathVariable String id) throws Exception {
        RemoteFileMetadata meta = storage.list().stream().filter(m -> m.getId().equals(id)).findFirst().orElseThrow(() -> new RuntimeException("Not found"));
        InputStream is = storage.download(id);
        String ext = meta.getExtensionLower();
        MediaType mt = "png".equals(ext) ? MediaType.IMAGE_PNG : MediaType.TEXT_PLAIN;
        String fn = URLEncoder.encode(meta.getName(), StandardCharsets.UTF_8.name());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(mt)
                .body(new InputStreamResource(is));
    }
}