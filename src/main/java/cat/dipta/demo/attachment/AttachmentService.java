package cat.dipta.demo.attachment;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentBackendClient client;


    public List<Attachment> findAll() {
        ResponseEntity<AttachmentResponse> response = client.findAll();
        if (response.getBody() != null && response.getBody().getAttachments() != null) {
            return response.getBody().getAttachments();
        }
        return List.of();
    }

    public Page<Attachment> findAll(Pageable pageable, String searchCode, String searchName,
                                    String searchFileName) {
        // Obtenir tots els adjunts de l'API
        List<Attachment> allAttachments = findAll();

        // Filtrar en memoria
        List<Attachment> filteredAttachments = allAttachments.stream()
                .filter(a -> searchCode == null || searchCode.isEmpty() ||
                        (a.getCode() != null && normalizeText(a.getCode()).contains(normalizeText(searchCode))))
                .filter(a -> searchName == null || searchName.isEmpty() ||
                        (a.getName() != null && normalizeText(a.getName()).contains(normalizeText(searchName))))
                .filter(a -> searchFileName == null || searchFileName.isEmpty() ||
                        (a.getFileName() != null && normalizeText(a.getFileName()).contains(normalizeText(searchFileName))))
                .collect(Collectors.toList());

        // Ordenar segons Pageable
        List<Attachment> sortedAttachments = sortAttachments(filteredAttachments, pageable.getSort());

        // Paginar
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedAttachments.size());

        List<Attachment> pageContent = start < sortedAttachments.size() ?
                sortedAttachments.subList(start, end) :
                List.of();

        return new PageImpl<>(pageContent, pageable, sortedAttachments.size());
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private List<Attachment> sortAttachments(List<Attachment> attachments, Sort sort) {
        if (sort.isUnsorted()) {
            return attachments;
        }

        Comparator<Attachment> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<Attachment> currentComparator = getComparator(order.getProperty());

            if (order.getDirection() == Sort.Direction.DESC) {
                currentComparator = currentComparator.reversed();
            }

            comparator = (comparator == null) ? currentComparator : comparator.thenComparing(currentComparator);
        }

        assert comparator != null;
        return attachments.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Comparator<Attachment> getComparator(String property) {
        return switch (property) {
            case "code" -> Comparator.comparing(a -> a.getCode() != null ? a.getCode() : "");
            case "name" -> Comparator.comparing(a -> a.getName() != null ? a.getName() : "");
            case "fileName" -> Comparator.comparing(a -> a.getFileName() != null ? a.getFileName() : "");
            default -> Comparator.comparing(a -> a.getCode() != null ? a.getCode() : "");
        };
    }

    public Attachment findById(Long attachmentId) {
        if (attachmentId == null) {
            return null;
        }
        ResponseEntity<Attachment> response = client.findById(attachmentId);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException(String.format("Attachment no funciona amb el ID: /%d", attachmentId));
    }

    public SimpleResponse<Attachment> saveAttachment(String code, String name, MultipartFile file) {
        log.debug("Intentant guardar attachment (code={}, name={}, fileName={})",
                code, name, file != null ? file.getOriginalFilename() : null);

        try {
            ResponseEntity<Attachment> response = client.saveAttachment(code, name, file);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al fer POST d'Attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar l'adjunt al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Attachment> updateAttachment(Long id, String code, String name, MultipartFile file) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Per fer PUT cal id");
            }

            ResponseEntity<Attachment> response = client.updateAttachment(id, code, name, file);
            return SimpleResponse.of(response.getBody());
        } catch (Exception e) {
            log.error("Error al fer PUT d'Attachment {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error al actualitzar el attachment al backend: " + e.getMessage(), e);
        }
    }

    public void deleteAttachment(String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Per fer DELETE cal id");
            }

            Long attachmentId = Long.valueOf(id);

            ResponseEntity<Void> response = client.deleteAttachment(attachmentId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("DELETE no satisfactori. Status: " + response.getStatusCode());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id ha de ser numèric: " + id, e);
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de l'adjunt: " + e.getMessage(), e);
        }
    }
}
