# About

This Graylog output plugin integrates [Icinga 2](https://www.icinga.com/products/icinga-2/)
with [Graylog](https://www.graylog.org/) by executing an [Icinga 2 API](https://www.icinga.com/docs/icinga2/latest/doc/12-icinga2-api/)
action for every processed message. Available actions:

* [Process Check Result](outputs/01-process-check-result.md)
* [Send Custom Notification](outputs/02-send-custom-notification.md)
* [Add Comment](outputs/03-add-comment.md)
* [Remove Comments](outputs/04-remove-comments.md)
* [Schedule Downtime](outputs/05-schedule-downtime.md)
* [Remove Downtimes](outputs/06-remove-downtimes.md)

## Example Use Cases

### Process Check Result

When HTTPd logs an error message, submit a passive check result which changes
the respective service's status to critical.

Detects errors in constant time (instead of checking for them every five
minutes).

### Send Custom Notification

When SMTPd logs about suspicious client behavior, send a custom notification to
the responsible sysadmin.

Detects dangers in constant time (instead of being surprised).

### Add Comment

When the backup tool logs that a backup has been started, add a comment to the
respective host telling the sysadmin not to shut it down.

Prevents hosts from being rebooted due to fixes or updates during critical
operations.

### Remove Comments

When the backup tool logs that a backup has been finished, remove all comments
added by Graylog from the respective host.

Cleans up Graylog comments automatically.

### Schedule Downtime

When the backup tool logs that a backup has been started, schedule a downtime
for check\_load on the respective host.

Schedules downtimes automatically, but more flexible than
[Icinga 2's recurring downtimes](https://www.icinga.com/docs/icinga2/latest/doc/09-object-types/#objecttype-scheduleddowntime).

### Remove Downtimes

When the backup tool logs that a backup has been finished, remove all downtimes
scheduled by Graylog for check\_load on the respective host.

Enables automatic downtimes with flexible durations.

## Requirements

* Graylog: v2.0 (or later)
* Java: v8 (or later)

## Installation

[Download the plugin](https://www.google.com/search?q=to+be+done)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

## Getting Started

After installation configure one or more of the outputs provided by this plugin:

* [Process Check Result](outputs/01-process-check-result.md)
* [Send Custom Notification](outputs/02-send-custom-notification.md)
* [Add Comment](outputs/03-add-comment.md)
* [Remove Comments](outputs/04-remove-comments.md)
* [Schedule Downtime](outputs/05-schedule-downtime.md)
* [Remove Downtimes](outputs/06-remove-downtimes.md)

## Development

This project is using Maven 3.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart Graylog.

## Plugin Release

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI
will build the release artifacts and upload to GitHub automatically.
