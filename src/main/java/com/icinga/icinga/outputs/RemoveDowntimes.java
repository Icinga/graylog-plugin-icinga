package com.icinga.icinga.outputs;

import com.google.inject.assistedinject.Assisted;
import com.icinga.icinga.Icinga2Filter;
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
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class RemoveDowntimes extends IcingaOutput {
    private static final String CK_DOWNTIME_AUTHOR = "downtime_author";

    @Inject
    public RemoveDowntimes(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        super(configuration);

    }

    @Override
    public void write(Message message) throws Exception {
        StringBuilder filter = new StringBuilder();
        filter.append("downtime.author == ");
        filter.append(Icinga2Filter.quoteString(resolveConfigField(CK_DOWNTIME_AUTHOR, message)));
        filter.append(" && host.name == ");
        filter.append(Icinga2Filter.quoteString(resolveConfigField(CK_ICINGA_HOST_NAME, message)));

        if (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME)) {
            filter.append(" && service.name == ");
            filter.append(Icinga2Filter.quoteString(resolveConfigField(CK_ICINGA_SERVICE_NAME, message)));
        } else {
            filter.append(" && !service");
        }

        Map<String, String> params = new TreeMap<>();
        params.put("type", "Downtime");
        params.put("filter", filter.toString());

        HttpResponse response = sendRequest(new HttpPost(), "actions/remove-downtime", params, Collections.emptyMap(), "");
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
            ConfigurationRequest baseRequest = super.getRequestedConfiguration();

            baseRequest.addField(new TextField(
                    CK_DOWNTIME_AUTHOR, "Downtimes Author", "graylog",
                    "Author of the downtimes",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return baseRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Remove Downtimes Output", false, "", "An output plugin removing downtimes from Icinga 2 objects");
        }
    }
}
