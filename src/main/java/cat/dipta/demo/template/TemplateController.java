package cat.dipta.demo.template;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@LayoutController
@RequestMapping("template")
@Slf4j
public class TemplateController {

    private final TemplateService templateService;
    private final StyleService styleService;
    private final AttachmentService attachmentService;
    private final ObjectMapper objectMapper;

    public TemplateController(TemplateService templateService, StyleService styleService, AttachmentService attachmentService, ObjectMapper objectMapper) {
        this.templateService = templateService;
        this.styleService = styleService;
        this.attachmentService = attachmentService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "list")
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

        return "taula/list";
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

    @GetMapping("styles")
    public String estils(@PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable,
                         Model model,
                         HttpServletResponse response,
                         @RequestParam(value = "searchName", required = false) String searchName,
                         @RequestParam(value = "searchCode", required = false) String searchCode,
                         @RequestParam(value = "searchEnsLocal", required = false) String searchEnsLocal) {

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Page<Style> llistatStyle = styleService.findAll(pageable, searchCode, searchName, searchEnsLocal);

        model.addAttribute("pageNumbers", buildPageNumbers(llistatStyle));
        model.addAttribute("title", "Estils");
        model.addAttribute("page", llistatStyle);

        model.addAttribute("searchCode", searchCode);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchEnsLocal", searchEnsLocal);

        return "taula/estils";
    }


    @GetMapping("attachments")
    public String adjunts(@PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable,
                          Model model,
                          HttpServletResponse response,
                          @RequestParam(value = "searchName", required = false) String searchName,
                          @RequestParam(value = "searchCode", required = false) String searchCode,
                          @RequestParam(value = "searchFileName", required = false) String searchFileName) {

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Page<Attachment> llistatAttachment = attachmentService.findAll(pageable, searchCode, searchName, searchFileName);

        model.addAttribute("pageNumbers", buildPageNumbers(llistatAttachment));
        model.addAttribute("title", "Adjunts");
        model.addAttribute("page", llistatAttachment);

        model.addAttribute("searchCode", searchCode);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchFileName", searchFileName);

        return "taula/adjunts";
    }
    //template
    @GetMapping(value = {"/create/{id}", "/edit/{id}"})
    public String createPlantilla(@PathVariable Long id, Model model) {
        if (id == 1) {
            model.addAttribute("title", "Crear nova plantilla");
            model.addAttribute("plantillaId", id);
            model.addAttribute("styles", styleService.findAll());
            SimpleResponse<List<PlantiFunc>> functionsResponse = templateService.getFunctions();
            model.addAttribute("functions", functionsResponse.getItem());
        }
        else {
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
        }

        return "taula/create";
    }


    @GetMapping(value = {"/styles/create", "/styles/edit/{id}"})
    public String createStyle(@PathVariable(required = false) Long id, Model model) {
        if (id == null) {
            model.addAttribute("title", "Crear nou estil");
            model.addAttribute("isEdit", false);
        } else {
            Style style = styleService.findById(id);
            model.addAttribute("title", "Editar estil");
            model.addAttribute("style", style);
            model.addAttribute("isEdit", true);
        }

        return "taula/createStyle";
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

    @PostMapping("/delete/{id}")
    public String deleteTemplate(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            templateService.deleteTemplate(id);
            return "redirect:/template/list?deleted=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error eliminant la plantilla: " + e.getMessage());
            return "redirect:/template/list";
        }
    }

    @PostMapping("/styles/delete/{id}")
    public String deleteStyle(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            styleService.deleteStyle(id);
            return "redirect:/template/styles?deleted=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error eliminant l'estil: " + e.getMessage());
            return "redirect:/template/styles";
        }
    }


    @PostMapping("/attachments/delete/{id}")
    public String deleteAttachment(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            attachmentService.deleteAttachment(id);
            return "redirect:/template/attachments?deleted=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error eliminant l'adjunt: " + e.getMessage());
            return "redirect:/template/attachments";
        }
    }


    @GetMapping("/styles/list")
    @ResponseBody
    public List<Style> getStyles() {
        return styleService.findAll();
    }

    @GetMapping("/ensLocals/list")
    @ResponseBody
    public List<EnsLocal> getEnsLocals() {
        return styleService.findAllEnsLocal();
    }

    @ResponseBody
    @PostMapping("/styles/save2")
    public SimpleResponse<Style> saveStyle2(@RequestBody @Valid Style style) {
        return styleService.saveStyle(style);
    }

    @ResponseBody
    @PutMapping("/styles/save2")
    public SimpleResponse<Style> updateStyle2(@RequestBody @Valid Style style) {
        return styleService.updateStyle(style);
    }

    @GetMapping(value = {"/attachments/create", "/attachments/edit/{id}"})
    public String createAttachment(@PathVariable(required = false) Long id, Model model) {
        if (id == null) {
            model.addAttribute("title", "Crear adjunt");
            model.addAttribute("isEdit", false);
        } else {
            Attachment attachment = attachmentService.findById(id);
            model.addAttribute("title", "Editar adjunt");
            model.addAttribute("attachment", attachment);
            model.addAttribute("isEdit", true);
        }

        return "taula/createAttachment";
    }


    @ResponseBody
    @PostMapping("/attachments/save2")
    public SimpleResponse<Attachment> saveAttachment2(@RequestParam("code") String code,
                                                      @RequestParam("name") String name,
                                                      @RequestParam("file") MultipartFile file) {
        return attachmentService.saveAttachment(code, name, file);
    }

    @ResponseBody
    @PutMapping("/attachments/save2")
    public SimpleResponse<Attachment> updateAttachment2(@RequestParam("id") Long id,
                                                        @RequestParam("code") String code,
                                                        @RequestParam("name") String name,
                                                        @RequestParam(value = "file", required = false) MultipartFile file) {
        return attachmentService.updateAttachment(id, code, name, file);
    }

    @ResponseBody
    @GetMapping("/import-variables")
    public SimpleResponse<List<Variable>> importVariables(@RequestParam String modelId) {
        return templateService.importVariablesFromModel(modelId);
    }

    @PostMapping("import")
    @ResponseBody
    public SimpleResponse<Template> importTemplate(@RequestBody TemplateImportDTO templateDTO) {
        log.info("Importando plantilla: {}", templateDTO.getName());
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

    @GetMapping("/styles/css/{styleId}")
    @ResponseBody
    public ResponseEntity<String> getStyleCss(@PathVariable Long styleId) {
        String css = styleService.getStyleCss(styleId);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/css"))
                .body(css);
    }
}