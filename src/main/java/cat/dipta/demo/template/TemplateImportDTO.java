package cat.dipta.demo.template;

import cat.dipta.demo.style.Style;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemplateImportDTO {
    private Long id;
    private String name;
    private String code;
    private Integer maxColPosition;
    private Integer maxRowPosition;
    private PageDimensions pageDimensions;
    private List<Box> boxes;
    private List<Variable> variables;
    private String environment;
    private Style style;  // Objecte complet, no Long
}