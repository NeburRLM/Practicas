package cat.dipta.demo.template;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.text.Normalizer;
import java.util.List;

@Service
@Slf4j
public class StyleService {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public StyleService(RestTemplate restTemplate,
                        @Value("${external.api.base-url:http://localhost:8080/plantidoc}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = baseUrl + "/rest/styles";
    }

    public List<Style> findAll() {
        ResponseEntity<StyleResponse> response = restTemplate.getForEntity(apiUrl, StyleResponse.class);
        if (response.getBody() != null) {
            return response.getBody().getStyles();
        }
        return List.of();
    }

    public Style findById(Long styleId) {
        if (styleId == null) {
            return null;
        }

        try {
            String url = apiUrl + "/" + styleId;
            ResponseEntity<Style> response = restTemplate.getForEntity(url, Style.class);
            return response.getBody();
        } catch (Exception e) {
            // Log el error y devuelve null
            return null;
        }
    }

    public Page<Style> findAll(Pageable pageable, String searchCode, String searchName, String searchEnsLocal) {
        // 1. Obtener todos los estilos de la API
        List<Style> allStyles = findAll();

        // 2. Filtrar en memoria con normalización de texto
        List<Style> filteredStyles = allStyles.stream()
                .filter(s -> searchCode == null || searchCode.isEmpty() ||
                        (s.getCode() != null && normalizeText(s.getCode()).contains(normalizeText(searchCode))))
                .filter(s -> searchName == null || searchName.isEmpty() ||
                        (s.getName() != null && normalizeText(s.getName()).contains(normalizeText(searchName))))
                .filter(s -> searchEnsLocal == null || searchEnsLocal.isEmpty() ||
                        (s.getEnsLocal() != null && s.getEnsLocal().getNom() != null &&
                                normalizeText(s.getEnsLocal().getNom()).contains(normalizeText(searchEnsLocal))))
                .collect(Collectors.toList());

        // 3. Ordenar según el Pageable
        List<Style> sortedStyles = sortStyles(filteredStyles, pageable.getSort());

        // 4. Paginar
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedStyles.size());

        List<Style> pageContent = start < sortedStyles.size() ?
                sortedStyles.subList(start, end) :
                List.of();

        return new PageImpl<>(pageContent, pageable, sortedStyles.size());
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private List<Style> sortStyles(List<Style> styles, Sort sort) {
        if (sort.isUnsorted()) {
            return styles;
        }

        Comparator<Style> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<Style> currentComparator = getComparator(order.getProperty());

            if (order.getDirection() == Sort.Direction.DESC) {
                currentComparator = currentComparator.reversed();
            }

            comparator = (comparator == null) ? currentComparator : comparator.thenComparing(currentComparator);
        }

        assert comparator != null;
        return styles.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Comparator<Style> getComparator(String property) {
        return switch (property) {
            case "code" -> Comparator.comparing(s -> s.getCode() != null ? s.getCode() : "");
            case "name" -> Comparator.comparing(s -> s.getName() != null ? s.getName() : "");
            default -> Comparator.comparing(s -> s.getCode() != null ? s.getCode() : "");
        };
    }

    public SimpleResponse<Style> saveStyle(Style style) {
        log.info("Intentando guardar estilo en: {}", apiUrl);
        log.debug("JSON payload: {}", style);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<Style> request = new HttpEntity<>(style, headers);

        try {
            ResponseEntity<Style> response = restTemplate.postForEntity(
                    apiUrl,
                    request,
                    Style.class
            );
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al hacer POST a {}: {}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("Error al guardar el estilo en el backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Style> updateStyle(Style style) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<Style> request = new HttpEntity<>(style, headers);

        try {
            Long id = style.getId();
            String urlConId = apiUrl + "/" + id;

            ResponseEntity<Style> response = restTemplate.exchange(
                    urlConId,
                    HttpMethod.PUT,
                    request,
                    Style.class
            );

            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al hacer PUT a {}: {}", apiUrl, e.getMessage(), e);
            throw new RuntimeException("Error al actualizar el estilo en el backend: " + e.getMessage(), e);
        }
    }

    public void deleteStyle(String id) {
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

    public List<EnsLocal> findAllEnsLocal() {
        String ensLocalsUrl = "http://localhost:8080/plantidoc/rest/ensLocals";
        ResponseEntity<EnsLocalResponse> response = restTemplate.getForEntity(ensLocalsUrl, EnsLocalResponse.class);
        if (response.getBody() != null) {
            return response.getBody().getEnsLocals();
        }
        return List.of();
    }

    public String getStyleCss(Long styleId) {
        String url = apiUrl + "/css/" + styleId + ".css";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error obteniendo CSS del estilo {}: {}", styleId, e.getMessage());
            return "";
        }
    }
}
