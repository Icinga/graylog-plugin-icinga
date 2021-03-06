# Icinga Plugin for Graylog

An output plugin for integrating [Icinga 2](https://www.icinga.com/products/icinga-2/) with Graylog.

**Required Graylog version:** 2.0 and later

Installation
------------

[Download the plugin](https://www.google.com/search?q=to+be+done)
and place the `.jar` file in your Graylog plugin directory. The plugin directory
is the `plugins/` folder relative from your `graylog-server` directory by default
and can be configured in your `graylog.conf` file.

Restart `graylog-server` and you are done.

Development
-----------

This project is using Maven 3 and requires Java 8 or higher.

* Clone this repository.
* Run `mvn package` to build a JAR file.
* Optional: Run `mvn jdeb:jdeb` and `mvn rpm:rpm` to create a DEB and RPM package respectively.
* Copy generated JAR file in target directory to your Graylog plugin directory.
* Restart Graylog.

Getting started
---------------

After installation configure one or more of the outputs provided by this plugin:

* Process Check Result
* Send Custom Notification
* Add Comment
* Remove Comments
* Schedule Downtime
* Remove Downtimes

For their configuration options read the [detailed manual](https://www.icinga.com/docs/graylog-plugin/latest).

Plugin Release
--------------

We are using the maven release plugin:

```
$ mvn release:prepare
[...]
$ mvn release:perform
```

This sets the version numbers, creates a tag and pushes to GitHub. Travis CI will build the release artifacts and upload to GitHub automatically.