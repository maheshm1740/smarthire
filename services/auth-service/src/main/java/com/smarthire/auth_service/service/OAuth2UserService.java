package com.smarthire.auth_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OAuth2UserService extends OidcUserService {

    private final AuthService authService;

    public OAuth2UserService(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider   = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oidcUser.getSubject();
        String email      = oidcUser.getEmail();
        String firstName  = oidcUser.getGivenName()  != null ? oidcUser.getGivenName()  : "";
        String lastName   = oidcUser.getFamilyName() != null ? oidcUser.getFamilyName() : "";

        log.info("OAuth2 login: provider={} email={}", provider, email);
        authService.upsertOAuth2User(provider, providerId, email, firstName, lastName);

        return oidcUser;
    }
}