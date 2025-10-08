package org.example.desktop.model;

import java.time.Instant;
import java.util.Objects;

public class RemoteFileMetadata {
    private String id;
    private String name;
    private Instant createdAt;
    private Instant modifiedAt;
    private String uploadedBy;
    private String editedBy;
    private long size;

    public RemoteFileMetadata() {}

    public RemoteFileMetadata(String id, String name, Instant createdAt, Instant modifiedAt,
                              String uploadedBy, String editedBy, long size) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.uploadedBy = uploadedBy;
        this.editedBy = editedBy;
        this.size = size;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getModifiedAt() { return modifiedAt; }
    public String getUploadedBy() { return uploadedBy; }
    public String getEditedBy() { return editedBy; }
    public long getSize() { return size; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setEditedBy(String editedBy) { this.editedBy = editedBy; }
    public void setSize(long size) { this.size = size; }

    public String getExtensionLower() {
        String n = name == null ? "" : name;
        int dot = n.lastIndexOf('.')
                ;        return dot >= 0 ? n.substring(dot + 1).toLowerCase() : "";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteFileMetadata)) return false;
        RemoteFileMetadata that = (RemoteFileMetadata) o;
        return Objects.equals(id, that.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
}