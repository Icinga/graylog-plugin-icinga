package com.icinga.icinga.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

@JsonAutoDetect
@AutoValue
public abstract class IcingaPluginConfig {
    private static final Logger LOG = LoggerFactory.getLogger(IcingaPluginConfig.class);

    @JsonProperty("icinga_endpoints")
    public abstract List<String> icingaEndpoints();

    @JsonProperty("icinga_user")
    public abstract String icingaUser();

    @JsonProperty("icinga_passwd")
    public abstract String icingaPasswd();

    @JsonProperty("verify_ssl")
    public abstract boolean verifySsl();

    @JsonProperty("ssl_ca_pem")
    public abstract String sslCaPem();

    @JsonCreator
    public static IcingaPluginConfig create(@JsonProperty("icinga_endpoints") List<String> icingaEndpoints,
                                            @JsonProperty("icinga_user") String icingaUser,
                                            @JsonProperty("icinga_passwd") String icingaPasswd,
                                            @JsonProperty("verify_ssl") boolean verifySsl,
                                            @JsonProperty("ssl_ca_pem") String sslCaPem) {
        return builder()
            .icingaEndpoints(icingaEndpoints)
            .icingaUser(icingaUser)
            .icingaPasswd(icingaPasswd)
            .verifySsl(verifySsl)
            .sslCaPem(sslCaPem)
            .build();
    }

    public static IcingaPluginConfig createDefault() {
        return builder()
            .icingaEndpoints(new LinkedList<>())
            .icingaUser("")
            .icingaPasswd("")
            .verifySsl(true)
            .sslCaPem("")
            .build();
    }

    public static Builder builder() {
        return new AutoValue_IcingaPluginConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder icingaEndpoints(List<String> icingaEndpoints);

        public abstract Builder icingaUser(String icingaUser);

        public abstract Builder icingaPasswd(String icingaPasswd);

        public abstract Builder verifySsl(boolean verifySsl);

        public abstract Builder sslCaPem(String sslCaPem);

        public abstract IcingaPluginConfig build();
    }
}
