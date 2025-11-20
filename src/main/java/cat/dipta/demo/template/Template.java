package cat.dipta.demo.template;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import java.util.List;


@Getter
@Setter

public class Template {
    @Id
    private long id ;

    String name;
    String code;
    String creatorUser;
    String creatorUnit;
    Date dateCreated;
    Date lastUpdated;
    String updateUser;
    private List<Box> boxes;
    private List<Variable> variables;
    private PageDimensions pageDimensions;
    private Long style;
    private Integer maxColPosition;
    private Integer maxRowPosition;
}
