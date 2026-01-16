package cat.dipta.demo.style;

import cat.dipta.demo.template.Template;
import cat.dipta.demo.utils.RequestUtilsService;
import cat.dipta.springutils.httpresponses.SimpleResponse;
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
    private final RequestUtilsService requestUtilsService;

    @Value("${external.api.base-url}")
    private String apiUrl;

    public StyleService(RestTemplate restTemplate, RequestUtilsService requestUtilsService) {
        this.restTemplate = restTemplate;
        this.requestUtilsService = requestUtilsService;
    }

    public List<Style> findAll() {
        String url = apiUrl + "/styles";
        HttpEntity<Void> request = requestUtilsService.getRequest(null);

        ResponseEntity<StyleResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                StyleResponse.class
        );
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
            String url = String.format("%s/%d", apiUrl + "/styles", styleId);
            HttpEntity<Void> request = requestUtilsService.getRequest(null);

            ResponseEntity<Style> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    Style.class
            );
            return response.getBody();
        } catch (Exception e) {
            // Log el error y devuelve null
            return null;
        }
    }

    public Page<Style> findAll(Pageable pageable, String searchCode, String searchName, String searchEnsLocal) {
        // Obtenir tots els estils de la API
        List<Style> allStyles = findAll();

        // Filtrar en memoria
        List<Style> filteredStyles = allStyles.stream()
                .filter(s -> searchCode == null || searchCode.isEmpty() ||
                        (s.getCode() != null && normalizeText(s.getCode()).contains(normalizeText(searchCode))))
                .filter(s -> searchName == null || searchName.isEmpty() ||
                        (s.getName() != null && normalizeText(s.getName()).contains(normalizeText(searchName))))
                .filter(s -> searchEnsLocal == null || searchEnsLocal.isEmpty() ||
                        (s.getEnsLocal() != null && s.getEnsLocal().getNom() != null &&
                                normalizeText(s.getEnsLocal().getNom()).contains(normalizeText(searchEnsLocal))))
                .collect(Collectors.toList());

        // Ordenar segons Pageable
        List<Style> sortedStyles = sortStyles(filteredStyles, pageable.getSort());

        // Paginar
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
        log.info("Intentant guardar l'estil a: {}", apiUrl + "/styles");
        log.debug("JSON payload: {}", style);

        HttpEntity<Style> request = requestUtilsService.getRequest(style);

        try {
            ResponseEntity<Style> response = restTemplate.postForEntity(
                    apiUrl + "/styles",
                    request,
                    Style.class
            );
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al fer POST a {}: {}", apiUrl + "/styles", e.getMessage(), e);
            throw new RuntimeException("Error al guardar l'estil al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Style> updateStyle(Style style) {
        // Crear la petició amb el JSON y els headers
        HttpEntity<Style> request = requestUtilsService.getRequest(style);

        try {
            Long id = style.getId();
            String urlConId = apiUrl + "/styles/" + id;

            ResponseEntity<Style> response = restTemplate.exchange(
                    urlConId,
                    HttpMethod.PUT,
                    request,
                    Style.class
            );

            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al fer PUT a {}: {}", apiUrl + "/styles", e.getMessage(), e);
            throw new RuntimeException("Error a l'actualitzar l'estil al backend: " + e.getMessage(), e);
        }
    }

    public void deleteStyle(String id) {
        // Configurar headers
        HttpEntity<Style> request = requestUtilsService.getRequest(null);

        try {
            restTemplate.exchange(
                    apiUrl + "/styles/" + id,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de l'estil: " + e.getMessage(), e);
        }
    }

    public List<EnsLocal> findAllEnsLocal() {
        String ensLocalsUrl = apiUrl + "/ensLocals";
        HttpEntity<Void> request = requestUtilsService.getRequest(null);

        ResponseEntity<EnsLocalResponse> response = restTemplate.exchange(
                ensLocalsUrl,
                HttpMethod.GET,
                request,
                EnsLocalResponse.class
        );
        if (response.getBody() != null) {
            return response.getBody().getEnsLocals();
        }
        return List.of();
    }

    public String getStyleCss(Long styleId) {
        String url = apiUrl + "/styles/css/" + styleId + ".css";
        HttpEntity<Void> request = requestUtilsService.getRequest(null);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            log.error("Error obtenint el CSS de l'estil {}: {}", styleId, e.getMessage());
            return "";
        }
    }
}
