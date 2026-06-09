package com.hellochat.backend.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SentinelRuleProperties.class)
public class SentinelRuleConfig {

    private final SentinelRuleProperties properties;

    public SentinelRuleConfig(SentinelRuleProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void loadRules() {
        if (!properties.isLocalRulesEnabled()) {
            return;
        }
        List<FlowRule> rules = new ArrayList<>();
        rules.add(qpsRule(SentinelResourceNames.AUTH_SEND_CAPTCHA, properties.getAuthCaptchaQps()));
        rules.add(qpsRule(SentinelResourceNames.AUTH_SEND_CAPTCHA_GET, properties.getAuthCaptchaQps()));
        rules.add(qpsRule(SentinelResourceNames.AUTH_LOGIN, properties.getAuthLoginQps()));
        rules.add(qpsRule(SentinelResourceNames.CHAT_SEND_MESSAGE, properties.getChatSendMessageQps()));
        rules.add(qpsRule(SentinelResourceNames.GROUP_SEND_MESSAGE, properties.getGroupSendMessageQps()));
        rules.add(qpsRule(SentinelResourceNames.GROUP_SEND_MENTION_ALL, properties.getGroupMentionAllQps()));
        rules.add(qpsRule(SentinelResourceNames.FILE_UPLOAD, properties.getFileUploadQps()));
        FlowRuleManager.loadRules(rules);
    }

    private FlowRule qpsRule(String resource, double threshold) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setCount(threshold);
        return rule;
    }
}
