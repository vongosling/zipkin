![Zipkin (doc/zipkin-logo-200x119.jpg)](https://github.com/twitter/zipkin/raw/master/doc/zipkin-logo-200x119.jpg)

[Zipkin](http://twitter.github.com/zipkin) is a distributed tracing system that helps us gather timing data for all the disparate services at Twitter.

## Full documentation
See [http://twitter.github.com/zipkin](http://twitter.github.com/zipkin)

## Quick start

Zipkin provides three services:

 - To collect data: `bin/collector`
 - To extract data: `bin/query`
 - To display data: `bin/web`

You can run these services immediately after downloading Zipkin. Once all three
daemons are running, you should be able to visit http://localhost:8080 to view
the Zipkin UI. There is also a
[browser extension](https://github.com/twitter/zipkin/tree/master/zipkin-browser-extension)
which shows visualizations of traces of each page as you browse your website.

The next step is to collect trace data to view in Zipkin. To do this, interface
with the collector (e.g. by using Scribe) to record trace data. There are
several libraries to make this easier to do in different environments. Twitter
uses [Finagle](https://github.com/twitter/finagle/tree/master/finagle-zipkin);
external libraries (currently for Python, REST, node, and Java) are listed in the
[wiki](https://github.com/twitter/zipkin/wiki#external-projects-that-use-zipkin);
and there is also a [Ruby gem](https://rubygems.org/gems/finagle-thrift) and
[Ruby Thrift client](https://github.com/twitter/thrift_client).

See the [in-depth installation guide](https://github.com/twitter/zipkin/blob/master/doc/install.md)
for more information on larger / more complex Zipkin installations.

## Get involved

Check out the #zipkin IRC channel on chat.freenode.com to see if any
developers are there for questions or live debugging tips. Otherwise,
there are two mailing lists you can use to get in touch with other
users and developers.

Users: [https://groups.google.com/group/zipkin-user](https://groups.google.com/group/zipkin-user)

Developers: [https://groups.google.com/group/zipkin-dev](https://groups.google.com/group/zipkin-dev)

## Issues
Noticed a bug? [https://github.com/twitter/zipkin/issues](https://github.com/twitter/zipkin/issues)

## Contributing
See [CONTRIBUTING.md](https://github.com/twitter/zipkin/blob/master/CONTRIBUTING.md) for guidelines.

Areas where we'd love to see contributions: 

* adding tracing to more libraries and protocols
* interesting reports generated with Hadoop from the trace data
* extending collector to support more transports and storage systems
* trace data visualizations in the web UI

## Versioning
We use [SemVer](http://semver.org/) style versioning.

## License
Copyright 2012 Twitter, Inc.

Licensed under the Apache License, Version 2.0: [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

