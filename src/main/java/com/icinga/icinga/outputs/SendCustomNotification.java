package com.icinga.icinga.outputs;

import com.google.inject.assistedinject.Assisted;
import com.icinga.icinga.IcingaOutput;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class SendCustomNotification extends IcingaOutput {
    private static final String CK_NOTIFICATION = "notification";
    private static final String CK_NOTIFICATION_AUTHOR = "notification_author";
    private static final String CK_NOTIFICATION_FORCE = "notification_force";

    @Inject
    public SendCustomNotification(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        super(configuration);
    }

    @Override
    public void write(Message message) throws Exception {
        Map<String, String> params = new TreeMap<>();

        if (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME)) {
            params.put("type", "Service");
            params.put("service", resolveConfigField(CK_ICINGA_HOST_NAME, message) + "!" + resolveConfigField(CK_ICINGA_SERVICE_NAME, message));
        } else {
            params.put("type", "Host");
            params.put("host", resolveConfigField(CK_ICINGA_HOST_NAME, message));
        }

        params.put("author", resolveConfigField(CK_NOTIFICATION_AUTHOR, message));
        params.put("comment", resolveConfigField(CK_NOTIFICATION, message));

        params.put("force", configuration.getBoolean(CK_NOTIFICATION_FORCE) ? "1" : "0");

        HttpResponse response = sendRequest(new HttpPost(), "actions/send-custom-notification", params, Collections.emptyMap(), "");

        if (response.getStatusLine().getStatusCode() == 404 && configuration.getBoolean(CK_CREATE_OBJECT)) {
            LOG.debug("Icinga object "
                    + configuration.getString(CK_ICINGA_HOST_NAME)
                    + (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME) ? "!" + configuration.getString(CK_ICINGA_SERVICE_NAME) : "")
                    + " could not be found. Trying to create it."
            );
            response = createIcingaObject(message);
            if (response.getStatusLine().getStatusCode() == 200) {
                response = sendRequest(new HttpPost(), "actions/send-custom-notification", params, Collections.emptyMap(), "");
            } else {
                LOG.debug("Could not create Icinga object while sending custom notification: " + response.toString());
                return;
            }
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.debug("Could not send custom notification: " + response.toString());
        }
    }

    public interface Factory extends MessageOutput.Factory<SendCustomNotification> {
        @Override
        SendCustomNotification create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends IcingaOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest baseRequest = super.getRequestedConfiguration();

            baseRequest.addField(new TextField(
                    CK_NOTIFICATION, "Notification", "",
                    "Notification text",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new TextField(
                    CK_NOTIFICATION_AUTHOR, "Notification Author", "graylog",
                    "The author of the notification",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new BooleanField(
                    CK_NOTIFICATION_FORCE, "Notification Force", true,
                    "Is this a forced notification?"
            ));

            addObjectCreationOptions(baseRequest);

            return baseRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Send Custom Notification Output", false, "", "An output plugin sending custom notifications via Icinga 2");
        }
    }
}
