package cat.dipta.demo.attachment;


import cat.dipta.demo.url.PlantillesApi;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class AttachmentBackendClient {
    private final RestTemplate restTemplate;
    private final PlantillesApi plantillesApi;

    public ResponseEntity<AttachmentResponse> findAll() {
        return restTemplate.exchange(plantillesApi.attachments(), HttpMethod.GET, HttpEntity.EMPTY, AttachmentResponse.class);
    }

    public ResponseEntity<Attachment> findById(Long id) {
        String url = plantillesApi.attachmentsFindById(id);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Attachment.class);
    }

    public ResponseEntity<Attachment> saveAttachment(String code, String name, MultipartFile file) {
        HttpEntity<MultiValueMap<String, Object>> req = buildMultipartRequest(code, name, file);
        return restTemplate.exchange(plantillesApi.attachments(), HttpMethod.POST, req, Attachment.class);
    }

    public ResponseEntity<Attachment> updateAttachment(Long id, String code, String name, MultipartFile file) {
        HttpEntity<MultiValueMap<String, Object>> req = buildMultipartRequest(code, name, file);
        String url = plantillesApi.attachmentsFindById(id);
        return restTemplate.exchange(url, HttpMethod.PUT, req, Attachment.class);
    }

    public ResponseEntity<Void> deleteAttachment(Long id) {
        String url = plantillesApi.attachmentsFindById(id);
        return restTemplate.exchange(url, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
    }

    private HttpEntity<MultiValueMap<String, Object>> buildMultipartRequest(String code, String name, MultipartFile file) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("code", code);
            body.add("name", name);

            if (file != null && !file.isEmpty()) {
                body.add("fileName", file.getOriginalFilename());
                body.add("fileType", file.getContentType());

                body.add("dataBytes", new ByteArrayResource(file.getBytes()) {
                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }
                });
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            return new HttpEntity<>(body, headers);
        } catch (Exception e) {
            throw new RuntimeException("Error construint petició multipart (attachment)", e);
        }
    }

}
