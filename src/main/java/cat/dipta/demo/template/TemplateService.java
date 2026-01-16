package cat.dipta.demo.template;

import cat.dipta.demo.utils.RequestUtilsService;
import cat.dipta.springutils.httpresponses.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.*;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
@Slf4j
public class TemplateService {

    private final RestTemplate restTemplate;
    private final RequestUtilsService requestUtilsService;

    @Value("${external.api.base-url}")
    private String apiUrl;

    public TemplateService(RestTemplate restTemplate, RequestUtilsService requestUtilsService) {
        this.restTemplate = restTemplate;
        this.requestUtilsService = requestUtilsService;
    }


    /*

    public RestTemplate setupRestTemplate(String user, String pass) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeOut))
                .setResponseTimeout(Timeout.ofSeconds(timeOut))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);


//        ObjectMapper mapper = new ObjectMapper();
//        mapper.activateDefaultTypingAsProperty(
//                mapper.getPolymorphicTypeValidator(),
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                "@class"
//        );



        RestTemplate restTemplate = new RestTemplateBuilder()
                .basicAuthentication(user, pass)
                .requestFactory(() -> factory)
                .build();
//        ObjectMapper objectMapper = new ObjectMapper()
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//                .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
//
//        // Activar metadata de tipo solo para POJOs
//        objectMapper.activateDefaultTyping(
//                BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter(objectMapper);
//
//        restTemplate.getMessageConverters().add(0, messageConverter);

        return restTemplate;
    }

     */

    public List<Template> findAll() {
        String url = apiUrl + "/template";
        ResponseEntity<TemplateResponse> response = restTemplate.getForEntity(url, TemplateResponse.class);
        if (response.getBody() != null) {
            return response.getBody().getTemplates();
        }
        return List.of();
    }

