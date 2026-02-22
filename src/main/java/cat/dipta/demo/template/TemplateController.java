package cat.dipta.demo.template;

import cat.dipta.demo.style.StyleService;
import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@LayoutController
@RequestMapping("templates")
@Slf4j
@RequiredArgsConstructor

public class TemplateController {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE_VALUE = "no-cache, no-store, must-revalidate";

    private final TemplateService templateService;
    private final StyleService styleService;
    private final ObjectMapper objectMapper;


    @GetMapping
    public String list(@PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model,
                       HttpServletResponse response,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "20") int size,
                       @RequestParam(value = "searchName", required = false) String searchName,
                       @RequestParam(value = "searchCode", required = false) String searchCode,
                       @RequestParam(value = "searchUnit", required = false) String searchUnit,
                       @RequestParam(value = "searchUser", required = false) String searchUser) {

        setNoCacheHeaders(response);

        PageRequest pageRequest = PageRequest.of(page, size, pageable.getSort());
        Page<Template> templatePage = templateService.findAll(pageRequest, searchName, searchCode, searchUnit, searchUser);

        log.info("Total templates: {}", templatePage.getTotalElements());

        populateListModel(model, templatePage, searchName, searchCode, searchUnit, searchUser);

        return "taula/templates";
    }

    @GetMapping("/create")
    public String createTemplate(Model model) {
        populateTemplateFormModel(model, "Crear plantilla", null);
        return "taula/createTemplate";
    }

    @GetMapping("/edit/{id}")
    public String editTemplate(@PathVariable Long id, Model model) {
        Template template = templateService.findById(id);
        populateTemplateFormModel(model, "Editar plantilla", template);

        model.addAttribute("plantillaId", id);
        model.addAttribute("allBoxesHtml", buildBoxesHtml(template));
        model.addAttribute("templateStyle", getTemplateStyle(template));

        return "taula/createTemplate";
    }

    @ResponseBody
    @PostMapping("/save2")
    public SimpleResponse<Template> saveTemplate(@RequestBody @Valid Template template) {
        return templateService.saveTemplate(template);
    }

    @ResponseBody
    @PutMapping("/save2")
    public SimpleResponse<Template> updateTemplate(@RequestBody @Valid Template template) {
        return templateService.putTemplate(template);
    }

    @ResponseBody
    @DeleteMapping("/delete/{id}")
    public SimpleResponse<String> deleteTemplate(@PathVariable Long id) {
        try {
            templateService.deleteTemplate(String.valueOf(id));
            return SimpleResponse.of("Plantilla eliminada correctament");
        } catch (Exception e) {
            log.error("Error eliminant la plantilla {}: {}", id, e.getMessage());
            throw new RuntimeException("Error eliminant la plantilla: " + e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/import-variables")
    public SimpleResponse<List<Variable>> importVariables(@RequestParam String modelId) {
        return templateService.importVariablesFromModel(modelId);
    }

    @ResponseBody
    @PostMapping("import")
    public SimpleResponse<Template> importTemplate(@RequestBody TemplateImportDTO templateDTO) {
        log.info("Important plantilla: {}", templateDTO.getName());
        return templateService.importTemplate(templateDTO);
    }

    @ResponseBody
    @GetMapping("export/{id}")
    public SimpleResponse<TemplateImportDTO> exportTemplate(@PathVariable Long id) {
        return templateService.exportTemplate(id);
    }

    @ResponseBody
    @GetMapping("get/{id}")
    public SimpleResponse<Template> getTemplate(@PathVariable Long id) {
        Template template = templateService.findById(id);
        return SimpleResponse.of(template);
    }

    @PostMapping("/store-processed-template")
    public ResponseEntity<Void> storeProcessedTemplate(@RequestBody Template template, HttpSession session) {
        try {
            String json = objectMapper.writeValueAsString(template);
            session.setAttribute("processedTemplate", json);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error storing processed template: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/show/{id}")
    public String showPreview(@PathVariable Long id, Model model) {
        Template template = templateService.findById(id);

        model.addAttribute("title", "Vista prèvia plantilla");
        model.addAttribute("plantillaId", id);
        model.addAttribute("template", template);
        model.addAttribute("typeMap", VariableTypeConstants.getTypeMap());
        model.addAttribute("defaultValues", VariableTypeConstants.getDefaultValues());

        return "taula/preview";
    }

    @ResponseBody
    @PostMapping("/process-template")
    public SimpleResponse<Template> processTemplate(@RequestBody Template template) {
        try {
            return templateService.processTemplateForPreview(template);
        } catch (Exception e) {
            log.error("Error processant template: {}", e.getMessage());
            throw new RuntimeException("Error processant el template: " + e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/validate-html")
    public Map<String, Object> validateTemplateHtml(@RequestBody Template template) {
        return templateService.validateHtmlSimple(template);
    }


    @ResponseBody
    @PostMapping("/getPdfPreview/{id}")
    public ResponseEntity<byte[]> getPdfPreview(@PathVariable Long id, @RequestBody Template template) {
        try {
            byte[] pdfBytes = templateService.generatePdfPreview(id, template);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(template.getName() + ".pdf")
                            .build()
            );

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generant PDF per template {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Mètodes auxiliars

    private void setNoCacheHeaders(HttpServletResponse response) {

        response.setHeader(CACHE_CONTROL_HEADER, NO_CACHE_VALUE);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    private void populateListModel(Model model, Page<Template> templatePage,
                                   String searchName, String searchCode,
                                   String searchUnit, String searchUser) {
        model.addAttribute("pageNumbers", buildPageNumbers(templatePage));
        model.addAttribute("title", "Plantilles");
        model.addAttribute("page", templatePage);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchCode", searchCode);
        model.addAttribute("searchUnit", searchUnit);
        model.addAttribute("searchUser", searchUser);
    }

    private void populateTemplateFormModel(Model model, String title, Template template) {
        //if (id.isEmpty()) {

        model.addAttribute("title", title);
        model.addAttribute("styles", styleService.findAll());
        model.addAttribute("functions", templateService.getFunctions().getItem());
        model.addAttribute("typeMap", VariableTypeConstants.getTypeMap());

        if (template != null) {
            model.addAttribute("template", template);
        }
    }

    private List<Integer> buildPageNumbers(Page<?> page) {
        int totalPages = page.getTotalPages();
        if (totalPages == 0) {
            return Collections.emptyList();
        }

        List<Integer> pageNumbers = new ArrayList<>(totalPages);
        for (int i = 1; i <= totalPages; i++) {
            pageNumbers.add(i);
        }
        return pageNumbers;
    }

    private String buildBoxesHtml(Template template) {
        if (template.getBoxes() == null || template.getBoxes().isEmpty()) {
            return "";
        }

        return template.getBoxes().stream()
                .sorted(Comparator.comparing(Box::getRowPosition)
                        .thenComparing(Box::getColPosition))
                .map(Box::getInnerHtml)
                .filter(Objects::nonNull)
                .reduce("", (a, b) -> a + "\n" + b);
    }

    private Object getTemplateStyle(Template template) {
        if (template.getStyle() != null) {
            return styleService.findById(template.getStyle());
        }
        return null;
    }
}