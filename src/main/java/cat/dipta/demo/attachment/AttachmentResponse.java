package cat.dipta.demo.attachment;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentResponse {
    private List<Attachment> attachments;
}