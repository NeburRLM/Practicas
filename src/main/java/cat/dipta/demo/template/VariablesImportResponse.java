package cat.dipta.demo.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VariablesImportResponse {
    @JsonProperty("variables")
    private List<Variable> variables;
}
