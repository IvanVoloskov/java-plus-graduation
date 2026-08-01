package ewm.client;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestClientConfig {

    @Bean
    public Retryer feignRetryer() {
        // 500мс между попытками, максимум 2000мс суммарно, 3 попытки
        return new Retryer.Default(500, 2000, 3);
    }
}