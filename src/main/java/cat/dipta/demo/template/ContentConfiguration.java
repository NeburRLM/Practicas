package cat.dipta.demo.template;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ContentConfiguration {
    private String gspCode;
    private List<Object> variables;
}