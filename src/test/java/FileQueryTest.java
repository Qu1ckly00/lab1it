import org.example.desktop.model.RemoteFileMetadata;
import org.example.desktop.util.FileQuery;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class FileQueryTest {

    private RemoteFileMetadata m(String name, String uploadedBy) {
        return new RemoteFileMetadata("id-"+name, name, Instant.now(), Instant.now(), uploadedBy, uploadedBy, 1);
    }

    @Test
    public void filter_html_and_png() {
        List<RemoteFileMetadata> src = Arrays.asList(
                m("a.html","u1"), m("b.png","u2"), m("c.js","u3"), m("d.HTML","u4"));
        List<RemoteFileMetadata> html = FileQuery.filterByType(src, FileQuery.TypeFilter.HTML);
        List<String> namesH = html.stream().map(RemoteFileMetadata::getName).collect(Collectors.toList());
        assertTrue(namesH.contains("a.html"));
        assertFalse(namesH.contains("b.png"));
        assertFalse(namesH.contains("c.js"));

        List<RemoteFileMetadata> png = FileQuery.filterByType(src, FileQuery.TypeFilter.PNG);
        List<String> namesP = png.stream().map(RemoteFileMetadata::getName).collect(Collectors.toList());
        assertTrue(namesP.contains("b.png"));
        assertFalse(namesP.contains("a.html"));
    }

    @Test
    public void sortByUploaderAscendingDescending() {
        List<RemoteFileMetadata> src = Arrays.asList(
                m("a.html","zeta"), m("b.png","alpha"), m("c.js","Beta"));
        List<RemoteFileMetadata> asc = FileQuery.sortByUploader(src, true);
        assertEquals("alpha", asc.get(0).getUploadedBy().toLowerCase());
        List<RemoteFileMetadata> desc = FileQuery.sortByUploader(src, false);
        assertEquals("zeta", desc.get(0).getUploadedBy().toLowerCase());
    }
}
