package cat.dipta.demo.greeting;

import cat.dipta.demo.greeting.dto.GreetMessageDto;
import cat.dipta.springutils.httpresponses.MultipleResponse;
import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.springutils.responses.DiptaRespostaMeta;
import cat.dipta.springutils.responses.DiptaRespostaMultiple;
import cat.dipta.springutils.responses.DiptaRespostaSimple;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Tags(value = {@Tag(name= "Greeting", description = "Operacions de salutacions.")})
@Slf4j // logs
@RequestMapping("/rest/greet") // see /doc/greetings.http for examples on calling this controller. Secured by basic auth
class RestGreetingController {
    
    GreetingService greetingService;
    
    public RestGreetingController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }
    
    @GetMapping
    SimpleResponse<GreetMessageDto> greet (@RequestParam(required = false) String name) {
        String msg = greetingService.greet(name);
        
        GreetMessageDto message = new GreetMessageDto(msg);
        
        Map<String, Object> diptaRespostaMeta = new HashMap<>();
        diptaRespostaMeta.put("dada-extra", "aquesta-dada");
        SimpleResponse<GreetMessageDto> resposta = SimpleResponse.of(message);
        resposta.withMeta(diptaRespostaMeta);
        return resposta;

    }
    
    @GetMapping("multi")
    MultipleResponse<GreetMessageDto> greetMulti () {
        return MultipleResponse.of(List.of(new GreetMessageDto("Hola Xavi !"), new GreetMessageDto("Hola Manu !")));
    }
    
}
