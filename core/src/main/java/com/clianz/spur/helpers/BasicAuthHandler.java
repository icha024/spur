package com.clianz.spur.helpers;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class BasicAuthHandler implements HttpHandler {
    private HttpHandler next;
    private String expectedUsername;
    private String expectedPasswordStr;

    public BasicAuthHandler(HttpHandler next, String expectedUsername, String expectedPasswordStr) {
        this.next = next;
        this.expectedUsername = expectedUsername;
        this.expectedPasswordStr = expectedPasswordStr;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
        addSecurity(this.next, new IdentityManager() {
            @Override
            public Account verify(Account account) {
                return null;
            }

            @Override
            public Account verify(String id, Credential credential) {
                if (verifyCredential(id, credential)) {
                    new Account() {

                        private final Principal principal = () -> id;

                        @Override
                        public Principal getPrincipal() {
                            return principal;
                        }

                        @Override
                        public Set<String> getRoles() {
                            return Collections.emptySet();
                        }

                    };
                }
                return null;
            }

            @Override
            public Account verify(Credential credential) {
                return null;
            }
        });
    }

    private boolean verifyCredential(String id, Credential credential) {
        if (credential instanceof PasswordCredential) {
            char[] password = ((PasswordCredential) credential).getPassword();
            char[] expectedPassword = expectedPasswordStr.toCharArray();

            return id.equals(expectedUsername) && Arrays.equals(password, expectedPassword);
        }
        return false;
    }

    // https://github.com/undertow-io/undertow/blob/4995d33afcc9b4b3358a0d2beefbde2d315dd0d8/examples/src/main/java/io/undertow/examples/security/basic/BasicAuthServer.java
    private HttpHandler addSecurity(final HttpHandler toWrap, final IdentityManager identityManager) {
        HttpHandler handler = toWrap;
        handler = new AuthenticationCallHandler(handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.singletonList(new BasicAuthenticationMechanism("My Realm"));
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
        return handler;
    }
}
