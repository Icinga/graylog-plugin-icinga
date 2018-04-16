package com.icinga.icinga;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcingaOutput implements MessageOutput{
    private static final String CK_ICINGA_HOST = "icinga_host";
    private static final String CK_ICINGA_PORT = "icinga_port";
    private static final String CK_ICINGA_USER = "icinga_user";
    private static final String CK_ICINGA_PASSWD = "icinga_passwd";
    private static final Logger LOG = LoggerFactory.getLogger(IcingaOutput.class);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Configuration configuration;

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
    public void write(Message message) throws Exception {
        //TODO find workaround
        SSLUtilities.trustAllHostnames();
        SSLUtilities.trustAllHttpsCertificates();

        URL url = new URL("https://" + configuration.getString(CK_ICINGA_USER) + ":" + configuration.getString(CK_ICINGA_PASSWD) + "@" + configuration.getString(CK_ICINGA_HOST) + ":" + configuration.getInt(CK_ICINGA_PORT) + "/v1/status");

        LOG.info(url.toString());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
            for (String line; (line = reader.readLine()) != null;) {
                LOG.info(line);
            }
        }
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }
    }

    @Override
    public void stop() {
        isRunning.set(false);
    }

    public interface Factory extends MessageOutput.Factory<IcingaOutput> {
        @Override
        IcingaOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            configurationRequest.addField(new TextField(
                    CK_ICINGA_HOST, "Icinga Host", "",
                    "Hostname of your Icinga 2 instance",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new NumberField(
                    CK_ICINGA_PORT, "Icinga Port", 5665,
                    "Port of your Icinga 2 API",
                    ConfigurationField.Optional.OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                    CK_ICINGA_USER, "Icinga User", "",
                    "User of your Icinga 2 API",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                    CK_ICINGA_PASSWD, "Icinga Password", "",
                    "Password of your Icinga 2 API",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Output", false, "", "An output plugin sending Icinga 2 check results");
        }
    }
}