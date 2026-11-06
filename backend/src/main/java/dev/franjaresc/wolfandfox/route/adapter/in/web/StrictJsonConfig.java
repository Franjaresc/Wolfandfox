package dev.franjaresc.wolfandfox.route.adapter.in.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * Rejects JSON numbers and booleans where the contract declares a string. Jackson accepts them by
 * default, so {@code "routeCode": 1234} would become the valid code "1234". No application property
 * covers this; the other strictness settings are in application.yml.
 */
@Configuration(proxyBeanMethods = false)
class StrictJsonConfig {

    @Bean
    JsonMapperBuilderCustomizer rejectScalarsAsStrings() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, config -> {
            config.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
            config.setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
            config.setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
        });
    }
}
