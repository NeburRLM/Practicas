package cat.dipta.demo.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Box {
    private Long id;
    private Integer boxType;
    private Integer rowPosition;
    private Integer colPosition;
    private Integer height;
    private Integer width;
    private String innerHtml;
    private ContentConfiguration contentConfiguration;
}