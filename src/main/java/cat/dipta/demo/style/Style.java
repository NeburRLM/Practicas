package cat.dipta.demo.style;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Style {
    private Long id;
    private String code;
    private String name;
    private String rules;
    private String creatorUser;
    private String creatorUnit;
    private Date dateCreated;
    private Date lastUpdated;
    private String updateUser;
    private EnsLocal ensLocal;
}
