package org.example.desktop.util;

import org.example.desktop.model.RemoteFileMetadata;

import java.util.*;
import java.util.stream.Collectors;

public final class FileQuery {
    private FileQuery() {}

    public enum TypeFilter { ALL, HTML, PNG }

    public static List<RemoteFileMetadata> filterByType(List<RemoteFileMetadata> list, TypeFilter filter) {
        if (filter == null || filter == TypeFilter.ALL) return new ArrayList<>(list);
        String need = filter == TypeFilter.HTML ? "html" : "png";
        return list.stream().filter(m -> need.equals(m.getExtensionLower())).collect(Collectors.toList());
    }

    public static List<RemoteFileMetadata> sortByUploader(List<RemoteFileMetadata> list, boolean ascending) {
        Comparator<RemoteFileMetadata> cmp = Comparator.comparing(
                m -> Optional.ofNullable(m.getUploadedBy()).orElse(""), String.CASE_INSENSITIVE_ORDER);
        if (!ascending) cmp = cmp.reversed();
        return list.stream().sorted(cmp).collect(Collectors.toList());
    }
}
