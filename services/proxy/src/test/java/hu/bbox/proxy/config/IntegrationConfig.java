package hu.bbox.proxy.config;

import hu.bbox.proxy.Application;
import hu.bbox.proxy.utils.IntegrationIdGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Supplier;

@Configuration
@Import(Application.class)
public class IntegrationConfig {
    @Bean
    public IntegrationIdGenerator integrationIdGenerator() {
        return new IntegrationIdGenerator();
    }

    @Bean
    public BeanPostProcessor replacingBeanPostProcessor(IntegrationIdGenerator integrationIdGenerator) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if ("idGenerator".equals(beanName)) {
                    return integrationIdGenerator;
                }
                return bean;
            }
        };
    }
}
