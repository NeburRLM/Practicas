package cat.dipta.demo.template;

import cat.dipta.demo.style.Style;
import cat.dipta.demo.style.StyleService;
import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
import cat.dipta.starters.layout.toasts.Toast;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@LayoutController
@RequestMapping("templates")
@Slf4j
public class TemplateController {

    private final TemplateService templateService;
    private final StyleService styleService;
    private final ObjectMapper objectMapper;

    public TemplateController(TemplateService templateService, StyleService styleService, ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.styleService = styleService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(@PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model,
                       HttpServletResponse response,
                       @RequestParam("page") Optional<Integer> page,
                       @RequestParam("size") Optional<Integer> size,
                       @RequestParam(value = "searchName", required = false) String searchName,
                       @RequestParam(value = "searchCode", required = false) String searchCode,
                       @RequestParam(value = "searchUnit", required = false) String searchUnit,
                       @RequestParam(value = "searchUser", required = false) String searchUser) {

        // Prevenir caché del navegador
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        int currentPage = page.orElse(0);
        int pageSize = size.orElse(20);


        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, pageable.getSort());

        Page<Template> llistatTemplate = templateService.findAll(pageRequest, searchName, searchCode, searchUnit, searchUser);

        log.info("Total templates: {}", llistatTemplate.getTotalElements());

        model.addAttribute("pageNumbers", buildPageNumbers(llistatTemplate));
        model.addAttribute("title", "Plantilles");
        model.addAttribute("page", llistatTemplate);

        model.addAttribute("searchName", searchName);
        model.addAttribute("searchCode", searchCode);
        model.addAttribute("searchUnit", searchUnit);
        model.addAttribute("searchUser", searchUser);

        return "taula/templates";
    }

