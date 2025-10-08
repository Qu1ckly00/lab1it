import org.example.desktop.impl.DiskRemoteStorage;
import org.example.desktop.model.RemoteFileMetadata;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class DiskRemoteStoragePersistenceTest {

    @Test
    public void persists_across_instances() throws Exception {
        Path serverRoot = Files.createTempDirectory("server-root");
        DiskRemoteStorage s1 = new DiskRemoteStorage(serverRoot);
        assertTrue(s1.login("u","p"));

        File tmp = File.createTempFile("demo",".js");
        try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write("console.log('x');".getBytes("UTF-8")); }
        RemoteFileMetadata uploaded = s1.upload(tmp.toPath(), "u");

        // new instance reading same folder
        DiskRemoteStorage s2 = new DiskRemoteStorage(serverRoot);
        s2.login("u","p");
        List<RemoteFileMetadata> list = s2.listFiles();
        assertTrue(list.stream().anyMatch(m -> m.getId().equals(uploaded.getId())));
    }
}
