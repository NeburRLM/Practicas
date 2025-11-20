package cat.dipta.demo.template;

import cat.dipta.starters.layout.LayoutController;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@LayoutController
@RequestMapping("template")
@Slf4j
public class TemplateController {

    private final TemplateService templateService;
    private final StyleService styleService;
    private final AttachmentService attachmentService;
    private final ObjectMapper objectMapper;

    public TemplateController(TemplateService templateService, StyleService styleService, AttachmentService attachmentService) {
        this.templateService = templateService;
        this.styleService = styleService;
        this.attachmentService = attachmentService;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping(value = "list")
    public String list(@PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model,
                       @RequestParam("page") Optional<Integer> page,
                       @RequestParam("size") Optional<Integer> size,
                       @RequestParam(value = "searchName", required = false) String searchName,
                       @RequestParam(value = "searchCode", required = false) String searchCode,
                       @RequestParam(value = "searchUnit", required = false) String searchUnit,
                       @RequestParam(value = "searchUser", required = false) String searchUser) {

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
                         @RequestParam(value = "searchName", required = false) String searchName,
                         @RequestParam(value = "searchCode", required = false) String searchCode,
                         @RequestParam(value = "searchEnsLocal", required = false) String searchEnsLocal) {

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
                          @RequestParam(value = "searchName", required = false) String searchName,
                          @RequestParam(value = "searchCode", required = false) String searchCode,
                          @RequestParam(value = "searchFileName", required = false) String searchFileName) {

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


            model.addAttribute("title", "Editar plantilla");
            model.addAttribute("plantillaId", id);
            model.addAttribute("template", template);
            model.addAttribute("typeMap", typeMap);
            model.addAttribute("templateStyle", templateStyle);
            model.addAttribute("allBoxesHtml", allBoxesHtml);
            model.addAttribute("styles", styleService.findAll());
        }

        return "taula/create";
    }


    @ResponseBody
    @PostMapping("/save2")
    public Map<String, Object> saveTemplate2(@RequestBody @Valid Template template) {
        try {
            Template savedTemplate = templateService.saveTemplate(
                    objectMapper.writeValueAsString(template)
            );
            return Map.of(
                    "estat", "OK",
                    "id", savedTemplate.getId()
            );
        } catch (Exception e) {
            return Map.of(
                    "estat", "ERROR",
                    "message", e.getMessage()
            );
        }
    }

