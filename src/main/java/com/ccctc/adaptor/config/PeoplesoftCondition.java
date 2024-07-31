package com.ccctc.adaptor.config;

import com.ccctc.adaptor.util.CoverageIgnore;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to test whether the profile is for the peoplesoft sisType. Used to conditionally load certain beans.
 */
public class PeoplesoftCondition implements Condition {

    @CoverageIgnore
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        String sisType = conditionContext.getEnvironment().getProperty("sisType");
        return (sisType != null && sisType.matches("peoplesoft"));
    }
}
