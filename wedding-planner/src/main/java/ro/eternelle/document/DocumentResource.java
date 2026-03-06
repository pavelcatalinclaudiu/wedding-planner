package ro.eternelle.document;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/couples")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("COUPLE")
public class DocumentResource {

    @Inject DocumentRepository documentRepository;
    @Inject CoupleRepository coupleRepository;
    @Inject JsonWebToken jwt;

    @ConfigProperty(name = "app.uploads.dir", defaultValue = "uploads")
    String uploadsDir;

    // ── List ────────────────────────────────────────────────────────────────

    @GET
    @Path("/me/documents")
    public Response listDocuments() {
        CoupleProfile couple = getCouple();
        List<DocumentDTO> docs = documentRepository.findByCouple(couple.id)
                .stream().map(DocumentDTO::from).toList();
        return Response.ok(docs).build();
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    @POST
    @Path("/me/documents")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public Response uploadDocument(@RestForm("file") FileUpload file) throws IOException {
        if (file == null) throw new BusinessException("No file provided");

        CoupleProfile couple = getCouple();

        // Ensure upload directory exists
        var uploadPath = Paths.get(uploadsDir);
        Files.createDirectories(uploadPath);

        // Generate unique filename preserving extension
        String original = file.fileName();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.'))
                : "";
        String stored = UUID.randomUUID() + ext;

        Files.copy(file.uploadedFile(), uploadPath.resolve(stored), StandardCopyOption.REPLACE_EXISTING);

        CoupleDocument doc = new CoupleDocument();
        doc.couple = couple;
        doc.name = original;
        doc.filename = stored;
        doc.size = Files.size(uploadPath.resolve(stored));
        doc.uploadedAt = Instant.now();
        documentRepository.persist(doc);

        return Response.status(Response.Status.CREATED).entity(DocumentDTO.from(doc)).build();
    }

    // ── Serve file ───────────────────────────────────────────────────────────

    @GET
    @Path("/documents/file/{filename}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @PermitAll
    public Response serveFile(@PathParam("filename") String filename) throws IOException {
        // Prevent path traversal
        if (filename.contains("..") || filename.contains("/")) {
            return Response.status(400).build();
        }
        var filePath = Paths.get(uploadsDir).resolve(filename);
        if (!Files.exists(filePath)) return Response.status(404).build();

        String type = Files.probeContentType(filePath);
        return Response.ok(filePath.toFile())
                .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                .type(type != null ? type : MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @DELETE
    @Path("/me/documents/{id}")
    @Transactional
    public Response deleteDocument(@PathParam("id") UUID id) throws IOException {
        CoupleDocument doc = documentRepository.findById(id);
        if (doc == null) throw new BusinessException("Document not found");

        // Delete physical file
        var file = Paths.get(uploadsDir).resolve(doc.filename);
        Files.deleteIfExists(file);

        documentRepository.delete(doc);
        return Response.noContent().build();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private CoupleProfile getCouple() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return coupleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));
    }
}
