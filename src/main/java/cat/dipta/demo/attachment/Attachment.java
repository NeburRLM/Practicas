package cat.dipta.demo.attachment;

import lombok.Data;

@Data
public class Attachment {
    private Long id;
    private String code;
    private String name;
    private String fileName;
    private String fileType;
    private String creatorUser;
    private String url;
}