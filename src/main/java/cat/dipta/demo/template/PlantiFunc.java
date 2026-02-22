package cat.dipta.demo.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantiFunc {
    private Long id;
    private String name;
    private String description;
    private String htmlText;
}