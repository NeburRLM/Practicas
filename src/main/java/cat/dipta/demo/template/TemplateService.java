package cat.dipta.demo.template;

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
import java.util.List;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
public class TemplateService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private static final String BACKEND_URL = "http://localhost:8080/plantidoc/rest/template/";
    public TemplateService(RestTemplate restTemplate,
                           @Value("${external.api.base-url:http://localhost:8080/plantidoc/rest/template}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    public List<Template> findAll() {
        ResponseEntity<TemplateResponse> response = restTemplate.getForEntity(apiUrl, TemplateResponse.class);
        if (response.getBody() != null) {
            return response.getBody().getTemplates();
        }
        return List.of();
    }

    public Page<Template> findAll(Pageable pageable, String searchName, String searchCode,
                                  String searchUnit, String searchUser) {
        // 1. Obtener TODOS los templates de la API
        List<Template> allTemplates = findAll();

        // 2. Filtrar en memoria con normalización de texto
        List<Template> filteredTemplates = allTemplates.stream()
                .filter(t -> searchName == null || searchName.isEmpty() ||
                        (t.getName() != null && normalizeText(t.getName()).contains(normalizeText(searchName))))
                .filter(t -> searchCode == null || searchCode.isEmpty() ||
                        (t.getCode() != null && normalizeText(t.getCode()).contains(normalizeText(searchCode))))
                .filter(t -> searchUnit == null || searchUnit.isEmpty() ||
                        (t.getCreatorUnit() != null && normalizeText(t.getCreatorUnit()).contains(normalizeText(searchUnit))))
                .filter(t -> searchUser == null || searchUser.isEmpty() ||
                        (t.getCreatorUser() != null && normalizeText(t.getCreatorUser()).contains(normalizeText(searchUser))))
                .collect(Collectors.toList());

        // 3. Ordenar según el Pageable
        List<Template> sortedTemplates = sortTemplates(filteredTemplates, pageable.getSort());

        // 4. Paginar
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedTemplates.size());

        List<Template> pageContent = start < sortedTemplates.size() ?
                sortedTemplates.subList(start, end) :
                List.of();

        return new PageImpl<>(pageContent, pageable, sortedTemplates.size());
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")  // Elimina marcas diacríticas (acentos)
                .toLowerCase();
    }

    private List<Template> sortTemplates(List<Template> templates, Sort sort) {
        if (sort.isUnsorted()) {
            return templates;
        }

        Comparator<Template> comparator = null;

        for (Sort.Order order : sort) {
            Comparator<Template> currentComparator = getComparator(order.getProperty());

            if (order.getDirection() == Sort.Direction.DESC) {
                currentComparator = currentComparator.reversed();
            }

            comparator = (comparator == null) ? currentComparator : comparator.thenComparing(currentComparator);
        }

        assert comparator != null;
        return templates.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private Comparator<Template> getComparator(String property) {
        return switch (property) {
            case "name" -> Comparator.comparing(Template::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "code" -> Comparator.comparing(Template::getCode, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "creatorUnit" -> Comparator.comparing(Template::getCreatorUnit, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "creatorUser" -> Comparator.comparing(Template::getCreatorUser, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "lastUpdated" -> Comparator.comparing(Template::getLastUpdated, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(Template::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    public Template findById(Long id) {
        String url = String.format("%s/%d", apiUrl, id);
        ResponseEntity<Template> response = restTemplate.getForEntity(url, Template.class);
        if (response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException(String.format("Template not found with id: /%d", id));
    }

    /**
     * Guarda una plantilla en el backend enviando un POST con el JSON
     *
     * @param templateJson JSON con la estructura de la plantilla
     * @return
     * @throws RuntimeException si hay un error al guardar
     */
    public Template saveTemplate(String templateJson) {

        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        // Crear la petición con el JSON y los headers
        HttpEntity<String> request = new HttpEntity<>(templateJson, headers);

        try {
            // Hacer el POST al backend
            ResponseEntity<Template> response = restTemplate.postForEntity(
                    BACKEND_URL,
                    request,
                    Template.class
            );
            return response.getBody();


        } catch (RestClientException e) {

            throw new RuntimeException("Error al guardar la plantilla en el backend: " + e.getMessage(), e);
        }
    }

    public void deleteTemplate(String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    BACKEND_URL + "/" + id,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de la plantilla: " + e.getMessage(), e);
        }
    }
}