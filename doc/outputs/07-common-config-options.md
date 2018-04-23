# Common Config Options

In addition to output-specific options all outputs share the following common
config options for the Icinga 2 cluster, the target host/service and
auto-creation of hosts/services.

## Icinga 2 Cluster

The Icinga 2 cluster to interact with via
[the ReST API](https://www.icinga.com/docs/icinga2/latest/doc/12-icinga2-api/).

### Icinga Endpoints

One or more endpoints of the Icinga 2 cluster in one of the following formats:

* `DOMAIN:PORT`, e.g. `icinga.com:5665`
* `IPV4:PORT`, e.g. `192.0.2.1:5665`
* `[IPV6]:PORT`, e.g. `[2001:db8::1]:5665`

For every API request this plugin tries all endpoints in the specified order
until one returns a non-500 response.

### Icinga User and Icinga Password

The credentials for authentication against the API.
In case of multiple endpoints all of them must share that
[API user](https://www.icinga.com/docs/icinga2/latest/doc/09-object-types/#apiuser). 

### Verify SSL and SSL CA

In a production environment SSL verification should always be enabled to prevent
man-in-the-middle attacks. If enabled, the Icinga 2 root CA certificate must be
provided (as it's probably not part of the default certificate store).
That certificate can be copied from e.g. `/var/lib/icinga2/ca/ca.crt`.
In case of multiple endpoints all of them must share the same SSL PKI (if SSL
verification is enabled).

## Target Host/Service

All outputs target a specific host/service on every log message. Either a
service of a host (if both are given) or a host (if no service is given).

### Icinga Host Name

The target [host](https://www.icinga.com/docs/icinga2/latest/doc/09-object-types/#host)
name. May contain [message field macros](../03-field-macros.md).

### Icinga Service Name

The target [service](https://www.icinga.com/docs/icinga2/latest/doc/09-object-types/#service)
description. May contain [message field macros](../03-field-macros.md).

## Auto-Creation of Hosts/Services

All non-removing outputs support automatic creation of the target host/service
if it doesn't exist.

### Create object

Enable auto-creation of hosts/services.

### Object Templates

The [templates](https://www.icinga.com/docs/icinga2/latest/doc/17-language-reference/#template-imports)
a newly created host/service shall import. May contain [message field macros](../03-field-macros.md).

### Object Attributes

The attributes of a newly created host/service in the format `KEY=VALUE`. May
contain [message field macros](../03-field-macros.md).
