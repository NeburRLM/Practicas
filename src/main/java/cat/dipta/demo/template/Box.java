package cat.dipta.demo.template;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Box {
    private Long id;
    private Integer boxType;
    private Integer rowPosition;
    private Integer colPosition;
    private Integer height;
    private Integer width;
    private String innerHtml;
}