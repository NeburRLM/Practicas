package cat.dipta.demo.style;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@LayoutController
@RequestMapping("styles")
@Slf4j
public class StyleController {
    private final StyleService styleService;

    public StyleController(StyleService styleService) {
        this.styleService = styleService;
    }

    @GetMapping
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

        return "taula/styles";
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

    @GetMapping("/create")
    public String createStyle(Model model) {
        model.addAttribute("title", "Crear estil");
        model.addAttribute("isEdit", false);

        return "taula/createStyle";
    }

    @GetMapping( "/edit/{id}")
    public String editStyle(@PathVariable Long id, Model model) {

        Style style = styleService.findById(id);
        model.addAttribute("title", "Editar estil");
        model.addAttribute("style", style);
        model.addAttribute("isEdit", true);

        return "taula/createStyle";
    }

    @ResponseBody
    @PostMapping("/save2")
    public SimpleResponse<Style> saveStyle2(@RequestBody @Valid Style style) {
        return styleService.saveStyle(style);
    }

    @ResponseBody
    @PutMapping("/save2")
    public SimpleResponse<Style> updateStyle2(@RequestBody @Valid Style style) {
        return styleService.updateStyle(style);
    }

    @ResponseBody
    @PostMapping("/delete/{id}")
    public SimpleResponse<String> deleteStyle(@PathVariable String id) {
        try {
            styleService.deleteStyle(id);
            return SimpleResponse.of("Estil eliminat correctament");
        } catch (Exception e) {
            throw new RuntimeException("Error eliminant l'estil: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @ResponseBody
    public List<Style> getStyles() {
        return styleService.findAll();
    }

    @GetMapping("/ensLocals/list")
    @ResponseBody
    public List<EnsLocal> getEnsLocals() {
        return styleService.findAllEnsLocal();
    }

    @GetMapping("/css/{styleId}")
    @ResponseBody
    public ResponseEntity<String> getStyleCss(@PathVariable Long styleId) {
        String css = styleService.getStyleCss(styleId);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/css"))
                .body(css);
    }

}
