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
import java.util.*;

public class AddComment extends IcingaOutput {
    private static final String CK_COMMENT_AUTHOR = "comment_author";
    private static final String CK_COMMENT = "comment";

    @Inject
    public AddComment(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
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

        params.put("author", resolveConfigField(CK_COMMENT_AUTHOR, message));
        params.put("comment", resolveConfigField(CK_COMMENT, message));

        HttpResponse response = sendRequest(new HttpPost(), "actions/add-comment", params, Collections.emptyMap(), "");

        if (response.getStatusLine().getStatusCode() == 404 && configuration.getBoolean(CK_CREATE_OBJECT)) {
            LOG.debug("Icinga object "
                    + configuration.getString(CK_ICINGA_HOST_NAME)
                    + (configuration.stringIsSet(CK_ICINGA_SERVICE_NAME) ? "!" + configuration.getString(CK_ICINGA_SERVICE_NAME) : "")
                    + " could not be found. Trying to create it."
            );
            response = createIcingaObject(message);
            if (response.getStatusLine().getStatusCode() == 200) {
                response = sendRequest(new HttpPost(), "actions/add-comment", params, Collections.emptyMap(), "");
            } else {
                LOG.error("Could not create Icinga object while adding a comment: " + response.toString());
                return;
            }
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("Could not add comment: " + response.toString());
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
            ArrayList<ConfigurationField> configurationFields = getDefaultConfigFields(true);

            configurationFields.add(new TextField(
                    CK_COMMENT_AUTHOR, "Comment Author", "graylog",
                    "Author of the comment (may contain field macros)",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            configurationFields.add(new TextField(
                    CK_COMMENT, "Comment", "",
                    "The comment itself (may contain field macros)",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));


            final ConfigurationRequest configurationRequest = new ConfigurationRequest();
            configurationRequest.addFields(configurationFields);
            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Add Comment Output", false, "", "An output plugin adding comments to Icinga 2 objects");
        }
    }
}
