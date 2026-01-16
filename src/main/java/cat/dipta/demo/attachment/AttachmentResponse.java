package cat.dipta.demo.attachment;

import lombok.Data;
import java.util.List;

@Data
public class AttachmentResponse {
    private List<Attachment> attachments;
}