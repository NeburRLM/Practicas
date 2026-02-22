package cat.dipta.demo.style;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

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
@RequiredArgsConstructor
public class StyleService {

    private final StyleBackendClient client;


    public List<Style> findAll() {
        ResponseEntity<StyleResponse> response = client.findAll();
        if (response.getBody() != null) {
            return response.getBody().getStyles();
        }
        return List.of();
    }

    public Style findById(Long styleId) {
        if (styleId == null) {
            return null;
        }
        ResponseEntity<Style> response = client.findById(styleId);
        if (response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException(String.format("Style no funciona amb el ID: /%d", styleId));
    }

    public Page<Style> findAll(Pageable pageable, String searchCode, String searchName, String searchEnsLocal) {
        // Obtenir tots els estils de l'API
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
        log.debug("Intentant guardar l'estil (payload): {}", style);

        try {
            ResponseEntity<Style> response = client.saveStyle(style);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al fer POST de Style: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar l'estil al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Style> updateStyle(Style style) {
        log.debug("JSON que s'envia al Backend (Style): {}", style);

        try {
            if (style == null || style.getId() == null) {
                throw new IllegalArgumentException("Per fer PUT cal style i style.id");
            }

            ResponseEntity<Style> response = client.updateStyle(style);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al fer PUT de Style: {}", e.getMessage(), e);
            throw new RuntimeException("Error a l'actualitzar l'estil al backend: " + e.getMessage(), e);
        }
    }

    public void deleteStyle(String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Per fer DELETE cal id");
            }

            Long styleId = Long.valueOf(id);

            ResponseEntity<Void> response = client.deleteStyle(styleId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("DELETE no satisfactori. Status: " + response.getStatusCode());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id ha de ser numèric: " + id, e);
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de l'estil: " + e.getMessage(), e);
        }
    }

    public List<EnsLocal> findAllEnsLocal() {
        try {
            ResponseEntity<EnsLocalResponse> response = client.findAllEnsLocal();

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            List<EnsLocal> ensLocals = response.getBody().getEnsLocals();
            return ensLocals != null ? ensLocals : List.of();
        } catch (RestClientException e) {
            log.error("Error obtenint ensLocals: {}", e.getMessage(), e);
            throw new RuntimeException("Error obtenint ensLocals: " + e.getMessage(), e);
        }
    }

    public String getStyleCss(Long styleId) {
        if (styleId == null) {
            return "";
        }

        try {
            ResponseEntity<String> response = client.getStyleCss(styleId);
            return response.getBody() != null ? response.getBody() : "";
        } catch (RestClientException e) {
            log.error("Error obtenint el CSS de l'estil {}: {}", styleId, e.getMessage(), e);
            return "";
        }
    }
}
