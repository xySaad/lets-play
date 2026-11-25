package com.z01.letsplay.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.z01.letsplay.infrastructure.EntityAccessResolver;
import com.z01.letsplay.infrastructure.PermissionInterceptor;
import com.z01.letsplay.model.User;
import com.z01.letsplay.resolver.AuthResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final EntityAccessResolver<User> entityAccessResolver;
    private final AuthResolver authResolver;
    private final PermissionInterceptor permissionInterceptor;

    public WebConfig(AuthResolver authUserResolver, EntityAccessResolver<User> entityAccessResolver,
            PermissionInterceptor permissionInterceptor) {
        this.authResolver = authUserResolver;
        this.entityAccessResolver = entityAccessResolver;
        this.permissionInterceptor = permissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authResolver);
        resolvers.add(entityAccessResolver);
    }
}
