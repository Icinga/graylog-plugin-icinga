package com.icinga.icinga;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class IcingaMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "com.icinga.graylog-plugin-icinga/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "com.icinga.icinga.IcingaPlugin";
    }

    @Override
    public String getName() {
        return "Icinga";
    }

    @Override
    public String getAuthor() {
        return "Icinga Development Team <info@icinga.com>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://icinga.com");
    }

    @Override
    public Version getVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(0, 9, 0, "unknown"));
    }

    @Override
    public String getDescription() {
        return "Icinga 2 output plugin";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
