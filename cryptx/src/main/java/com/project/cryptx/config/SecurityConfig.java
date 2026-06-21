package com.project.cryptx.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

import com.project.cryptx.security.CsrfCookieFilter;
import com.project.cryptx.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.project.cryptx.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
@Slf4j
@EnableWebSecurity
public class SecurityConfig {

        @Value("${cors.allowed-origin}")
        private String allowedOrigins;

        @Value("${cookie.secure}")
        private boolean cookieSecure;

        private JwtFilter jwtFilter;
        private CsrfCookieFilter csrfCookieFilter;
        private OAuth2SuccessHandler oauth2SuccessHandler;
        private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

        public SecurityConfig(JwtFilter jwtFilter, CsrfCookieFilter csrfCookieFilter,
                        OAuth2SuccessHandler oauth2SuccessHandler,
                        HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
                this.jwtFilter = jwtFilter;
                this.csrfCookieFilter = csrfCookieFilter;
                this.oauth2SuccessHandler = oauth2SuccessHandler;
                this.cookieRepository = cookieRepository;
        }

        @Bean
        public CsrfTokenRequestAttributeHandler requestHandler() {
                CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName(null);
                return requestHandler;
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.setAllowedOriginPatterns(
                                Arrays.stream(allowedOrigins.split(","))
                                                .map(String::trim)
                                                .toList());
                config.setAllowedHeaders(List.of("*"));
                config.setExposedHeaders(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setMaxAge(3600L);
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
                        ClientRegistrationRepository clientRegistrationRepository) {

                DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                                clientRegistrationRepository, "/oauth2/authorization");

                authorizationRequestResolver.setAuthorizationRequestCustomizer(
                                customizer -> customizer.additionalParameters(
                                                params -> params.put("prompt", "select_account")));

                return authorizationRequestResolver;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http,
                        ClientRegistrationRepository clientRegistrationRepository) throws Exception {
                return http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> {
                                        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository
                                                        .withHttpOnlyFalse();
                                        repository.setCookieCustomizer(cookie -> {
                                                cookie.sameSite("None");
                                                cookie.secure(cookieSecure);
                                        });
                                        csrf.csrfTokenRepository(repository)
                                                        .csrfTokenRequestHandler(requestHandler())
                                                        .ignoringRequestMatchers("/auth/**", "/actuator/health",
                                                                        "/home/crypto/**", "/ws/crypto/**",
                                                                        "/payment/razorpay/**");
                                })
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers("/home/crypto/**").permitAll()
                                                .requestMatchers("/ws/crypto/**").permitAll()
                                                // .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/payment/**").authenticated()
                                                .requestMatchers("/auth/**", "/csrf").permitAll()
                                                .requestMatchers("/", "/error", "/auth/**", "/csrf").permitAll()
                                                // .requestMatchers("/cache/**").permitAll()
                                                .requestMatchers("/portfolio/**").authenticated()
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/user/**").authenticated()
                                                .requestMatchers("/wallet/**").authenticated()
                                                .requestMatchers("/trade/**").authenticated()
                                                .requestMatchers("/actuator/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .oauth2Login(oauth2 -> oauth2
                                                .authorizationEndpoint(authEndpoint -> authEndpoint
                                                                .authorizationRequestRepository(cookieRepository)
                                                                .authorizationRequestResolver(
                                                                                authorizationRequestResolver(
                                                                                                clientRegistrationRepository)))
                                                .successHandler(oauth2SuccessHandler)
                                                .failureHandler((request, response, exception) -> {
                                                        log.error("OAuth2 Login Failed: {}", exception.getMessage(),
                                                                        exception);
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("text/plain");
                                                        response.getWriter().write(
                                                                        "OAuth2 Error: " + exception.getMessage());
                                                }))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // This prevents the browser popup by sending a clean 401
                                                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                                                        "Unauthorized access");
                                                }))
                                .httpBasic(basic -> basic.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(csrfCookieFilter, CsrfFilter.class)
                                .build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public WebClient webClient() {
                return WebClient.builder()
                                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                                .build();
        }
}
