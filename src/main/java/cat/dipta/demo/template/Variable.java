package cat.dipta.demo.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Variable {
    private Long id;
    private String name;
    private Integer type;
    private Object value;
}
