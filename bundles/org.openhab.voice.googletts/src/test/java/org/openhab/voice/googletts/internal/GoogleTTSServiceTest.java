/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.voice.googletts.internal;

import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.voice.tts.TTSCache;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests OAuth token cleanup for the Google TTS service.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
class GoogleTTSServiceTest {
    @Test
    void clearsStoredTokenOnlyAfterCredentialsAreCleared() {
        OAuthFactory oAuthFactory = mock(OAuthFactory.class);
        GoogleTTSService service = new GoogleTTSService(mock(ConfigurationAdmin.class), oAuthFactory,
                mock(TTSCache.class), Map.of());

        service.activate(Map.of());
        verify(oAuthFactory, never()).deleteServiceAndAccessToken(GoogleTTSService.SERVICE_PID);

        service.updateConfig(Map.of("clientId", "id"));
        verify(oAuthFactory, never()).deleteServiceAndAccessToken(GoogleTTSService.SERVICE_PID);

        service.updateConfig(Map.of());
        verify(oAuthFactory).deleteServiceAndAccessToken(GoogleTTSService.SERVICE_PID);
        service.dispose();
        verify(oAuthFactory, times(1)).deleteServiceAndAccessToken(GoogleTTSService.SERVICE_PID);
    }

    @Test
    void usesConfiguredRedirectUriForAuthorizationCodeExchange() throws Exception {
        verifyRedirectUri("http://127.0.0.1:8888", "http://127.0.0.1:8888");
    }

    @Test
    void defaultsToGoogleRedirectUriWhenNotConfigured() throws Exception {
        verifyRedirectUri(null, "https://www.google.com");
    }

    @SuppressWarnings("null")
    private void verifyRedirectUri(@Nullable String configuredRedirectUri, String expectedRedirectUri)
            throws Exception {
        OAuthFactory oAuthFactory = mock(OAuthFactory.class);
        OAuthClientService oAuthService = mock(OAuthClientService.class);
        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(oAuthFactory.createOAuthClientService(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyBoolean())).thenReturn(oAuthService);
        when(oAuthService.getAccessTokenResponseByAuthorizationCode("authorization-code", expectedRedirectUri))
                .thenReturn(tokenResponse);
        when(tokenResponse.getRefreshToken()).thenReturn("refresh-token");

        ConfigurationAdmin configAdmin = mock(ConfigurationAdmin.class);
        Configuration configuration = mock(Configuration.class);
        Dictionary<String, Object> properties = new Hashtable<>();
        when(configAdmin.getConfiguration(GoogleTTSService.SERVICE_PID)).thenReturn(configuration);
        when(configuration.getProperties()).thenReturn(properties);

        GoogleTTSService service = new GoogleTTSService(configAdmin, oAuthFactory, mock(TTSCache.class), Map.of());
        service.activate(Map.of());
        Map<String, Object> config = new HashMap<>(
                Map.of("clientId", "client-id", "clientSecret", "client-secret", "authcode", "authorization-code"));
        if (configuredRedirectUri != null) {
            config.put("redirectUri", configuredRedirectUri);
        }
        service.updateConfig(config);

        verify(oAuthService).getAccessTokenResponseByAuthorizationCode("authorization-code", expectedRedirectUri);
        service.dispose();
    }
}
