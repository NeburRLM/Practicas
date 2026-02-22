package cat.dipta.demo.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import java.util.List;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Template {
    @Id
    private Long id ;

    private String name;
    private String code;
    private Integer parentFolder;
    private String creatorUser;
    private String creatorUnit;
    private Date dateCreated;
    private Date lastUpdated;
    private String updateUser;
    private List<Box> boxes;
    private List<Variable> variables;
    private PageDimensions pageDimensions;
    private Long style;
    private Integer maxColPosition=23;
    private Integer maxRowPosition=33;


}
