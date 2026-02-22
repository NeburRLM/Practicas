package cat.dipta.demo.template;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.text.Normalizer;

@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateBackendClient client;


    public List<Template> findAll() {
        ResponseEntity<TemplateResponse> response = client.findAll();
        if (response.getBody() != null) {
            return response.getBody().getTemplates();
        }
        return List.of();
    }

    public Page<Template> findAll(Pageable pageable, String searchName, String searchCode,
                                  String searchUnit, String searchUser) {
        // Obtenir tots els templates de l'API
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
        if (id == null) {
            return null;
        }
        ResponseEntity<Template> response = client.findById(id);
        if (response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException(String.format("Template no funciona amb el ID: /%d", id));
    }

    public SimpleResponse<Template> saveTemplate(Template template) {
        log.debug("Intentant guardar plantilla (payload): {}", template);
        // Configurar headers
        try {
            // Fer el POST al backend
            ResponseEntity<Template> response = client.saveTemplate(template);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al fer POST de plantilla: {}", e.getMessage(), e);
            throw new RuntimeException("Error al guardar la plantilla al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Template> putTemplate(Template template) {
        log.debug("JSON que s'envia al Backend: {}", template);

        try {
            if (template == null || template.getId() == null) {
                throw new IllegalArgumentException("Per fer PUT cal template i template.id");
            }
            ResponseEntity<Template> response = client.putTemplate(template);
            return SimpleResponse.of(response.getBody());

        } catch (RestClientException e) {
            log.error("Error al fer PUT de plantilla: {}", e.getMessage(), e);
            throw new RuntimeException("Error a l'actualitzar la plantilla al backend: " + e.getMessage(), e);
        }
    }


    public void deleteTemplate(String id) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Per fer DELETE cal id");
            }
            Long templateId = Long.valueOf(id);
            ResponseEntity<Void> response = client.deleteTemplate(templateId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("DELETE no satisfactori. Status: " + response.getStatusCode());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El id ha de ser numèric: " + id, e);
        } catch (RestClientException e) {
            throw new RuntimeException("Error fent DELETE de la plantilla: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<List<Variable>> importVariablesFromModel(String modelId) {
        try {
            // Obtenir resposta amb JsonNode per evitar deserialització directa
            ResponseEntity<JsonNode> response = client.importVariablesFromModel(modelId);

            // Convertir manualment els IDs a Long (dades incorrectes del backend)
            List<Variable> variables = new ArrayList<>();
            JsonNode body = response.getBody();

            if (body != null && body.has("variables")) {
                JsonNode variablesNode = body.get("variables");
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
        try {
            ResponseEntity<PlantiFuncsResponse> response = client.getFunctions();

            List<PlantiFunc> functions = response.getBody() != null
                    ? response.getBody().getPlantifuncs()
                    : Collections.emptyList();

            return SimpleResponse.of(functions);
        } catch (RestClientException e) {
            log.error("Error obtenint les funcions: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtenir funcions", e);
        }
    }

    public SimpleResponse<Template> importTemplate(TemplateImportDTO templateDTO) {
        log.info("Intentant importar plantilla");

        try {
            ResponseEntity<Template> response = client.importTemplate(templateDTO);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error al importar la plantilla al backend: {}", e.getMessage(), e);
            throw new RuntimeException("Error a l'importar la plantilla al backend: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<TemplateImportDTO> exportTemplate(Long id) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Per exportar cal id");
            }

            ResponseEntity<TemplateImportDTO> response = client.exportTemplate(id);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error exportant plantilla: {}", e.getMessage(), e);
            throw new RuntimeException("Error a l'exportar la plantilla: " + e.getMessage(), e);
        }
    }

    public SimpleResponse<Template> processTemplateForPreview(Template template) {
        try {
            ResponseEntity<Template> response = client.processTemplateForPreview(template);
            return SimpleResponse.of(response.getBody());
        } catch (RestClientException e) {
            log.error("Error processant template: {}", e.getMessage(), e);
            throw new RuntimeException("Error al processar el template: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> validateHtmlSimple(Template template) {
        List<Map<String, String>> errors = new ArrayList<>();

        if (template.getBoxes() == null || template.getBoxes().isEmpty()) {
            return Map.of("errors", errors, "errorCount", 0);
        }

        // Concatenar HTML
        String content = template.getBoxes().stream()
                .map(Box::getInnerHtml)
                .filter(html -> html != null)
                .map(HtmlUtils::htmlUnescape)
                .map(html -> html
                        .replace("<%", "&lt;%")
                        .replace("%>", "%&gt;"))
                .collect(Collectors.joining("\n"));

        // Parser amb tracking
        Parser parser = Parser.htmlParser().setTrackErrors(500);
        Jsoup.parse(content, "", parser);

        // Processar errors
        String[] lines = content.split("\n");
        for (org.jsoup.parser.ParseError parseError : parser.getErrors()) {
            int[] lineCol = calculateLineColumn(content, parseError.getPosition());
            String contextLine = lineCol[0] <= lines.length ? lines[lineCol[0] - 1] : "";
            String element = extractHtmlElement(contextLine);

            errors.add(Map.of(
                    "type", "error",
                    "message", HtmlUtils.htmlEscape(buildFriendlyMessage(parseError.getErrorMessage(), element)),
                    "location", "Line " + lineCol[0] + ", column " + lineCol[1],
                    "element", element != null ? element : "unknown",
                    "code", HtmlUtils.htmlEscape(contextLine.trim())
            ));
        }

        return Map.of("errors", errors, "errorCount", errors.size());
    }

    private int[] calculateLineColumn(String content, int position) {
        int line = 1, column = 1;
        for (int i = 0; i < position && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    private String extractHtmlElement(String line) {
        if (line == null) return null;
        Matcher matcher = Pattern.compile("<\\s*([a-zA-Z0-9:-]+)").matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String buildFriendlyMessage(String originalMessage, String element) {
        if (originalMessage == null) {
            return "Invalid HTML syntax" + (element != null ? " in <" + element + ">" : "");
        }

        String msg = originalMessage.toLowerCase();
        String elemSuffix = element != null ? " in <" + element + ">" : "";

        if (msg.contains("unexpected end")) return "HTML ends unexpectedly. Possible unclosed tag" + elemSuffix;
        if (msg.contains("unexpected character")) return "Unexpected character found inside HTML" + elemSuffix;
        if (msg.contains("missing")) return "Missing required part of HTML structure" + elemSuffix;

        return originalMessage + (element != null ? " (element <" + element + ">)" : "");
    }

    public byte[] generatePdfPreview(Long id, Template template) {
        try {
            if (id == null) {
                throw new IllegalArgumentException("Per generar el PDF cal id");
            }
            if (template == null || template.getCode() == null || template.getCode().isBlank()) {
                throw new IllegalArgumentException("Per generar el PDF cal template.code");
            }

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("id", id);

            Map<String, Object> variablesMap = new HashMap<>();
            if (template.getVariables() != null) {
                for (Variable variable : template.getVariables()) {
                    variablesMap.put(variable.getName(), variable.getValue());
                }
            }
            requestMap.put("variables", variablesMap);

            ResponseEntity<byte[]> response = client.generatePdfPreview(template, requestMap);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

            throw new RuntimeException("Error generant PDF des de el backend. Status: " + response.getStatusCode());
        } catch (RestClientException e) {
            log.error("Error generant PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error de comunicació amb backend", e);
        }
    }
}