package cat.dipta.demo.validacio;

import cat.dipta.springutils.httpresponses.SimpleResponse;
import cat.dipta.springutils.responses.DiptaRespostaSimple;
import cat.dipta.springutils.validations.Doi;
import cat.dipta.springutils.validations.utils.DocumentValidator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("rest/validacio")
@Slf4j
@Validated
public class ValidacioController {
    @GetMapping("nif")
    SimpleResponse<Boolean> validateNif (@RequestParam String nif) {
        Boolean result = DocumentValidator.validateNIF(nif);
        return SimpleResponse.of(result);
    }

    @PostMapping("nif")
    SimpleResponse<Boolean> validateNifAnnotacio (@Valid @RequestBody DocIdent ident) {
        return SimpleResponse.of(true);
    }
  
}
