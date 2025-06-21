package com.ikeda.payment.configs;

import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ResolverConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
//        Filtros
        argumentResolvers.add(new SpecificationArgumentResolver());

//      Paginação
        var pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setFallbackPageable(PageRequest.of(0, 10));
        argumentResolvers.add(pageableResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
//      CORS
//        Limitando acesso para determinadas origens de forma global, pois pode ser inserido em nível de class
//        registry.addMapping("/users/**").allowedOrigins("http://example.com");
//        registry.addMapping("/auth/**").allowedOrigins("http://abc.com");
        registry.addMapping("/**").maxAge(3600);
    }
}
