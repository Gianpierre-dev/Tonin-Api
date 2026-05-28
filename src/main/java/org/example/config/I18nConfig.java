package org.example.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Internacionalización (es/en) basada en el header Accept-Language.
 * El MessageSource lo autoconfigura Spring Boot a partir de messages*.properties
 * (ver spring.messages.* en application.properties).
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    private static final Locale ESPANOL = Locale.of("es");

    private final MessageSource messageSource;

    public I18nConfig(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(ESPANOL);
        resolver.setSupportedLocales(List.of(ESPANOL, Locale.ENGLISH));
        return resolver;
    }

    /**
     * Conecta Bean Validation con el MessageSource para que las anotaciones
     * (@NotBlank, @Size, etc.) resuelvan sus claves {validation.*} por idioma.
     * Vía WebMvcConfigurer para reemplazar el validador de MVC sin ambigüedad de beans.
     */
    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
