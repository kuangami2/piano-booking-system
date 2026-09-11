package com.roomreservation.config;

import com.roomreservation.config.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * 拦截 /api/**，公开接口以 @AuthAccess 标注放行
 */
@Configuration
/**
 * Web 配置：注册鉴权拦截器与静态资源映射，注意继承 WebMvcConfigurationSupport 会关闭默认静态映射。
 */
public class InterceptorConfig extends WebMvcConfigurationSupport {

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor())
                .addPathPatterns("/api/**");
        super.addInterceptors(registry);
    }

    /**
     * 继承 WebMvcConfigurationSupport 会关闭默认静态资源映射，此处补轮播图静态目录
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/**").addResourceLocations("classpath:/static/img/");
        super.addResourceHandlers(registry);
    }

    @Bean
    public JwtInterceptor jwtInterceptor() {
        return new JwtInterceptor();
    }

}
