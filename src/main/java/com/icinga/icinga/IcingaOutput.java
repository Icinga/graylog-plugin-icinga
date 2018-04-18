package com.icinga.icinga;

import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang.text.StrSubstitutor;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.net.ssl.*;
import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IcingaOutput implements MessageOutput {
    protected static final String CK_ICINGA_ENDPOINTS = "icinga_endpoints";
    protected static final String CK_ICINGA_USER = "icinga_user";
    protected static final String CK_ICINGA_PASSWD = "icinga_passwd";
    protected static final String CK_ICINGA_HOST_NAME = "icinga_host_name";
    protected static final String CK_ICINGA_SERVICE_NAME = "icinga_service_name";
    protected static final String CK_CREATE_OBJECT = "create_object";
    protected static final String CK_OBJECT_TEMPLATES = "object_templates";
    protected static final String CK_OBJECT_ATTRIBUTES = "object_attributes";
    protected static final String CK_VERIFY_SSL = "verify_ssl";
    protected static final String CK_SSL_CA_PEM = "ssl_ca_pem";


    protected static final Logger LOG = LoggerFactory.getLogger(IcingaOutput.class);
    protected final AtomicBoolean isRunning = new AtomicBoolean(false);
    protected Configuration configuration;

    @Inject
    public IcingaOutput(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        this.configuration = configuration;
        isRunning.set(true);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }
    }

    protected String resolveConfigField(String configField, Message message) {
        return (new StrSubstitutor(message.getFields())).replace(configuration.getString(configField));
    }

    protected IcingaHTTPResponse sendRequest(String method, String relativeURL, Map<String, String> params, Map<String, String> headers, String body) throws Exception {
        List<String> paramStrings = new LinkedList<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            paramStrings.add(URLEncoder.encode(param.getKey(), "UTF-8") + "=" + URLEncoder.encode(param.getValue(), "UTF-8"));
        }

        relativeURL = "/v1/" + relativeURL + "?" + String.join("&", paramStrings);

        headers = new TreeMap<>(headers);

        SSLSocketFactory socketFactory = null;
        HostnameVerifier hostnameVerifier = null;

        if (!configuration.getBoolean(CK_VERIFY_SSL)) {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(
                    null,
                    new TrustManager[] {new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }},
                    new SecureRandom()
            );

            socketFactory = sc.getSocketFactory();
            hostnameVerifier = (s, ss) -> true;
        } else if (configuration.stringIsSet(CK_SSL_CA_PEM)) {
            String caCert = configuration.getString(CK_SSL_CA_PEM);
            InputStream caCertStream = new ByteArrayInputStream(caCert.getBytes(StandardCharsets.UTF_8));

            Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(caCertStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", cert);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

            socketFactory = sc.getSocketFactory();
        }

        String authorization = configuration.getString(CK_ICINGA_USER) + ":" + configuration.getString(CK_ICINGA_PASSWD);
        String authorizationBase64 = Base64.getEncoder().encodeToString(authorization.getBytes());

        headers.put("Authorization", "Basic " + authorizationBase64);
        headers.put("Accept", "application/json");

        for (String endpoint : configuration.getList(CK_ICINGA_ENDPOINTS)) {
            try {
                URL url = new URL("https://" + endpoint + relativeURL);

                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                if (socketFactory != null) {
                    con.setSSLSocketFactory(socketFactory);
                }

                if (hostnameVerifier != null) {
                    con.setHostnameVerifier(hostnameVerifier);
                }

                con.setRequestMethod(method);

                for (Map.Entry<String, String> header : headers.entrySet()) {
                    con.setRequestProperty(header.getKey(), header.getValue());
                }

                con.setDoOutput(true);
                DataOutputStream out = new DataOutputStream(con.getOutputStream());
                out.writeBytes(body);
                out.flush();
                out.close();

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                con.disconnect();

                Map<String, String> responseHeaders = new TreeMap<>();

                for (Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
                    if (entry.getKey() != null) {
                        responseHeaders.put(entry.getKey(), String.join(",", entry.getValue()));
                    }
                }


                return new IcingaHTTPResponse(con.getResponseCode(), responseHeaders, content.toString());
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                LOG.error(stringWriter.toString());
            }
        }

        return null;
    }

    @Override
    public void stop() {
        isRunning.set(false);
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            configurationRequest.addField(new ListField(
                    CK_ICINGA_ENDPOINTS, "Icinga Endpoints",
                    Collections.emptyList(), Collections.emptyMap(),
                    "Endpoints of your Icinga 2 instances in format \"HOST:PORT\"",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    ListField.Attribute.ALLOW_CREATE
            ));

            configurationRequest.addField(new TextField(
                    CK_ICINGA_USER, "Icinga User", "",
                    "User of your Icinga 2 API",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            configurationRequest.addField(new TextField(
                    CK_ICINGA_PASSWD, "Icinga Password", "",
                    "Password of your Icinga 2 API",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    TextField.Attribute.IS_PASSWORD
            ));

            configurationRequest.addField(new TextField(
                    CK_ICINGA_HOST_NAME, "Icinga Host Name", "",
                    "Icinga host to use for results",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            configurationRequest.addField(new TextField(
                    CK_ICINGA_SERVICE_NAME, "Icinga Service Name", "",
                    "Icinga service to use for results",
                    ConfigurationField.Optional.OPTIONAL
            ));

            configurationRequest.addField(new BooleanField(
                    CK_CREATE_OBJECT, "Create object", false,
                    "Create Icinga object if missing (Service if given, host otherwise)"
            ));

            configurationRequest.addField(new ListField(
                    CK_OBJECT_TEMPLATES, "Object Templates",
                    Collections.emptyList(), Collections.emptyMap(),
                    "Templates to create the object from",
                    ConfigurationField.Optional.OPTIONAL,
                    ListField.Attribute.ALLOW_CREATE
            ));

            configurationRequest.addField(new ListField(
                    CK_OBJECT_ATTRIBUTES, "Object Attributes",
                    Collections.emptyList(), Collections.emptyMap(),
                    "Attributes to set while creating an object (Format: ATTR=VALUE)",
                    ConfigurationField.Optional.OPTIONAL,
                    ListField.Attribute.ALLOW_CREATE
            ));

            configurationRequest.addField(new BooleanField(
                    CK_VERIFY_SSL, "Verify SSL", true,
                    "Verify the SSL certificates"
            ));

            configurationRequest.addField(new TextField(
                    CK_SSL_CA_PEM, "SSL CA", "",
                    "SSL CA Certificate (PEM)",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.TEXTAREA
            ));

            return configurationRequest;
        }
    }
}