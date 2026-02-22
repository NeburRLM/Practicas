package cat.dipta.demo.attachment;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@LayoutController
@RequestMapping("attachments")
@Slf4j
@RequiredArgsConstructor

public class AttachmentController {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String NO_CACHE_VALUE = "no-cache, no-store, must-revalidate";

    private final AttachmentService attachmentService;

    @GetMapping
    public String adjunts(@PageableDefault(sort = "code", direction = Sort.Direction.ASC) Pageable pageable,
                          Model model,
                          HttpServletResponse response,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          @RequestParam(value = "size", defaultValue = "20") int size,
                          @RequestParam(value = "searchName", required = false) String searchName,
                          @RequestParam(value = "searchCode", required = false) String searchCode,
                          @RequestParam(value = "searchFileName", required = false) String searchFileName) {

        setNoCacheHeaders(response);

        PageRequest pageRequest = PageRequest.of(page, size, pageable.getSort());
        Page<Attachment> attachmentPage = attachmentService.findAll(pageRequest, searchCode, searchName, searchFileName);

        populateListModel(model, attachmentPage, searchName, searchCode, searchFileName);

        return "taula/attachments";
    }

    @GetMapping("/create")
    public String createAttachment(Model model) {
        populateAttachmentFormModel(model, "Crear adjunt", null, false);
        return "taula/createAttachment";
    }

    @GetMapping("/edit/{id}")
    public String editAttachment(@PathVariable Long id, Model model) {
        Attachment attachment = attachmentService.findById(id);
        populateAttachmentFormModel(model, "Editar adjunt", attachment, true);
        return "taula/createAttachment";
    }

    @ResponseBody
    @PostMapping("/save2")
    public SimpleResponse<Attachment> saveAttachment(@RequestParam("code") String code,
                                                     @RequestParam("name") String name,
                                                     @RequestParam("file") MultipartFile file) {
        return attachmentService.saveAttachment(code, name, file);
    }

    @ResponseBody
    @PutMapping("/save2")
    public SimpleResponse<Attachment> updateAttachment(@RequestParam("id") Long id,
                                                       @RequestParam("code") String code,
                                                       @RequestParam("name") String name,
                                                       @RequestParam(value = "file", required = false) MultipartFile file) {
        return attachmentService.updateAttachment(id, code, name, file);
    }

    @ResponseBody
    @DeleteMapping("/delete/{id}")
    public SimpleResponse<String> deleteAttachment(@PathVariable String id) {
        try {
            attachmentService.deleteAttachment(id);
            return SimpleResponse.of("Adjunt eliminat correctament");
        } catch (Exception e) {
            throw new RuntimeException("Error eliminant l'adjunt: " + e.getMessage());
        }
    }

    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader(CACHE_CONTROL_HEADER, NO_CACHE_VALUE);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    private void populateListModel(Model model, Page<Attachment> attachmentPage,
                                   String searchName, String searchCode, String searchFileName) {
        model.addAttribute("pageNumbers", buildPageNumbers(attachmentPage));
        model.addAttribute("title", "Adjunts");
        model.addAttribute("page", attachmentPage);
        model.addAttribute("searchCode", searchCode);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchFileName", searchFileName);
    }

    private void populateAttachmentFormModel(Model model, String title, Attachment attachment, boolean isEdit) {
        model.addAttribute("title", title);
        model.addAttribute("isEdit", isEdit);

        if (attachment != null) {
            model.addAttribute("attachment", attachment);
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
