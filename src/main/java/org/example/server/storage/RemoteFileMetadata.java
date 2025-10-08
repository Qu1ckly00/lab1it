package org.example.server.storage;

import java.time.Instant;

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
        this.id = id; this.name = name; this.createdAt = createdAt; this.modifiedAt = modifiedAt;
        this.uploadedBy = uploadedBy; this.editedBy = editedBy; this.size = size;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getModifiedAt() { return modifiedAt; }
    public String getUploadedBy() { return uploadedBy; }
    public String getEditedBy() { return editedBy; }
    public long getSize() { return size; }
    public void setId(String v) { id=v; } public void setName(String v) { name=v; }
    public void setCreatedAt(Instant v) { createdAt=v; } public void setModifiedAt(Instant v) { modifiedAt=v; }
    public void setUploadedBy(String v) { uploadedBy=v; } public void setEditedBy(String v) { editedBy=v; }
    public void setSize(long v) { size=v; }

    public String getExtensionLower() {
        String n = name==null?"":name; int i = n.lastIndexOf('.');
        return i>=0? n.substring(i+1).toLowerCase():"";
    }
}
