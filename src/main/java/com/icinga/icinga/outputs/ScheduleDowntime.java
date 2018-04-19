package com.icinga.icinga.outputs;

import com.google.inject.assistedinject.Assisted;
import com.icinga.icinga.IcingaOutput;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class ScheduleDowntime extends IcingaOutput {
    private static final String CK_DOWNTIME_AUTHOR = "downtime_author";
    private static final String CK_DOWNTIME_COMMENT = "downtime_comment";
    private static final String CK_DOWNTIME_DURATION = "downtime_duration";
    private static final String CK_DOWNTIME_TRIGGER_NAME = "downtime_trigger_name";
    private static final String CK_DOWNTIME_CHILD_OPTIONS = "downtime_child_options";

    @Inject
    public ScheduleDowntime(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        super(configuration);
    }

    @Override
    public void write(Message message) throws Exception {
        LOG.info(message.toString());

        Map<String, String> params = new TreeMap<>();

        if (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME)) {
            params.put("type", "Service");
            params.put("service", resolveConfigField(CK_ICINGA_HOST_NAME, message) + "!" + resolveConfigField(CK_ICINGA_SERVICE_NAME, message));
        } else {
            params.put("type", "Host");
            params.put("host", resolveConfigField(CK_ICINGA_HOST_NAME, message));
        }

        Object timestamp = message.getField("timestamp");
        long start = (timestamp instanceof DateTime ? (DateTime)timestamp : new DateTime()).getMillis() / 1000;
        JsonObjectBuilder bodyJson = Json.createObjectBuilder();

        bodyJson.add("author", resolveConfigField(CK_DOWNTIME_AUTHOR, message));
        bodyJson.add("comment", resolveConfigField(CK_DOWNTIME_COMMENT, message));
        bodyJson.add("start_time", start);
        bodyJson.add("end_time", start + configuration.getInt(CK_DOWNTIME_DURATION));
        bodyJson.add("trigger_name", resolveConfigField(CK_DOWNTIME_TRIGGER_NAME, message));
        bodyJson.add("child_options", configuration.getInt(CK_DOWNTIME_CHILD_OPTIONS));

        String body = bodyJson.build().toString();
        HttpResponse response = sendRequest(new HttpPost(), "actions/schedule-downtime", params, Collections.emptyMap(), body);

        if (response.getStatusLine().getStatusCode() == 404 && configuration.getBoolean(CK_CREATE_OBJECT)) {
            LOG.debug("Icinga object "
                + configuration.getString(CK_ICINGA_HOST_NAME)
                + (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME) ? "!" + configuration.getString(CK_ICINGA_SERVICE_NAME) : "")
                + " could not be found. Trying to create it."
            );
            response = createIcingaObject(message);
            if (response.getStatusLine().getStatusCode() == 200) {
                response = sendRequest(new HttpPost(), "actions/schedule-downtime", params, Collections.emptyMap(), body);
            } else {
                LOG.debug("Could not create Icinga object while scheduling a downtime: " + response);
                return;
            }
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.debug("Could not schedule downtime: " + response);
        }
    }

    public interface Factory extends MessageOutput.Factory<ScheduleDowntime> {
        @Override
        ScheduleDowntime create(Stream stream, Configuration configuration);

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
                    CK_DOWNTIME_AUTHOR, "Downtime Author", "graylog",
                    "Author of the downtime",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new TextField(
                    CK_DOWNTIME_COMMENT, "Downtime Comment", "",
                    "Comment of the downtime",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new NumberField(
                    CK_DOWNTIME_DURATION, "Downtime Duration", 0,
                    "Duration of the downtime in seconds",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new TextField(
                    CK_DOWNTIME_TRIGGER_NAME, "Downtime Trigger Name", "",
                    "Trigger of the downtime",
                    ConfigurationField.Optional.OPTIONAL
            ));

            baseRequest.addField(new NumberField(
                    CK_DOWNTIME_CHILD_OPTIONS, "Downtime Child Options", 0,
                    "Child options of the downtime",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            addObjectCreationOptions(baseRequest);

            return baseRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Schedule Downtime Output", false, "", "An output plugin scheduling downtimes of Icinga 2 objects");
        }
    }
}
