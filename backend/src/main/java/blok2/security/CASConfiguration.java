package blok2.security;

import blok2.helpers.Resources;
import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Collections;

/*******************************************************************************************
 *                  Beans for CAS Client Single Sign-On Configuration                      *
 *                                                                                         *
 *    For a more detailed explanation, have a look at                                      *
 *      - https://docs.spring.io/spring-security/site/docs/4.2.x/reference/html/cas.html   *
 *      - https://debbabi-nader.github.io/cas-spring-angular/index.html                    *
 *******************************************************************************************/
@Configuration
public class CASConfiguration {

    private final AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;
    private final AuthenticationSuccessHandler authenticationSuccessHandler;

    @Autowired
    public CASConfiguration(AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService,
                            AuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public ServiceProperties serviceProperties() {
        ServiceProperties serviceProperties = new ServiceProperties();

        // The 'service' property must equal a URL that will be monitored by the
        // CasAuthenticationFilter. This will be: https://localhost:8080/login/cas
        serviceProperties.setService(Resources.blokatugentConf.getString("casCallbackUrl"));

        // Tell the CAS login services that SSO is acceptable, the user will not be
        // forced to fill in his credentials if he is already logged in with CAS on
        // another 'UGent CAS client' service (e.g. Ufora, Oasis...).
        serviceProperties.setSendRenew(false);

        return serviceProperties;
    }

    @Bean
    public CasAuthenticationEntryPoint casAuthenticationEntryPoint(ServiceProperties serviceProperties) {
        CasAuthenticationEntryPoint entryPoint = new CasAuthenticationEntryPoint();
        entryPoint.setLoginUrl(Resources.blokatugentConf.getString("casLoginUrl") + "/login");
        entryPoint.setServiceProperties(serviceProperties);
        return entryPoint;
    }

    @Bean
    public TicketValidator ticketValidator() {
        return new Cas20ServiceTicketValidator(Resources.blokatugentConf.getString("casLoginUrl"));
    }

    @Bean
    public CasAuthenticationProvider casAuthenticationProvider() {
        CasAuthenticationProvider provider = new CasAuthenticationProvider();
        provider.setServiceProperties(serviceProperties());
        provider.setTicketValidator(ticketValidator());

        // TODO: actually implement the userDetailsService for the CasAuthenticationProvider
        provider.setAuthenticationUserDetailsService(authenticationUserDetailsService);

        // A key is required so CasAuthenticationProvider can identify tokens
        // that haven been authenticated previously
        provider.setKey("CAS_PROVIDER_LOCALHOST_8080");

        return provider;
    }

    @Bean
    public CasAuthenticationFilter casAuthenticationFilter(ServiceProperties serviceProperties) {
        CasAuthenticationFilter filter = new CasAuthenticationFilter();
        filter.setServiceProperties(serviceProperties);

        filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);

        AuthenticationManager authenticationManager =
                new ProviderManager(Collections.singletonList(casAuthenticationProvider()));
        filter.setAuthenticationManager(authenticationManager);

        return filter;
    }
}
