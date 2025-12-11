package cat.dipta.demo.template;

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
    private final String apiUrl;

    public AttachmentService(RestTemplate restTemplate,
                             @Value("${external.api.base-url:http://localhost:8080/plantidoc/rest}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = baseUrl + "/attachments";
    }

    public List<Attachment> findAll() {
        ResponseEntity<AttachmentResponse> response = restTemplate.getForEntity(apiUrl, AttachmentResponse.class);
        if (response.getBody() != null) {
            return response.getBody().getAttachments();
        }
        return List.of();
    }

    public Page<Attachment> findAll(Pageable pageable, String searchCode, String searchName,
                                    String searchFileName) {
        List<Attachment> allAttachments = findAll();

        List<Attachment> filteredAttachments = allAttachments.stream()
                .filter(a -> searchCode == null || searchCode.isEmpty() ||
                        (a.getCode() != null && normalizeText(a.getCode()).contains(normalizeText(searchCode))))
                .filter(a -> searchName == null || searchName.isEmpty() ||
                        (a.getName() != null && normalizeText(a.getName()).contains(normalizeText(searchName))))
                .filter(a -> searchFileName == null || searchFileName.isEmpty() ||
                        (a.getFileName() != null && normalizeText(a.getFileName()).contains(normalizeText(searchFileName))))
                .collect(Collectors.toList());

        List<Attachment> sortedAttachments = sortAttachments(filteredAttachments, pageable.getSort());

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
            String url = apiUrl + "/" + attachmentId;
            ResponseEntity<Attachment> response = restTemplate.getForEntity(url, Attachment.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public SimpleResponse<Attachment> saveAttachment(String code, String name, MultipartFile file) {
        log.info("Intentando guardar attachment en: {}", apiUrl);

        try {
            // Crear MultiValueMap para multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Añadir los campos básicos
            body.add("code", code);
            body.add("name", name);

            // Extraer y añadir los datos del archivo
            if (file != null && !file.isEmpty()) {
                body.add("fileName", file.getOriginalFilename());
                body.add("fileType", file.getContentType());
                body.add("dataBytes", new ByteArrayResource(file.getBytes()));
            }

            // Configurar headers para multipart
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-DIPTA-App-Id", "plantilles-web");
            headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Attachment> response = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    Attachment.class
            );

            return SimpleResponse.of(response.getBody());
        } catch (Exception e) {
            log.error("Error al hacer POST a {}: {}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("Error al guardar el attachment en el backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Attachment> updateAttachment(Long id, String code, String name, MultipartFile file) {
        try {
            // Crear MultiValueMap para multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Añadir los campos básicos
            body.add("code", code);
            body.add("name", name);

            // Extraer y añadir los datos del archivo solo si se proporciona uno nuevo
            if (file != null && !file.isEmpty()) {
                body.add("fileName", file.getOriginalFilename());
                body.add("fileType", file.getContentType());
                body.add("dataBytes", new ByteArrayResource(file.getBytes()));
            }

            // Configurar headers para multipart
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-DIPTA-App-Id", "plantilles-web");
            headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            String urlConId = apiUrl + "/" + id;

            ResponseEntity<Attachment> response = restTemplate.exchange(
                    urlConId,
                    HttpMethod.PUT,
                    request,
                    Attachment.class
            );

            return SimpleResponse.of(response.getBody());
        } catch (Exception e) {
            log.error("Error al hacer PUT a {}: {}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("Error al actualizar el attachment en el backend: " + e.getMessage(), e);
        }
    }

    public void deleteAttachment(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    apiUrl + "/" + id,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de l'estil: " + e.getMessage(), e);
        }
    }
}
