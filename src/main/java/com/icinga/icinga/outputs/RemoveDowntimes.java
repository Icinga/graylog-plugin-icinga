package com.icinga.icinga.outputs;

import com.google.inject.assistedinject.Assisted;
import com.icinga.icinga.IcingaOutput;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.*;

public class RemoveDowntimes extends IcingaOutput {
    private static final String CK_DOWNTIME_AUTHOR = "downtime_author";

    @Inject
    public RemoveDowntimes(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        super(configuration);
    }

    @Override
    public void write(Message message) throws Exception {
        StringBuilder filter = new StringBuilder();
        JsonObjectBuilder filterVars = Json.createObjectBuilder();

        filter.append("downtime.author == fv_da && host.name == fv_hn");
        filterVars.add("fv_da", resolveConfigField(CK_DOWNTIME_AUTHOR, message));
        filterVars.add("fv_hn", resolveConfigField(CK_ICINGA_HOST_NAME, message));

        if (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME)) {
            filter.append(" && service.name == fv_sn");
            filterVars.add("fv_sn", resolveConfigField(CK_ICINGA_SERVICE_NAME, message));
        } else {
            filter.append(" && !service");
        }

        JsonObjectBuilder jsonBody = Json.createObjectBuilder();
        jsonBody.add("filter", filter.toString());
        jsonBody.add("filter_vars", filterVars);

        Map<String, String> params = new TreeMap<>();
        params.put("type", "Downtime");

        HttpResponse response = sendRequest(new HttpPost(), "actions/remove-downtime", params, Collections.emptyMap(), jsonBody.build().toString());

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("Could not remove downtime: " + response);
        }
    }

    public interface Factory extends MessageOutput.Factory<RemoveDowntimes> {
        @Override
        RemoveDowntimes create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends IcingaOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ArrayList<ConfigurationField> configurationFields = getDefaultConfigFields(false);

            configurationFields.add(new TextField(
                    CK_DOWNTIME_AUTHOR, "Downtimes Author", "graylog",
                    "Author of the downtimes (may contain field macros)",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            final ConfigurationRequest configurationRequest = new ConfigurationRequest();
            configurationRequest.addFields(configurationFields);
            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Remove Downtimes Output", false, "", "An output plugin removing downtimes from Icinga 2 objects");
        }
    }
}
