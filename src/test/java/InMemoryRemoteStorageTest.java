import org.example.desktop.impl.InMemoryRemoteStorage;
import org.example.desktop.model.RemoteFileMetadata;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class InMemoryRemoteStorageTest {

    @Test
    public void upload_sets_metadata_and_list() throws Exception {
        InMemoryRemoteStorage s = new InMemoryRemoteStorage();
        assertTrue(s.login("user","pass"));
        File tmp = File.createTempFile("demo",".js");
        try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write("console.log(1);".getBytes("UTF-8")); }
        RemoteFileMetadata m = s.upload(tmp.toPath(), "user");
        assertEquals("user", m.getUploadedBy());
        List<RemoteFileMetadata> list = s.listFiles();
        assertTrue(list.stream().anyMatch(mm -> mm.getId().equals(m.getId())));
        tmp.delete();
    }

    @Test
    public void delete_removes_file() throws Exception {
        InMemoryRemoteStorage s = new InMemoryRemoteStorage();
        s.login("u","p");
        File tmp = File.createTempFile("demo",".png");
        Path p = tmp.toPath();
        RemoteFileMetadata m = s.upload(p, "u");
        s.delete(m.getId());
        assertTrue(s.listFiles().stream().noneMatch(x -> x.getId().equals(m.getId())));
        tmp.delete();
    }
}
