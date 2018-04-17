package com.icinga.icinga;

import com.google.inject.assistedinject.Assisted;
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

public class IcingaOutputAddComment extends IcingaOutput {
    private static final String CK_COMMENT_AUTHOR = "comment_author";
    private static final String CK_COMMENT = "comment";

    @Inject
    public IcingaOutputAddComment(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        super(configuration);

    }

    @Override
    public void write(Message message) throws Exception {
        LOG.info(message.toString());

        Map<String, String> params = new TreeMap<>();

        if (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME)) {
            params.put("type", "Service");
            params.put("service", configuration.getString(CK_ICINGA_HOST_NAME) + "!" + configuration.getString(CK_ICINGA_SERVICE_NAME));
        } else {
            params.put("type", "Host");
            params.put("host", configuration.getString(CK_ICINGA_HOST_NAME));
        }

        params.put("author", configuration.getString(CK_COMMENT_AUTHOR));
        params.put("comment", configuration.getString(CK_COMMENT));

        IcingaHTTPResponse response = sendRequest("POST", "actions/add-comment", params, Collections.emptyMap(), "");

        LOG.info(response.getBody());
    }

    public interface Factory extends MessageOutput.Factory<IcingaOutputAddComment> {
        @Override
        IcingaOutputAddComment create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends IcingaOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest baseRequest = super.getRequestedConfiguration();

            //TODO add stuff;

            baseRequest.addField(new TextField(
                    CK_COMMENT_AUTHOR, "Comment Author", "",
                    "Author of the comment",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new TextField(
                    CK_COMMENT, "Comment", "",
                    "The comment itself",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));


            return baseRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Add Comment Output", false, "", "An output plugin adding comments to Icinga 2 objects");
        }
    }
}
