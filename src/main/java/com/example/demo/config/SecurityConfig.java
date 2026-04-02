package com.example.demo.config;

import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <- нужно для @PreAuthorize
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtTokenProvider jwtTokenProvider) {
        this.userDetailsService = userDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/", "/hello", "/auth/register", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/licenses/public-key").permitAll() // ← добавлено

                        // License endpoints
                        .requestMatchers(HttpMethod.POST, "/api/licenses").hasRole("ADMIN")
                        .requestMatchers("/api/licenses/**").authenticated()

                        // Signature endpoints — read (all authenticated)
                        .requestMatchers(HttpMethod.GET, "/api/signatures").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/signatures/diff").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/signatures/by-ids").authenticated()
                        // Signature endpoints — write (ADMIN only)
                        .requestMatchers(HttpMethod.POST, "/api/signatures").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/signatures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/signatures/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/signatures/*/history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/signatures/*/audit").hasRole("ADMIN")

                        // Binary signature endpoints (authenticated)
                        .requestMatchers("/api/binary/signatures/**").authenticated()

                        // Users
                        .requestMatchers(HttpMethod.POST, "/api/users/create").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").authenticated()

                        // Everything else
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    public static class JwtAuthenticationFilter extends OncePerRequestFilter {

        private final JwtTokenProvider jwtTokenProvider;
        private final CustomUserDetailsService userDetailsService;

        public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                       CustomUserDetailsService userDetailsService) {
            this.jwtTokenProvider = jwtTokenProvider;
            this.userDetailsService = userDetailsService;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {

            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateAccessToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            filterChain.doFilter(request, response);
        }

        private String extractToken(HttpServletRequest request) {
            String bearer = request.getHeader("Authorization");
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
            return null;
        }
    }
}