    private List<Integer> buildPageNumbers(Page<?> page) {
        int totalPages = page.getTotalPages();
        if (totalPages > 0) {
            return IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


    //template
    @GetMapping("/create")
    public String createPlantilla(Model model) {
        //if (id.isEmpty()) {

            model.addAttribute("title", "Crear nova plantilla");
            model.addAttribute("styles", styleService.findAll());
            SimpleResponse<List<PlantiFunc>> functionsResponse = templateService.getFunctions();
            model.addAttribute("functions", functionsResponse.getItem());



        return "taula/createTemplate";
    }

    @GetMapping("/edit/{id}")
    public String editarPlantilla(@PathVariable Long id, Model model) {

            Template template = templateService.findById(id);

            String allBoxesHtml = "";
            if (template.getBoxes() != null && !template.getBoxes().isEmpty()) {
                allBoxesHtml = template.getBoxes().stream()
                        .sorted(Comparator.comparing(Box::getRowPosition)
                                .thenComparing(Box::getColPosition))
                        .map(Box::getInnerHtml)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("\n"));
            }
            Style templateStyle = null;
            if (template.getStyle() != null) {
                templateStyle = styleService.findById(template.getStyle());
            }
            Map<Integer, String> typeMap = Map.ofEntries(
                    Map.entry(0, "short"),
                    Map.entry(1, "int"),
                    Map.entry(2, "long"),
                    Map.entry(3, "float"),
                    Map.entry(4, "double"),
                    Map.entry(5, "char"),
                    Map.entry(6, "boolean"),
                    Map.entry(7, "String"),
                    Map.entry(8, "List"),
                    Map.entry(9, "Map"),
                    Map.entry(10, "Data"),
                    Map.entry(11, "Html"),
                    Map.entry(12, "taula csv o html")
            );

            SimpleResponse<List<PlantiFunc>> functionsResponse = templateService.getFunctions();
            model.addAttribute("title", "Editar plantilla");
            model.addAttribute("plantillaId", id);
            model.addAttribute("template", template);
            model.addAttribute("typeMap", typeMap);
            model.addAttribute("templateStyle", templateStyle);
            model.addAttribute("functions", functionsResponse.getItem());
            model.addAttribute("allBoxesHtml", allBoxesHtml);
            model.addAttribute("styles", styleService.findAll());


        return "taula/createTemplate";
    }


    @ResponseBody
    @PostMapping("/save2")
    public SimpleResponse<Template> saveTemplate2(@RequestBody @Valid Template template) {
        return templateService.saveTemplate(template);
    }

    @ResponseBody
    @PutMapping("/save2")
    public SimpleResponse<Template> putTemplate2(@RequestBody @Valid Template template) {
        return templateService.putTemplate(template);
    }

    @ResponseBody
    @PostMapping("/delete/{id}")
    public SimpleResponse<String> deleteTemplate(@PathVariable String id) {
        try {
            templateService.deleteTemplate(id);
            return SimpleResponse.of("Plantilla eliminada correctament");
        } catch (Exception e) {
            throw new RuntimeException("Error eliminant la plantilla: " + e.getMessage());
        }
    }


    @ResponseBody
    @GetMapping("/import-variables")
    public SimpleResponse<List<Variable>> importVariables(@RequestParam String modelId) {
        return templateService.importVariablesFromModel(modelId);
    }

    @PostMapping("import")
    public String importTemplate(@RequestParam("templateDTO") String templateDTO,
                                 RedirectAttributes redirectAttributes) {
        try {
            TemplateImportDTO dto = objectMapper.readValue(templateDTO, TemplateImportDTO.class);
            log.info("Importando plantilla: {}", dto.getName());
            templateService.importTemplate(dto);

            Toast toast = Toast.builder()
                    .title("Èxit")
                    .message("Plantilla importada correctament!")
                    .iconClass("fa fa-check")
                    .build();

            Toast.addToastsTo(Arrays.asList(toast), redirectAttributes);
            return "redirect:/template/list";

        } catch (Exception e) {
            Toast errorToast = Toast.builder()
                    .title("Error")
                    .message("Error important la plantilla: " + e.getMessage())
                    .iconClass("fa fa-exclamation-triangle")
                    .build();

            Toast.addToastsTo(Arrays.asList(errorToast), redirectAttributes);
            log.error("Error important plantilla: {}", e.getMessage());
            return "redirect:/template/list";
        }
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @ResponseBody
    @PostMapping("/generate-prova/{id}")
    public SimpleResponse<Template> generateProva(@PathVariable Long id, @RequestBody Template template) {
        try {
            return templateService.processTemplateForPreview(template);
        } catch (Exception e) {
            log.error("Error generant prova: {}", e.getMessage());
            throw new RuntimeException("Error generant la prova: " + e.getMessage());
        }
    }

    @GetMapping("/show/{id}")
    public String showPreview(@PathVariable Long id, Model model) {
        Template template = templateService.findById(id);

        Map<Integer, String> typeMap = Map.ofEntries(
                Map.entry(0, "short"), Map.entry(1, "int"), Map.entry(2, "long"),
                Map.entry(3, "float"), Map.entry(4, "double"), Map.entry(5, "char"),
                Map.entry(6, "boolean"), Map.entry(7, "String"), Map.entry(8, "List"),
                Map.entry(9, "Map"), Map.entry(10, "Data"), Map.entry(11, "Html"),
                Map.entry(12, "csv_html")
        );

        Map<Integer, String> defaultValues = Map.ofEntries(
                Map.entry(0, "0"), Map.entry(1, "0"), Map.entry(2, "0"),
                Map.entry(3, "0.0"), Map.entry(4, "0.0"), Map.entry(5, "a"),
                Map.entry(6, "true"), Map.entry(7, "text de prova"), Map.entry(8, "[]"),
                Map.entry(9, "[:]"), Map.entry(10, "2018-01-01T12:56:42+0200"),
                Map.entry(11, "<p>html de prova</p>"),
                Map.entry(12, "<table><tr><td>csv/html de prova</td></tr></table>")
        );

        model.addAttribute("title", "Vista previa de plantilla");
        model.addAttribute("plantillaId", id);
        model.addAttribute("template", template);
        model.addAttribute("typeMap", typeMap);
        model.addAttribute("defaultValues", defaultValues);
        return "taula/preview";
    }

    @ResponseBody
    @PostMapping("/process-template")
    public SimpleResponse<Template> processTemplate(@RequestBody Template template) {
        try {
            return templateService.processTemplateForPreview(template);
        } catch (Exception e) {
            log.error("Error procesant template: {}", e.getMessage());
            throw new RuntimeException("Error procesant el template: " + e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/getPdfPreview/{id}")
    public ResponseEntity<byte[]> getPdfPreview(@PathVariable Long id, @RequestBody Template template) {
        try {
            byte[] pdfBytes = templateService.generatePdfPreview(id, template);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(template.getName() + ".pdf")
                    .build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generando PDF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


}