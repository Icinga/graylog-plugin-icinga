# Schedule Downtime

Schedules a downtime for the target host/service. In addition to the
[common config options](07-common-config-options.md) there are the following
ones:

* Downtime Author
* Downtime Comment
* Downtime Duration
* Downtime Trigger Name
* Downtime Child Options

For their meaning (except *Downtime Duration*) see the
[Icinga 2 API documentation](https://www.icinga.com/docs/icinga2/latest/doc/12-icinga2-api/#schedule-downtime).
*Downtime Author*, *Downtime Comment* and *Downtime Trigger Name* may contain
[message field macros](../03-field-macros.md).

## Limitations

The scheduled downtimes are always fixed and start always at the message
timestamp. *Downtime Duration* specifies the duration in seconds.
