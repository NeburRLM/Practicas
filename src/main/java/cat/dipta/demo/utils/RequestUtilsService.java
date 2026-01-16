package cat.dipta.demo.utils;

import cat.dipta.starters.userinfo.dto.DiptaUserInfoData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestUtilsService {
    public static final String APP_NAME = "plantilles-web";
    private final DiptaUserInfoData diptaUserInfoData;

    public <T> HttpEntity<T> getRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-DIPTA-App-Id", APP_NAME);
        headers.set("X-DIPTA-Remote-User-Id", diptaUserInfoData.getUser());
        headers.set("X-DIPTA-Remote-User-Unit", diptaUserInfoData.getUnit());
        headers.set("X-DIPTA-Remote-User-Ens", diptaUserInfoData.getEns());
        return new HttpEntity<>(body, headers);
    }
}
