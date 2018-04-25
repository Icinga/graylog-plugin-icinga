const PluginWebpackConfig = require('graylog-web-plugin').PluginWebpackConfig;
const loadBuildConfig = require('graylog-web-plugin').loadBuildConfig;
const path = require('path');

module.exports = new PluginWebpackConfig('com.icinga.icinga.IcingaPlugin', loadBuildConfig(path.resolve(__dirname, './build.config')), {
});
