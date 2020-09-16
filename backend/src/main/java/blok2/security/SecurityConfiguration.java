package blok2.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.PortMapper;
import org.springframework.security.web.PortMapperImpl;
import org.springframework.security.web.PortResolverImpl;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

import java.util.HashMap;
import java.util.Map;

@Profile("!test")
@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Value("${server.port}")
    private int serverPort;

    @Value("${security.sslRedirectPort}")
    private int sslRedirectPort;

    private final CasAuthenticationProvider casAuthenticationProvider;
    private final CasAuthenticationEntryPoint casAuthenticationEntryPoint;

    @Autowired
    public SecurityConfiguration(CasAuthenticationProvider casAuthenticationProvider,
                                 CasAuthenticationEntryPoint casAuthenticationEntryPoint) {
        this.casAuthenticationProvider = casAuthenticationProvider;
        this.casAuthenticationEntryPoint = casAuthenticationEntryPoint;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(this.casAuthenticationProvider);
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());

        http.authorizeRequests()
                .regexMatchers("/login/cas").authenticated() // used to trigger cas flow
                .regexMatchers(HttpMethod.GET, "/api/locations").permitAll() // allow to get the locations
                .regexMatchers(HttpMethod.GET, "/api/locations/.*").permitAll() // allow to get a specific location
                .regexMatchers(HttpMethod.GET, "/api/tags").permitAll() // allow to get the tags of a location
                .regexMatchers("/api.*").authenticated();

        http.httpBasic()
                .authenticationEntryPoint(casAuthenticationEntryPoint);

        http.requestCache().requestCache(requestCache());
    }

    /**
     * The class PortMapperImpl will put two values by default in
     * its attribute 'httpsPortMappings':
     *   - 80 -> 443
     *   - 8080 -> 8443
     *
     * This is a problem when the AbstractAuthenticationProcessingFilter
     * calls successHandler.onAuthenticationSuccess(...) in the method
     * AbstractAuthenticationProcessingFilter#successfulAuthentication(...).
     *
     * (Note: the successHandler mentioned above by default is an instance of
     * SavedRequestAwareAuthenticationSuccessHandler)
     *
     * After validating the ticket, the server needs to redirect the user's
     * browser to the original request-url. But thanks to the default
     * PortMapperImpl, the port 8080 will be redirected to 8443, which is
     * not correct. Thanks to https://stackoverflow.com/a/55660281/9356123,
     * this is a working implementation.
     */
    private PortMapper portMapper() {
        PortMapperImpl portMapper = new PortMapperImpl();
        Map<String, String> mappings = new HashMap<>();
        mappings.put(Integer.toString(serverPort), Integer.toString(sslRedirectPort));
        portMapper.setPortMappings(mappings);
        return portMapper;
    }

    private RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        PortResolverImpl portResolver = new PortResolverImpl();
        portResolver.setPortMapper(portMapper());
        requestCache.setPortResolver(portResolver);
        return requestCache;
    }
}
