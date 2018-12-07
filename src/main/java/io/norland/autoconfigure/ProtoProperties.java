package io.norland.autoconfigure;

import lombok.Data;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@ConfigurationProperties(prefix = "norland")
@Data
public class ProtoProperties {

    private boolean dispatcherEnabled;
    private String serverType;//udp tcp
    private Integer listenPort;
    private String leakDetectorLevel;

    private boolean longTimeExecutorEnabled;
    private Integer currentThreadNum;
    private Integer coreThreadNum;
    private Integer maxThreadNum;
}
