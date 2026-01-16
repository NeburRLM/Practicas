package cat.dipta.demo.attachment;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttachmentService {

    private final RestTemplate restTemplate;

    @Value("${external.api.base-url}")
    private String apiUrl;

    public AttachmentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Attachment> findAll() {
        String url = apiUrl + "/attachments";
        ResponseEntity<AttachmentResponse> response = restTemplate.getForEntity(url, AttachmentResponse.class);
        if (response.getBody() != null) {
            return response.getBody().getAttachments();
        }
        return List.of();
    }

    public Page<Attachment> findAll(Pageable pageable, String searchCode, String searchName,
                                    String searchFileName) {
        // Obtenir tots els adjunts de la API
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

        try {
            String url = String.format("%s/%d", apiUrl + "/attachments", attachmentId);
            ResponseEntity<Attachment> response = restTemplate.getForEntity(url, Attachment.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public SimpleResponse<Attachment> saveAttachment(String code, String name, MultipartFile file) {
        log.info("Intentant guardar attachment a: {}", apiUrl + "/attachments");

        try {
            // Crear MultiValueMap per multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Afegir camps
            body.add("code", code);
            body.add("name", name);

            // Extracció i addició de les dades al fitxer
            if (file != null && !file.isEmpty()) {
                body.add("fileName", file.getOriginalFilename());
                body.add("fileType", file.getContentType());
                body.add("dataBytes", new ByteArrayResource(file.getBytes()));
            }

            // Configurar headers per multipart (temporal)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-DIPTA-App-Id", "plantilles-web");
            headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Attachment> response = restTemplate.postForEntity(
                    apiUrl + "/attachments",
                    request,
                    Attachment.class
            );

            return SimpleResponse.of(response.getBody());
        } catch (Exception e) {
            log.error("Error al fer POST a {}: {}", apiUrl + "/attachments", e.getMessage(), e);
            throw new RuntimeException("Error al guardar l'adjunt al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Attachment> updateAttachment(Long id, String code, String name, MultipartFile file) {
        try {
            // Crear MultiValueMap per multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Afegir camps
            body.add("code", code);
            body.add("name", name);

            // Extracció y addició de les dades del arxiu només si es proporciona un de nou (update)
            if (file != null && !file.isEmpty()) {
                body.add("fileName", file.getOriginalFilename());
                body.add("fileType", file.getContentType());
                body.add("dataBytes", new ByteArrayResource(file.getBytes()));
            }

            // Configurar headers per multipart (temporal)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-DIPTA-App-Id", "plantilles-web");
            headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            String urlConId = apiUrl + "/attachments/" + id;

            ResponseEntity<Attachment> response = restTemplate.exchange(
                    urlConId,
                    HttpMethod.PUT,
                    request,
                    Attachment.class
            );

            return SimpleResponse.of(response.getBody());
        } catch (Exception e) {
            log.error("Error al fer PUT a {}: {}", apiUrl + "/attachments", e.getMessage(), e);
            throw new RuntimeException("Error al actualitzar el attachment al backend: " + e.getMessage(), e);
        }
    }

    public void deleteAttachment(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    apiUrl + "/attachments/" + id,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de l'estil: " + e.getMessage(), e);
        }
    }
}
