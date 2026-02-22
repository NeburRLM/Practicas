package cat.dipta.demo.root;

import cat.dipta.starters.layout.LayoutController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@LayoutController
@RequestMapping("/")
public class RootController {

    @GetMapping
    public String home() {
        return "redirect:/templates";
    }
}