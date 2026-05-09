package com.hellochat.backend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AliyunOssProperties.class)
public class OssConfig {

    @Bean
    @ConditionalOnExpression(
        "'${aliyun.oss.access-key-id:}' != '' && '${aliyun.oss.access-key-secret:}' != '' && '${aliyun.oss.bucket-name:}' != ''"
    )
    public OSS ossClient(AliyunOssProperties properties) {
        return new OSSClientBuilder().build(
            properties.getEndpoint(),
            properties.getAccessKeyId(),
            properties.getAccessKeySecret()
        );
    }
}