    @PostMapping("/save")
    public String saveTemplate(
            @RequestParam String name,
            @RequestParam String code,
            @RequestParam String htmlContent,
            @RequestParam(required = false) String style,
            @RequestParam String variablesJson,
            @RequestParam(required = false) Long id,
            RedirectAttributes redirectAttributes) {

        log.info("Saving template: name={}, code={}, id={}", name, code, id);

        try {
            // Construir el JSON con la estructura requerida
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("name", name);
            templateData.put("code", code);
            templateData.put("parentFolder", 1);
            templateData.put("maxColPosition", 23);
            templateData.put("maxRowPosition", 33);

            if (id != null && id != 1) {
                templateData.put("id", id);
            }

            // Dimensiones de página
            Map<String, Object> pageDimensions = new HashMap<>();
            pageDimensions.put("width", 210);
            pageDimensions.put("height", 297);
            pageDimensions.put("marginTop", 18);
            pageDimensions.put("marginBottom", 18);
            pageDimensions.put("marginLeft", 14);
            pageDimensions.put("marginRight", 14);
            templateData.put("pageDimensions", pageDimensions);

            // Box con el contenido HTML
            Map<String, Object> box = new HashMap<>();
            box.put("rowPosition", 1);
            box.put("colPosition", 1);
            box.put("height", 28);
            box.put("width", 23);

            Map<String, Object> contentConfiguration = new HashMap<>();
            contentConfiguration.put("gspCode", "${raw(text)}");
            contentConfiguration.put("variables", new ArrayList<>());
            box.put("contentConfiguration", contentConfiguration);

            box.put("innerHtml", htmlContent);

            templateData.put("boxes", Collections.singletonList(box));
            List<Map<String, Object>> variables =
                    objectMapper.readValue(variablesJson, new TypeReference<>() {
                    });
            templateData.put("variables", variables);
            templateData.put("style", Long.parseLong(style));

            // Convertir a JSON y llamar al servicio
            String jsonPayload = objectMapper.writeValueAsString(templateData);
            Template savedTemplate = templateService.saveTemplate(jsonPayload);

            // addFlashAttribute añade un atributo que estará disponible solo en la siguiente petición
            // Es útil para mostrar mensajes después de un redirect
            redirectAttributes.addFlashAttribute("message", "Plantilla guardada correctament");
            redirectAttributes.addFlashAttribute("messageType", "success");

            return "redirect:/template/edit/" + savedTemplate.getId();

        } catch (Exception e) {
            log.error("Error saving template", e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar la plantilla: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/taula/create/1";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTemplate(
            @PathVariable String id,
            RedirectAttributes redirectAttributes) {

        try {
            templateService.deleteTemplate(id);
            redirectAttributes.addFlashAttribute("success", "Plantilla eliminada correctament.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error eliminant la plantilla: " + e.getMessage());
        }

        return "redirect:/template/list";
    }


    @GetMapping("/styles/list")
    @ResponseBody
    public List<Style> getStyles() {
        return styleService.findAll();
    }

    @GetMapping("styles/create")
    public String createStyle(Model model) {
        model.addAttribute("title", "Crear estil");
        // Aquí deberías cargar la lista de Ens Locals desde tu API
        // model.addAttribute("ensLocals", ensLocalService.findAll());
        return "taula/createStyle";
    }

    @PostMapping("styles/save")
    public String saveStyle(@RequestParam("code") String code,
                            @RequestParam("name") String name,
                            @RequestParam("ensLocal") String ensLocal,
                            @RequestParam("rules") String rules) {
        // Lógica para guardar el estilo en la API
        // styleService.save(code, name, ensLocal, rules);
        return "redirect:/template/styles";
    }

    @GetMapping("attachments/create")
    public String createAttachment(Model model) {
        model.addAttribute("title", "Crear adjunt");
        return "taula/createAttachment";
    }

    @PostMapping("attachments/save")
    public String saveAttachment(@RequestParam("code") String code,
                                 @RequestParam("name") String name,
                                 @RequestParam("file") MultipartFile file) {
        // Lógica para guardar el adjunto en la API
        // attachmentService.save(code, name, file);
        return "redirect:/template/attachments";
    }

    @GetMapping("styles/edit/{id}")
    public String editStyle(@PathVariable Long id, Model model) {
        Style style = styleService.findById(id);

        model.addAttribute("title", "Editar estil");
        model.addAttribute("style", style);
        model.addAttribute("isEdit", true);  // Indicador de modo edición

        return "taula/createStyle";
    }

    @PostMapping("styles/update")
    public String updateStyle(@RequestParam("id") Long id,
                              @RequestParam("code") String code,
                              @RequestParam("name") String name,
                              @RequestParam("ensLocal") String ensLocal,
                              @RequestParam("rules") String rules) {
        // Lógica para actualizar el estilo en la API
        // styleService.update(id, code, name, ensLocal, rules);
        return "redirect:/template/styles";
    }

    @GetMapping("attachments/edit/{id}")
    public String editAttachment(@PathVariable Long id, Model model) {
        Attachment attachment = attachmentService.findById(id);

        model.addAttribute("title", "Editar adjunt");
        model.addAttribute("attachment", attachment);
        model.addAttribute("isEdit", true);

        return "taula/createAttachment";
    }

    @PostMapping("attachments/update")
    public String updateAttachment(@RequestParam("id") Long id,
                                   @RequestParam("code") String code,
                                   @RequestParam("name") String name,
                                   @RequestParam(value = "file", required = false) MultipartFile file) {
        // Lógica para actualizar el adjunto en la API
        // attachmentService.update(id, code, name, file);
        return "redirect:/template/attachments";
    }
}