package com.icinga.icinga;

import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.json.*;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
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

    protected HttpResponse sendRequest(HttpRequestBase method, String relativeURL, Map<String, String> params, Map<String, String> headers, String body) throws Exception {
        List<String> paramStrings = new LinkedList<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            paramStrings.add(URLEncoder.encode(param.getKey(), "UTF-8") + "=" + URLEncoder.encode(param.getValue(), "UTF-8"));
        }

        relativeURL = "/v1/" + relativeURL + "?" + String.join("&", paramStrings);

        headers = new TreeMap<>(headers);

        SSLConnectionSocketFactory socketFactory = null;
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

            socketFactory = new SSLConnectionSocketFactory(sc);
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

            socketFactory = new SSLConnectionSocketFactory(sc);
        }

        String authorization = configuration.getString(CK_ICINGA_USER) + ":" + configuration.getString(CK_ICINGA_PASSWD);
        String authorizationBase64 = Base64.getEncoder().encodeToString(authorization.getBytes());

        headers.put("Authorization", "Basic " + authorizationBase64);
        headers.put("Accept", "application/json");

        HttpClient client = HttpClientBuilder.create().setSSLHostnameVerifier(hostnameVerifier).setSSLSocketFactory(socketFactory).build();

        for (String endpoint : configuration.getList(CK_ICINGA_ENDPOINTS)) {
            try {
                URI url = new URI("https://" + endpoint + relativeURL);

                method.setURI(url);

                for (Map.Entry<String, String> header : headers.entrySet()) {
                    method.addHeader(header.getKey(), header.getValue());
                }

                if (method instanceof HttpEntityEnclosingRequestBase) {
                    HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
                    ((HttpEntityEnclosingRequestBase) method).setEntity(entity);
                }

                HttpResponse response = client.execute(method);
                String result = EntityUtils.toString(response.getEntity());

                LOG.info(result);

                return response;
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                LOG.error(stringWriter.toString());
            }
        }

        return null;
    }

    boolean createIcingaObject() throws Exception {
        //curl -k -s -u root:icinga -H 'Accept: application/json' -X PUT 'https://localhost:5665/v1/objects/hosts/example.localdomain' \
        //-d '{ "templates": [ "generic-host" ], "attrs": { "address": "192.168.1.1", "check_command": "hostalive", "vars.os" : "Linux" } }' \

        JsonObjectBuilder jsonBody = Json.createObjectBuilder();
        JsonArrayBuilder templates = Json.createArrayBuilder();
        for (String template : configuration.getList(CK_OBJECT_TEMPLATES)) {
            templates.add(template);
        }

        jsonBody.add("templates", templates);

        JsonObjectBuilder attributes = Json.createObjectBuilder();
        for (String attribute : configuration.getList(CK_OBJECT_ATTRIBUTES)) {
            String[] parts = attribute.split("=");
            attributes.add(parts[0], parts[1]);
        }

        jsonBody.add("attrs", attributes);

        if (!configuration.stringIsSet(CK_ICINGA_SERVICE_NAME)) {
            HttpResponse response = sendRequest(new HttpPut(), "objects/hosts" + configuration.getString(CK_ICINGA_HOST_NAME), Collections.emptyMap(), Collections.emptyMap(), jsonBody.build().toString());
            LOG.info(response.toString());
        } else {

        }

        return true;
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