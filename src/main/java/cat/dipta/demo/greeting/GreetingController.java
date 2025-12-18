package cat.dipta.demo.greeting;

import cat.dipta.starters.layout.LayoutController;
import cat.dipta.starters.userinfo.dto.DiptaUserInfoData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@LayoutController // necessari per l'starter dipta-layout-starter
@RequestMapping({"/", "public"}) // TODO: configurat per respondre a 2 url diferents. Una de pública, l'altra privada.
@Slf4j
public class GreetingController {
    
    DiptaUserInfoData diptaUserInfoData;
    GreetingService greetingService;
    
    public GreetingController(DiptaUserInfoData diptaUserInfoData, GreetingService greetingService) {
        this.diptaUserInfoData = diptaUserInfoData;
        this.greetingService = greetingService;
    }
    
    @GetMapping
    String helloWorld(@RequestParam Optional<String> name, Model model) {
        String msg = greetingService.greet(name.orElse(null));
        log.info(String.format("HELLO: %s", msg));
        model.addAttribute("greet", msg);
        model.addAttribute("nomUsuari", diptaUserInfoData.getProfile().getNomComplet());
        return "hello-world";
    }

    
}
