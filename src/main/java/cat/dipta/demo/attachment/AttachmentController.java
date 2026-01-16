package cat.dipta.demo.attachment;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.starters.layout.LayoutController;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@LayoutController
@RequestMapping("attachments")
@Slf4j
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping
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

        return "taula/attachments";
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
    public String createAttachment(Model model) {
        model.addAttribute("title", "Crear adjunt");
        model.addAttribute("isEdit", false);
        return "taula/createAttachment";
    }

    @GetMapping("/edit/{id}")
    public String editAttachment(@PathVariable Long id, Model model) {
        Attachment attachment = attachmentService.findById(id);
        model.addAttribute("title", "Editar adjunt");
        model.addAttribute("attachment", attachment);
        model.addAttribute("isEdit", true);
        return "taula/createAttachment";
    }

    @ResponseBody
    @PostMapping("/save2")
    public SimpleResponse<Attachment> saveAttachment2(@RequestParam("code") String code,
                                                      @RequestParam("name") String name,
                                                      @RequestParam("file") MultipartFile file) {
        return attachmentService.saveAttachment(code, name, file);
    }

    @ResponseBody
    @PutMapping("/save2")
    public SimpleResponse<Attachment> updateAttachment2(@RequestParam("id") Long id,
                                                        @RequestParam("code") String code,
                                                        @RequestParam("name") String name,
                                                        @RequestParam(value = "file", required = false) MultipartFile file) {
        return attachmentService.updateAttachment(id, code, name, file);
    }

    @ResponseBody
    @PostMapping("/delete/{id}")
    public SimpleResponse<String> deleteAttachment(@PathVariable String id) {
        try {
            attachmentService.deleteAttachment(id);
            return SimpleResponse.of("Adjunt eliminat correctament");
        } catch (Exception e) {
            throw new RuntimeException("Error eliminant l'adjunt: " + e.getMessage());
        }
    }
}
