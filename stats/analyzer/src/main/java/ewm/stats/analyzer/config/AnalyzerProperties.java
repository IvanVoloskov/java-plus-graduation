package ewm.stats.analyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "analyzer")
public class AnalyzerProperties {

    private Weights weights;

    @Getter
    @Setter
    public static class Weights {
        private double view;
        private double register;
        private double like;
    }
}