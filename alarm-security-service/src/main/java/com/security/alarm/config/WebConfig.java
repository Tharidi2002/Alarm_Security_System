package com.security.alarm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ============================================================
        // ALLOWED ORIGINS - Add ALL Vercel domains
        // ============================================================
        configuration.setAllowedOriginPatterns(List.of(
            // Vercel Production Domains
            "https://alarm-security-system-java.vercel.app",
            "https://alarm-security-system-jawa.vercel.app",
            "https://alarm-security-system-*.vercel.app",
            "https://*.vercel.app",  // ALL Vercel domains
            
            // Railway Backend (Self)
            "https://alarmsecuritysystem-production.up.railway.app",
            
            // Local Development
            "http://localhost:5173",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000"
        ));
        
        // ============================================================
        // ALLOWED METHODS
        // ============================================================
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        
        // ============================================================
        // ALLOWED HEADERS
        // ============================================================
        configuration.setAllowedHeaders(List.of(
            "*",
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // ============================================================
        // EXPOSED HEADERS
        // ============================================================
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Disposition"
        ));
        
        // ============================================================
        // CREDENTIALS
        // ============================================================
        configuration.setAllowCredentials(true);
        
        // ============================================================
        // PREFLIGHT CACHE - 1 hour
        // ============================================================
        configuration.setMaxAge(3600L);
        
        // ============================================================
        // REGISTER CORS MAPPINGS
        // ============================================================
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/api/auth/**", configuration);
        source.registerCorsConfiguration("/api/admin/**", configuration);
        source.registerCorsConfiguration("/api/alerts/**", configuration);
        source.registerCorsConfiguration("/api/reports/**", configuration);
        source.registerCorsConfiguration("/api/admin/zones/**", configuration);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        
        System.out.println("✅ CORS Configuration updated with Vercel domains");
        
        return bean;
    }
}