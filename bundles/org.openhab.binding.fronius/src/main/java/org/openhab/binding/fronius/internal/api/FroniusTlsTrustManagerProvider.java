/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.fronius.internal.api;

import java.io.IOException;
import java.security.cert.CertificateException;

import javax.net.ssl.X509ExtendedTrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.net.http.PEMTrustManager;
import org.openhab.core.io.net.http.TlsTrustManagerProvider;

/**
 * Provides a TrustManager to allow secure connections to any Fronius device.
 * 
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
public class FroniusTlsTrustManagerProvider implements TlsTrustManagerProvider {

    private final String host;

    private final X509ExtendedTrustManager trustManager;

    public FroniusTlsTrustManagerProvider(String host) throws IOException, CertificateException {
        this.trustManager = PEMTrustManager.getInstanceFromServer("https://" + host);
        this.host = host;
    }

    @Override
    public String getHostName() {
        return this.host;
    }

    @Override
    public X509ExtendedTrustManager getTrustManager() {
        return this.trustManager;
    }
}
