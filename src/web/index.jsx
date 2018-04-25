import packageJson from '../../package.json';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import IcingaPluginConfig from 'components/IcingaPluginConfig';

PluginStore.register(new PluginManifest(packageJson, {
    systemConfigurations: [
        {
            component: IcingaPluginConfig,
            configType: 'com.icinga.icinga.config.IcingaPluginConfig',
        }
    ]
}));
