/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.certificates.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zowe.apiml.HttpClient;
import org.zowe.apiml.SSLContextFactory;
import org.zowe.apiml.Stores;
import org.zowe.apiml.certificates.ZoweConfiguration;
import org.zowe.apiml.certificates.service.CommandExecutor;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class StoresController {
    private final Stores stores;
    private final ZoweConfiguration configuration;

    @GetMapping("/trusted-certs")
    public Map<Object, Object> getListOfTrustedCerts() throws KeyStoreException {

        return stores.getListOfCertificates().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> ((X509Certificate) entry.getValue()).getSubjectDN().toString()));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUrl(@RequestParam("url") String url) throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContextFactory factory = SSLContextFactory.initSSLContextWithoutKeystore(stores);
        HttpClient httpClient = new HttpClient(factory.getSslContext());
        try {
            httpClient.getConnection(new URL("https://" + url));
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.I_AM_A_TEAPOT);
        }

    }

    @GetMapping("/verify-zosmf")
    public ResponseEntity<String> verifyZosmf() throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return verifyUrl(String.format("https://%s:%s/zosmf/", configuration.getZosmfHost(), configuration.getZosmfPort()));
    }

    @GetMapping("/tso-cmd")
    public String executeTSOCmd() throws IOException {
        return CommandExecutor.execute();
    }

    @GetMapping("/certificate")
    public ResponseEntity<String> getCertificateInPemFormat(@RequestParam("url") String url) throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContextFactory factory = SSLContextFactory.initIgnoringSSLContext();
        HttpClient httpClient = new HttpClient(factory.getSslContext());
        Certificate[] certificates = httpClient.getCertificateChain(new URL("https://" + url));
        String certInPem = "-----BEGIN CERTIFICATE-----" + System.lineSeparator();
        certInPem += new String(Base64.getEncoder().encode(certificates[1].getEncoded())) + System.lineSeparator();
        certInPem += "-----END CERTIFICATE-----";
        return new ResponseEntity<>(certInPem, HttpStatus.OK);
    }

    /**
     * Add to the truststore certificate of specific service.
     *
     * @return
     */
    @PostMapping("/certificate")
    public ResponseEntity<?> addCertificate(
        @RequestParam("label") String label,
        @RequestParam("url") String url
    ) throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        SSLContextFactory factory = SSLContextFactory.initIgnoringSSLContext();
        HttpClient httpClient = new HttpClient(factory.getSslContext());
        Certificate[] certificates = httpClient.getCertificateChain(new URL("https://" + url));
        if (certificates != null && certificates.length > 1) {

            X509Certificate x509cert = (X509Certificate) certificates[1];
            stores.add(label, x509cert);

            return new ResponseEntity<>(x509cert.toString(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Add uploaded certificate under selected label to the given keyring.
     *
     * @return 200 when suceeded, 500 otherwise
     */
    @PostMapping("/certificate/upload")
    public ResponseEntity<?> addUploadedCertificate(@RequestParam("certificate") MultipartFile certificateFile,
                                                    @RequestParam("label") String label) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) cf.generateCertificate(certificateFile.getInputStream());
            stores.add(label, certificate);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (KeyStoreException | IOException | CertificateException e) {
            return new ResponseEntity<>("There was an issue with adding of the certificate to the truststore", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * The expectation is that the current truststore contains the label and that
     * the certificate is removed from the default truststore. If that's not the case
     * return 400 otherwise 200
     *
     * @return 200 when succeeds, 400 otherwise
     */
    @DeleteMapping("/certificate")
    public ResponseEntity<String> removeCertificate(String label) {
        try {
            stores.remove(label);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (KeyStoreException exception) {
            return new ResponseEntity<>("The certificate with provided label isn't part of the default truststore",
                HttpStatus.BAD_REQUEST);
        }
    }
}