    public Page<Template> findAll(Pageable pageable, String searchName, String searchCode,
                                  String searchUnit, String searchUser) {
        // Obtenir tots els templates de la API
        List<Template> allTemplates = findAll();

        // Filtrar en memoria
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

        // Ordenar segons Pageable
        List<Template> sortedTemplates = sortTemplates(filteredTemplates, pageable.getSort());

        // Paginar
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
                .replaceAll("\\p{M}", "")  // Elimina accents
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
            case "creatorUnit" ->
                    Comparator.comparing(Template::getCreatorUnit, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "creatorUser" ->
                    Comparator.comparing(Template::getCreatorUser, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "lastUpdated" ->
                    Comparator.comparing(Template::getLastUpdated, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(Template::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    public Template findById(Long id) {
        String url = String.format("%s/%d", apiUrl + "/template", id);
        ResponseEntity<Template> response = restTemplate.getForEntity(url, Template.class);
        if (response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException(String.format("Template no funciona amb el ID: /%d", id));
    }

    public SimpleResponse<Template> saveTemplate(Template template) {
        log.info("Intentant guardar plantilla a: {}", apiUrl + "/template");
        log.debug("JSON payload: {}", template);
        // Configurar headers
        HttpEntity<Template> request = requestUtilsService.getRequest(template);

        try {
            // Fer el POST al backend
            ResponseEntity<Template> response = restTemplate.postForEntity(
                    apiUrl + "/template",
                    request,
                    Template.class
            );
            return SimpleResponse.of(response.getBody());


        } catch (RestClientException e) {
            log.error("Error al fer POST a {}: {}", apiUrl + "/template", e.getMessage(), e);
            throw new RuntimeException("Error al guardar la plantilla al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Template> putTemplate(Template template) {
        log.debug("JSON que s'envia al Baackend: {}", template);

        // Crear la petició amb el JSON y els headers
        HttpEntity<Template> request = requestUtilsService.getRequest(template);

        try {
            Long id = template.getId();

            // Construir URL amb ID
            String urlConId = apiUrl + "/template/" + id;

            // Fer PUT al backend
            ResponseEntity<Template> response = restTemplate.exchange(
                    urlConId,
                    HttpMethod.PUT,
                    request,
                    Template.class
            );

            return SimpleResponse.of(response.getBody());

        } catch (RestClientException e) {
            log.error("Error al fer PUT a {}: {}", apiUrl + "/template", e.getMessage(), e);
            throw new RuntimeException("Error a l'actualitzar la plantilla al backend: " + e.getMessage(), e);
        }
    }


    public void deleteTemplate(String id) {
        // Configurar headers
        HttpEntity<Template> request = requestUtilsService.getRequest(null);

        try {
            restTemplate.exchange(
                    apiUrl + "/template/" + id,
                    HttpMethod.DELETE,
                    request,
                    String.class
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de la plantilla: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<List<Variable>> importVariablesFromModel(String modelId) {
        String url = String.format(apiUrl + "/variables/import?modelId=%s", modelId);

        HttpEntity<Void> request = requestUtilsService.getRequest(null);

        try {
            // Obtenir resposta amb JsonNode per evitar deserialització directa
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    JsonNode.class
            );

            // Convertir manualment els IDs a Long (dades incorrectes del backend)
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
            log.error("Error importing variables del model {}: {}", modelId, e.getMessage());
            throw new RuntimeException("Error al importar variables del model", e);
        }
    }

    public SimpleResponse<List<PlantiFunc>> getFunctions() {
        String url = apiUrl + "/plantifuncs";

        // Configurar headers
        HttpEntity<Template> request = requestUtilsService.getRequest(null);

        try {
            ResponseEntity<PlantiFuncsResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, PlantiFuncsResponse.class);

            List<PlantiFunc> functions = response.getBody() != null ?
                    response.getBody().getPlantifuncs() : Collections.emptyList();

            return SimpleResponse.of(functions);
        } catch (RestClientException e) {
            log.error("Error obtenint les funcions: {}", e.getMessage());
            throw new RuntimeException("Error al obtener funcions", e);
        }
    }

    public SimpleResponse<Template> importTemplate(TemplateImportDTO templateDTO) {
        String importUrl = apiUrl + "/template/import";
        log.info("Intentant importar plantilla a: {}", importUrl);

        // Configurar headers
        HttpEntity<TemplateImportDTO> request = requestUtilsService.getRequest(templateDTO);

        try {
            ResponseEntity<Template> response = restTemplate.postForEntity(
                    importUrl,
                    request,
                    Template.class
            );
            return SimpleResponse.of(response.getBody());

        } catch (RestClientException e) {
            log.error("Error al fer POST a {}: {}", importUrl, e.getMessage(), e);
            throw new RuntimeException("Error a l'importar la plantilla al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<TemplateImportDTO> exportTemplate(Long id) {
        String url = apiUrl + "/template/" + id + "/export";

        // Configurar headers
        HttpEntity<Template> request = requestUtilsService.getRequest(null);

        try {
            ResponseEntity<TemplateImportDTO> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, TemplateImportDTO.class);

            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error exportant plantilla: {}", e.getMessage());
            throw new RuntimeException("Error a l'exportar la plantilla: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Template> processTemplateForPreview(Template template) {
        String url = apiUrl + "/template/processTemplateForPreview";
        log.info("Processant template per preview: {}", url);

        // Configurar headers
        HttpEntity<Template> request = requestUtilsService.getRequest(template);

        try {
            ResponseEntity<Template> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    Template.class
            );

            System.out.println("Template processat per preview: " + response.getBody());

            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error processant template: {}", e.getMessage());
            throw new RuntimeException("Error al processar el template: " + e.getMessage());
        }
    }

    public byte[] generatePdfPreview(Long id, Template template) {
        try {
            // URL del backend per generar PDF
            String url = apiUrl + "/templates/" + template.getCode() + "/apply";
            //String url = "http://localhost:8080/plantidoc/rest/templates/MFIDTA01/apply";

            log.info("Generant PDF: {}", url);

            // Crear el mapa principal amb id y variables
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("id", id);

            // Crear mapa de variables amb nombre: valor
            Map<String, Object> variablesMap = new HashMap<>();
            if (template.getVariables() != null) {
                for (Variable variable : template.getVariables()) {
                    variablesMap.put(variable.getName(), variable.getValue());
                }
            }
            requestMap.put("variables", variablesMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            //headers.set("X-DIPTA-App-Id", "plantilles-web"); AQUESTA NO
            //headers.set("X-DIPTA-Remote-User-Id", "rlopezm"); AQUESTA NO
            headers.set("Accept", "application/pdf");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);
            //--------------------------------------------------------
            //HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);*/
            // Headers amb el mètode genèric
            //HttpEntity<Map<String, Object>> request = requestUtilsService.getRequest(requestMap);

            // Afegir header específic per a la generació del PDF
            //request.getHeaders().set("Accept", "application/pdf");

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("Error generant PDF desde el backend");
        } catch (RestClientException e) {
            log.error("Error generant PDF: {}", e.getMessage());
            throw new RuntimeException("Error de comunicació amb backend");
        }
    }
}