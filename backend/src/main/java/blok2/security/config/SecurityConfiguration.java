package blok2.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.saml.*;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.web.*;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.*;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${server.port}")
    private int serverPort = 8080;

    @Value("${saml.sp}")
    private String samlAudience;

    private final CasAuthenticationProvider casAuthenticationProvider;
    private final CasAuthenticationEntryPoint casAuthenticationEntryPoint;
    private final LogoutSuccessHandler logoutSuccessHandler;

    private final Set<String> springProfilesActive;

    private final SAMLAuthenticationProvider samlAuthenticationProvider;
    private final SAMLEntryPoint samlEntryPoint;
    private final SAMLLogoutFilter samlLogoutFilter;
    private final SAMLLogoutProcessingFilter samlLogoutProcessingFilter;
    private final ExtendedMetadata extendedMetadata;
    private final KeyManager keyManager;

    @Qualifier("saml")
    private final SavedRequestAwareAuthenticationSuccessHandler samlAuthSuccessHandler;
    @Qualifier("saml")
    private final SimpleUrlAuthenticationFailureHandler samlAuthFailureHandler;


    @Autowired
    public SecurityConfiguration(CasAuthenticationProvider casAuthenticationProvider,
                                 CasAuthenticationEntryPoint casAuthenticationEntryPoint,
                                 @Qualifier("customLogoutSuccessHandler") LogoutSuccessHandler logoutSuccessHandler,
                                 Environment env,
                                 SAMLAuthenticationProvider samlAuthenticationProvider,
                                 SAMLEntryPoint samlEntryPoint,
                                 SAMLLogoutFilter samlLogoutFilter,
                                 SAMLLogoutProcessingFilter samlLogoutProcessingFilter,
                                 @Qualifier("successRedirectHandler") SavedRequestAwareAuthenticationSuccessHandler samlAuthSuccessHandler,
                                 SimpleUrlAuthenticationFailureHandler samlAuthFailureHandler,
                                 ExtendedMetadata extendedMetadata,
                                 KeyManager keyManager) {
        this.casAuthenticationProvider = casAuthenticationProvider;
        this.casAuthenticationEntryPoint = casAuthenticationEntryPoint;
        this.logoutSuccessHandler = logoutSuccessHandler;

        this.springProfilesActive = new TreeSet<>();
        Collections.addAll(springProfilesActive, env.getActiveProfiles());

        this.samlAuthenticationProvider = samlAuthenticationProvider;
        this.samlEntryPoint = samlEntryPoint;
        this.samlLogoutFilter = samlLogoutFilter;
        this.samlLogoutProcessingFilter = samlLogoutProcessingFilter;
        this.samlAuthSuccessHandler = samlAuthSuccessHandler;
        this.samlAuthFailureHandler = samlAuthFailureHandler;
        this.extendedMetadata = extendedMetadata;
        this.keyManager = keyManager;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(this.casAuthenticationProvider);
        auth.authenticationProvider(this.samlAuthenticationProvider);
    }

    /**
     * Disable the Spring Security chain all together for the endpoints "/dev/*"
     * if the spring.profiles.active environment variable contains "dev".
     */
    @Override
    public void configure(WebSecurity webSecurity) {
        if (springProfilesActive.contains("dev"))
            webSecurity.ignoring().antMatchers("/dev/**");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf()
                .disable();
        //.csrfTokenRepository(csrfTokenRepository());

        http.authorizeRequests()
                .regexMatchers("/login/cas", "/login/saml", "/SSO/saml", "/discovery/saml", "/api/logout/saml", "/SingleLogout/saml", "/api/SSO/saml").authenticated() // used to trigger cas flow
                .anyRequest().permitAll();

        /*http.exceptionHandling()
                .defaultAuthenticationEntryPointFor(
                        casAuthenticationEntryPoint,
                        new AntPathRequestMatcher("/login/cas"))
                .defaultAuthenticationEntryPointFor(
                        samlEntryPoint,
                        new AntPathRequestMatcher("/login/saml"));*/
        http
                .httpBasic()
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().endsWith("/login/saml")) {
                        samlEntryPoint.commence(request, response, authException);
                    } else {
                        casAuthenticationEntryPoint.commence(request, response, authException);
                    }
                });

        http
                .addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
                .addFilterAfter(samlFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(samlFilter(), CsrfFilter.class);

        http.requestCache().requestCache(requestCache());

        http.logout()
                .logoutSuccessHandler(logoutSuccessHandler)
                .logoutUrl("/logout");
    }

    /**
     * The class PortMapperImpl will put two values by default in
     * its attribute 'httpsPortMappings':
     * - 80 -> 443
     * - 8080 -> 8443
     * <p>
     * This is a problem when the AbstractAuthenticationProcessingFilter
     * calls successHandler.onAuthenticationSuccess(...) in the method
     * AbstractAuthenticationProcessingFilter#successfulAuthentication(...).
     * <p>
     * (Note: the successHandler mentioned above by default is an instance of
     * SavedRequestAwareAuthenticationSuccessHandler)
     * <p>
     * After validating the ticket, the server needs to redirect the user's
     * browser to the original request-url. But thanks to the default
     * PortMapperImpl, the port 8080 will be redirected to 8443, which is
     * not correct. Thanks to https://stackoverflow.com/a/55660281/9356123,
     * this is a working implementation.
     */
    private PortMapper portMapper() {
        PortMapperImpl portMapper = new PortMapperImpl();

        if (serverPort == -1) {
            serverPort = 8080;
        }

        portMapper.setPortMappings(
                new HashMap<String, String>() {{
                    put(Integer.toString(serverPort), Integer.toString(serverPort));
                }}
        );

        return portMapper;
    }

    private RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        PortResolverImpl portResolver = new PortResolverImpl();
        portResolver.setPortMapper(portMapper());
        requestCache.setPortResolver(portResolver);
        return requestCache;
    }

    private CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokenRepository.setCookiePath("/");
        return tokenRepository;
    }

    @Bean
    public FilterChainProxy samlFilter() throws Exception {
        List<SecurityFilterChain> chains = new ArrayList<>();
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/SSO/saml/**"),
                samlWebSSOProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/discovery/saml/**"),
                samlDiscovery()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/login/saml/**"),
                samlEntryPoint));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/logout/saml/**"),
                samlLogoutFilter));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/api/SingleLogout/saml/**"),
                samlLogoutProcessingFilter));
        return new FilterChainProxy(chains);
    }

    /**
     * Will authenticate the associated auth token when the user logs in and the IdP redirects the SAML response to the
     * /SSO/saml URI for processing.
     */
    @Bean
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(samlAuthSuccessHandler);
        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(samlAuthFailureHandler);
        return samlWebSSOProcessingFilter;
    }

    /**
     * Will discover the IdP to contact for authentication after that samlEntryPoint handled the entry request.
     */
    @Bean
    public SAMLDiscovery samlDiscovery() {
        return new SAMLDiscovery();
    }

    public MetadataGenerator metadataGenerator() {
        MetadataGenerator metadataGenerator = new MetadataGenerator();
        metadataGenerator.setEntityId(samlAudience);
        metadataGenerator.setExtendedMetadata(extendedMetadata);
        metadataGenerator.setIncludeDiscoveryExtension(false);
        metadataGenerator.setKeyManager(keyManager);
        return metadataGenerator;
    }

    @Bean
    public MetadataGeneratorFilter metadataGeneratorFilter() {
        return new MetadataGeneratorFilter(metadataGenerator());
    }
}
