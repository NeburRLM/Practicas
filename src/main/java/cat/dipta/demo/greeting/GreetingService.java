package cat.dipta.demo.greeting;

import cat.dipta.starters.userinfo.dto.DiptaUserInfoData;
import org.apache.catalina.LifecycleState;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GreetingService {
    DiptaUserInfoData diptaUserInfoData;
    
    public GreetingService(DiptaUserInfoData diptaUserInfoData) {
        this.diptaUserInfoData = diptaUserInfoData;
    }
    
    public String greet(String name) {
        if (name == null) {
            name = String.format("user %s", diptaUserInfoData.getUser());
        }
        return String.format("Hello %s !", name);
    }
    public boolean pendingMethod() {
        return false;
    }

}
