package ro.eternelle.document;

import java.time.Instant;

public class DocumentDTO {
    public String id;
    public String name;
    public String url;
    public long size;
    public Instant uploadedAt;

    public static DocumentDTO from(CoupleDocument d) {
        DocumentDTO dto = new DocumentDTO();
        dto.id = d.id.toString();
        dto.name = d.name;
        dto.url = "/api/couples/documents/file/" + d.filename;
        dto.size = d.size;
        dto.uploadedAt = d.uploadedAt;
        return dto;
    }
}
