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
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class AddComment extends IcingaOutput {
    private static final String CK_COMMENT_AUTHOR = "comment_author";
    private static final String CK_COMMENT = "comment";

    @Inject
    public AddComment(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
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

        params.put("author", resolveConfigField(CK_COMMENT_AUTHOR, message));
        params.put("comment", resolveConfigField(CK_COMMENT, message));

        HttpResponse response = sendRequest(new HttpPost(), "actions/add-comment", params, Collections.emptyMap(), "");

        if (response.getStatusLine().getStatusCode() == 404) {
            createIcingaObject(message);
            response = sendRequest(new HttpPost(), "actions/add-comment", params, Collections.emptyMap(), "");

            if (response.getStatusLine().getStatusCode() == 404) {
                LOG.error("Could not add comment: " + response.toString());
            }
        }
    }

    public interface Factory extends MessageOutput.Factory<AddComment> {
        @Override
        AddComment create(Stream stream, Configuration configuration);

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
                    CK_COMMENT_AUTHOR, "Comment Author", "graylog",
                    "Author of the comment",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new TextField(
                    CK_COMMENT, "Comment", "",
                    "The comment itself",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            addObjectCreationOptions(baseRequest);

            return baseRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Add Comment Output", false, "", "An output plugin adding comments to Icinga 2 objects");
        }
    }
}
