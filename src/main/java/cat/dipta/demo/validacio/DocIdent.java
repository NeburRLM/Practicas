package cat.dipta.demo.validacio;

import cat.dipta.springutils.validations.Doi;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Validated
public class DocIdent {
    @Doi(type="NIF")
    @NotNull
    String nif;
}
