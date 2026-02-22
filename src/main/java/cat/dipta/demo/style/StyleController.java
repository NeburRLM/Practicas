package cat.dipta.demo.style;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@LayoutController
@RequestMapping("styles")
@Slf4j
@RequiredArgsConstructor

public class StyleController {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE_VALUE = "no-cache, no-store, must-revalidate";

    private final StyleService styleService;

    @GetMapping
    public String estils(@PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable,
                         Model model,
                         HttpServletResponse response,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         @RequestParam(value = "size", defaultValue = "20") int size,
                         @RequestParam(value = "searchName", required = false) String searchName,
                         @RequestParam(value = "searchCode", required = false) String searchCode,
                         @RequestParam(value = "searchEnsLocal", required = false) String searchEnsLocal) {

        setNoCacheHeaders(response);

        PageRequest pageRequest = PageRequest.of(page, size, pageable.getSort());
        Page<Style> stylePage = styleService.findAll(pageRequest, searchCode, searchName, searchEnsLocal);

        populateListModel(model, stylePage, searchName, searchCode, searchEnsLocal);

        return "taula/styles";
    }

    @GetMapping("/create")
    public String createStyle(Model model) {
        populateStyleFormModel(model, "Crear estil", null, false);
        return "taula/createStyle";
    }

    @GetMapping("/edit/{id}")
    public String editStyle(@PathVariable Long id, Model model) {
        Style style = styleService.findById(id);
        populateStyleFormModel(model, "Editar estil", style, true);
        return "taula/createStyle";
    }

    @ResponseBody
    @PostMapping("/save2")
    public SimpleResponse<Style> saveStyle(@RequestBody @Valid Style style) {
        return styleService.saveStyle(style);
    }

    @ResponseBody
    @PutMapping("/save2")
    public SimpleResponse<Style> updateStyle(@RequestBody @Valid Style style) {
        return styleService.updateStyle(style);
    }

    @ResponseBody
    @DeleteMapping("/delete/{id}")
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

    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader(CACHE_CONTROL_HEADER, NO_CACHE_VALUE);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    private void populateListModel(Model model, Page<Style> stylePage,
                                   String searchName, String searchCode, String searchEnsLocal) {
        model.addAttribute("pageNumbers", buildPageNumbers(stylePage));
        model.addAttribute("title", "Estils");
        model.addAttribute("page", stylePage);
        model.addAttribute("searchCode", searchCode);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchEnsLocal", searchEnsLocal);
    }

    private void populateStyleFormModel(Model model, String title, Style style, boolean isEdit) {
        model.addAttribute("title", title);
        model.addAttribute("isEdit", isEdit);

        if (style != null) {
            model.addAttribute("style", style);
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

}
