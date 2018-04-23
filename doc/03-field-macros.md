# Field Macros

Some of the outputs' config fields may contain message field macros in the
format `${field_name}`. Such macros are replaced with the respective fields of
processed messages. E.g. `Created on ${timestamp}.` becomes
`Created on 2018-04-01T12:34:56.789Z.`.
