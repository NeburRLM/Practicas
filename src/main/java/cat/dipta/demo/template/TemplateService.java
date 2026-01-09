package cat.dipta.demo.template;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
@Slf4j
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
     * @param template JSON con la estructura de la plantilla
     * @return
     * @throws RuntimeException si hay un error al guardar
     */
    public SimpleResponse<Template> saveTemplate(Template template) {
        log.info("Intentando guardar plantilla en: {}", BACKEND_URL);
        log.debug("JSON payload: {}", template);
        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        // Crear la petición con el JSON y los headers
        HttpEntity<Template> request = new HttpEntity<>(template, headers);

        try {
            // Hacer el POST al backend
            ResponseEntity<Template> response = restTemplate.postForEntity(
                    BACKEND_URL,
                    request,
                    Template.class
            );
            return SimpleResponse.of(response.getBody());


        } catch (RestClientException e) {
            log.error("Error al hacer POST a {}: {}", BACKEND_URL, e.getMessage(), e);
            throw new RuntimeException("Error al guardar la plantilla en el backend: " + e.getMessage(), e);
        }
    }


    public SimpleResponse<Template> putTemplate(Template template) {
        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        log.debug("JSON que se va a enviar al backend: {}", template);

        // Crear la petición con el JSON y los headers
        HttpEntity<Template> request = new HttpEntity<>(template, headers);

        try {
            Long id = template.getId();

            // Construir URL con ID
            String urlConId = BACKEND_URL + "/" + id;

            // Hacer PUT al backend con la URL correcta
            ResponseEntity<Template> response = restTemplate.exchange(
                    urlConId,
                    HttpMethod.PUT,
                    request,
                    Template.class
            );

            return SimpleResponse.of(response.getBody());

        } catch (RestClientException e) {
            log.error("Error al hacer PUT a {}: {}", BACKEND_URL, e.getMessage(), e);
            throw new RuntimeException("Error al actualizar la plantilla en el backend: " + e.getMessage(), e);
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

    public SimpleResponse<List<Variable>> importVariablesFromModel(String modelId) {
        String url = String.format("http://localhost:8080/plantidoc/rest/variables/import?modelId=%s", modelId);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            // 1. Obtener respuesta como JsonNode para evitar deserialización directa
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            // 2. Convertir manualmente los IDs a Long
            List<Variable> variables = new ArrayList<>();
            if (response.getBody() != null && response.getBody().has("variables")) {
                JsonNode variablesNode = response.getBody().get("variables");
                for (JsonNode varNode : variablesNode) {
                    Variable variable = new Variable();
                    variable.setId(null);
                    variable.setName(varNode.get("name").asText());
                    variable.setType(varNode.get("type").asInt());
                    variables.add(variable);
                }
            }

            return SimpleResponse.of(variables);

        } catch (RestClientException e) {
            log.error("Error importing variables from model {}: {}", modelId, e.getMessage());
            throw new RuntimeException("Error al importar variables del modelo", e);
        }
    }

    public SimpleResponse<List<PlantiFunc>> getFunctions() {
        String url = "http://localhost:8080/plantidoc/rest/plantifuncs";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<PlantiFuncsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, PlantiFuncsResponse.class);

            List<PlantiFunc> functions = response.getBody() != null ?
                    response.getBody().getPlantifuncs() : Collections.emptyList();

            return SimpleResponse.of(functions);
        } catch (RestClientException e) {
            log.error("Error getting functions: {}", e.getMessage());
            throw new RuntimeException("Error al obtener funciones", e);
        }
    }

    // Añadir este método en TemplateService.java después del método saveTemplate()
    /**
     * Importa una plantilla en el backend enviando un POST a /rest/template/import
     *
     * @param templateDTO JSON con la estructura de la plantilla
     * @return SimpleResponse con la plantilla importada
     * @throws RuntimeException si hay un error al importar
     */
    public SimpleResponse<Template> importTemplate(TemplateImportDTO templateDTO) {
        String importUrl = BACKEND_URL.replace("/rest/template", "/rest/template/import");
        log.info("Intentando importar plantilla en: {}", importUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<TemplateImportDTO> request = new HttpEntity<>(templateDTO, headers);

        try {
            ResponseEntity<Template> response = restTemplate.postForEntity(
                    importUrl,
                    request,
                    Template.class
            );
            return SimpleResponse.of(response.getBody());

        } catch (RestClientException e) {
            log.error("Error al hacer POST a {}: {}", importUrl, e.getMessage(), e);
            throw new RuntimeException("Error al importar la plantilla en el backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<TemplateImportDTO> exportTemplate(Long id) {
        String url = BACKEND_URL + id + "/export";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<?> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<TemplateImportDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, TemplateImportDTO.class);

            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error exportando plantilla: {}", e.getMessage());
            throw new RuntimeException("Error al exportar la plantilla: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Template> processTemplateForPreview(Template template) {
        String url = BACKEND_URL.replace("/rest/template", "/rest/template/processTemplateForPreview");
        log.info("Procesant template per preview: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-DIPTA-App-Id", "plantilles-web");
        headers.set("X-DIPTA-Remote-User-Id", "rlopezm");

        HttpEntity<Template> request = new HttpEntity<>(template, headers);


        try {
            ResponseEntity<Template> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Template.class
            );


            System.out.println("Template procesat per preview: " + response.getBody());

            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error processant template: {}", e.getMessage());
            throw new RuntimeException("Error al processar el template: " + e.getMessage());
        }
    }

    public byte[] generatePdfPreview(Long id, Template template) {
        try {
            // URL del backend para generar PDF
            //String url = BACKEND_URL.replace("/rest/template", "/rest/templates/"+ template.getCode() + "/apply");
            String url = "http://localhost:8080/plantidoc/rest/templates/MFIDTA01/apply";

            log.info("Generant PDF: {}", url);

            // Crear el mapa principal con id y variables
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("id", id);

            // Crear mapa de variables con nombre: valor
            Map<String, Object> variablesMap = new HashMap<>();
            for (Variable variable : template.getVariables()) {
                variablesMap.put(variable.getName(), variable.getValue());
            }
            requestMap.put("variables", variablesMap);


            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            //headers.set("X-DIPTA-App-Id", "plantilles-web");
            //headers.set("X-DIPTA-Remote-User-Id", "rlopezm");
            headers.set("Accept", "application/pdf");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );


            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("Error generando PDF desde el backend");
        } catch (RestClientException e) {
            log.error("Error generant PDF: {}", e.getMessage());
            throw new RuntimeException("Error de comunicación con el backend");
        }
    }
}