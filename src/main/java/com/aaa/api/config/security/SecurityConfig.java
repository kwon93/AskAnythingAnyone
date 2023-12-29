package com.aaa.api.config.security;

import com.aaa.api.config.security.jwt.JwtTokenProvider;
import com.aaa.api.config.security.jwt.filter.JwtAuthenticationFilter;
import com.aaa.api.config.security.provider.CustomAuthenticationProvider;
import com.aaa.api.domain.Users;
import com.aaa.api.repository.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsersRepository usersRepository;
    private final ObjectMapper objectMapper;
    private final AuthenticationConfiguration authenticationConfiguration;

    @Bean
    public AuthenticationManager configure() throws Exception {
        ProviderManager manager = (ProviderManager) authenticationConfiguration.getAuthenticationManager();
        manager.getProviders().add(new CustomAuthenticationProvider());
        return authenticationConfiguration.getAuthenticationManager();

    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return web -> web.ignoring()
                .requestMatchers(
                        new AntPathRequestMatcher("/favicon.ico"),
                        new AntPathRequestMatcher("/error"),
                        toH2Console() // H2 DB
                );
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(request ->
                        request.requestMatchers(new AntPathRequestMatcher("/api/login")).permitAll()
                                .requestMatchers(new AntPathRequestMatcher("/api/signup")).permitAll()
                                .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable) //jwt 활용으로 csrf방어 비활성
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public UserDetailsService userDetailsService(){
        return username -> {
                   Users users = usersRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("찾을 수 없는 이메일입니다."));

                    return new CustomUserPrincipal(users);
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


}