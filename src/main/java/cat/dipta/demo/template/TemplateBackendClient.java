package cat.dipta.demo.template;

import cat.dipta.demo.url.PlantillesApi;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TemplateBackendClient {
    private final RestTemplate restTemplate;
    private final PlantillesApi plantillesApi;

    public ResponseEntity<TemplateResponse> findAll() {
        return restTemplate.exchange(plantillesApi.templates(), HttpMethod.GET, HttpEntity.EMPTY, TemplateResponse.class);
    }

    public ResponseEntity<Template> findById(Long id) {
        String url = plantillesApi.templatesFindById(id);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Template.class);
    }

    public ResponseEntity<Template> saveTemplate(Template template) {
        HttpEntity<Template> req = new HttpEntity<>(template);
        return restTemplate.exchange(plantillesApi.templates(), HttpMethod.POST, req, Template.class);
    }

    public ResponseEntity<Template> putTemplate(Template template) {
        if (template == null || template.getId() == null) {
            throw new IllegalArgumentException("Per fer PUT cal template i template.id");
        }
        HttpEntity<Template> req = new HttpEntity<>(template);
        String url = plantillesApi.templatesFindById(template.getId());
        return restTemplate.exchange(url, HttpMethod.PUT, req, Template.class);
    }

    public ResponseEntity<Void> deleteTemplate(Long id) {
        String url = plantillesApi.templatesFindById(id);
        return restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    public ResponseEntity<JsonNode> importVariablesFromModel(String modelId) {
        String url = plantillesApi.importVariablesFromModel(modelId);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, JsonNode.class);
    }

    public ResponseEntity<PlantiFuncsResponse> getFunctions() {
        return restTemplate.exchange(plantillesApi.plantiFuncs(), HttpMethod.GET, HttpEntity.EMPTY, PlantiFuncsResponse.class);
    }

    public ResponseEntity<Template> importTemplate(TemplateImportDTO templateDTO) {
        HttpEntity<TemplateImportDTO> req = new HttpEntity<>(templateDTO);
        return restTemplate.exchange(plantillesApi.templateImport(), HttpMethod.POST, req, Template.class);
    }

    public ResponseEntity<TemplateImportDTO> exportTemplate(Long id) {
        String url = plantillesApi.templateExport(id);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, TemplateImportDTO.class);
    }

    public ResponseEntity<Template> processTemplateForPreview(Template template) {
        HttpEntity<Template> req = new HttpEntity<>(template);
        return restTemplate.exchange(plantillesApi.processTemplateForPreview(), HttpMethod.POST, req, Template.class
        );
    }

    public ResponseEntity<byte[]> generatePdfPreview(Template template, Map<String, Object> requestMap) {
        if (template == null || template.getCode() == null || template.getCode().isBlank()) {
            throw new IllegalArgumentException("Per generar PDF cal template.code");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_PDF));

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(requestMap, headers);

        String url = plantillesApi.templateApply(template.getCode());
        return restTemplate.exchange(url, HttpMethod.POST, req, byte[].class);
    }


}
