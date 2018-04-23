# Process Check Result

Submits a passive check result for the target host/service. In addition to the
[common config options](02-common-config-options.md) there are the following
ones:

* Plugin Exit Code (0-3)
* Plugin Output
* Plugin Performance Data (format: `KEY=VALUE`)

For their meaning see the [Icinga 2 API documentation](https://www.icinga.com/docs/icinga2/latest/doc/12-icinga2-api/#process-check-result).
*Plugin Output* and *Plugin Performance Data* may contain
[message field macros](03-field-macros.md).
