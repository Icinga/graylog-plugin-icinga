package com.icinga.icinga.outputs;

import com.google.inject.assistedinject.Assisted;
import com.icinga.icinga.IcingaOutput;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.ListField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import java.util.*;

public class ProcessCheckResult extends IcingaOutput {
    private static final String CK_EXIT_CODE = "exit_code";
    private static final String CK_OUTPUT = "output";
    private static final String CK_PERF_DATA = "perf_data";

    @Inject
    public ProcessCheckResult(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
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

        params.put("exit_status", resolveConfigField(CK_EXIT_CODE, message));
        params.put("plugin_output", resolveConfigField(CK_OUTPUT, message));

        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        JsonArrayBuilder perfData = Json.createArrayBuilder();
        for (String metric : resolveConfigList(CK_PERF_DATA, message)) {
            perfData.add(metric);
        }

        jsonData.add("performance_data", perfData);

        HttpResponse response = sendRequest(new HttpPost(), "actions/process-check-result", params, Collections.emptyMap(), jsonData.build().toString());

        if (response.getStatusLine().getStatusCode() == 404 && configuration.getBoolean(CK_CREATE_OBJECT)) {
            response = createIcingaObject(message);
            if (response.getStatusLine().getStatusCode() == 200) {
                response = sendRequest(new HttpPost(), "actions/process-check-result", params, Collections.emptyMap(), jsonData.build().toString());
            } else {
                LOG.error("Could not create Icinga object while processing a check result: " + response.toString());
                return;
            }
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            LOG.error("Could not process check result: " + response.toString());
        }
    }

    public interface Factory extends MessageOutput.Factory<ProcessCheckResult> {
        @Override
        ProcessCheckResult create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends IcingaOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            ConfigurationRequest baseRequest = super.getRequestedConfiguration();

            Map<String, String> exitCodes = new TreeMap<>();
            exitCodes.put("0", "0");
            exitCodes.put("1", "1");
            exitCodes.put("2", "2");
            exitCodes.put("3", "3");

            baseRequest.addField(new DropdownField(
                    CK_EXIT_CODE, "Plugin Exit Code", "",
                    exitCodes,
                    "Exit code of this check",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new TextField(
                    CK_OUTPUT, "Plugin Output", "",
                    "Output of this check result",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            baseRequest.addField(new ListField(
                    CK_PERF_DATA, "Plugin Performance Data",
                    Collections.emptyList(), Collections.emptyMap(),
                    "Performance data for this check result \"KEY=VALUE\"",
                    ConfigurationField.Optional.NOT_OPTIONAL,
                    ListField.Attribute.ALLOW_CREATE
            ));

            addObjectCreationOptions(baseRequest);

            return baseRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Icinga Process Check Result Output", false, "", "An output plugin adding check results to Icinga 2 objects");
        }
    }
}
