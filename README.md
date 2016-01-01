Artemis
=======
<a href="https://raw.githubusercontent.com/Groupon/artemis/master/LICENSE">
    <img src="https://img.shields.io/hexpm/l/plug.svg"
         alt="License: Apache 2">
</a>

Artemis is a deployment system for high visibility, easy configuration management and rapid deployment.
Artemis currently supports two deployment mechanisms: [Roller](https://github.com/groupon/roll) and
[Docker](https://www.docker.com/).
Roller is a deployment system created and open sourced by Groupon to solve the problem of repeatable
deployments.  Docker is a common platform for using Linux containers.


### Building

Prerequisites:
* [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [Play 2.4.3](http://www.playframework.com/download)

Building:

    artemis> ./activator stage

### Installing

The artifacts from the build are in *artemis/target/universal/stage* and should be copied to an appropriate directory on the Artemis host(s).

### Execution

In the installation's *bin* directory there are scripts to start Artemis: *artemis* (Linux/Mac) and *artemis.bat* (Windows).  One of these should be executed on system start with appropriate parameters.  For example:

    /usr/local/lib/artemis/bin/artemis -J-Xmn150m -J-XX:+UseG1GC -Dhttp.port=80 -Dpidfile.path=/usr/local/var/ARTEMIS_PID

Model
=====

Environments
------------
Artemis is based around the idea that deployments should be organized, repeatable, and audited.
The model starts with the concept of an environment.  An environment is merely an organizational
structure used to hold configuration and the actual units of deployment, stages.

Stages
------
Stages are the fundamental units of deployment.  Stages belong to environments and have a list of hostclasses
associated with them.  When deploying, a release is combined with the configuration of a stage and pushed to
the hosts in the set of hostclasses for that stage.

Releases
--------
A release is a set of packages and versions to be deployed somewhere.

Manifest
--------
A manifest is the combination of a release and rendered configuration applied to a stage.  A manifest is what is
actually deployed to a host.


Running the app
===============

`./activator run` to run in dev mode

`./activator start` to run in prod mode

Prerequisites
-------------
There are several things Artemis needs to be able to function properly:
1) A database: we encourage the use of PostgreSQL as the DBMS
2) An ssh private key: Artemis uses SSH to connect to hosts.  Key-based authentication is the only method supported
3) A roller config server: If you want to use roller, you need to provide a config server to vend the base configurations
4) A github server for authentication: You can use GitHub Enterprise or GitHub public

Configuration
-------------
See the [sample config](conf/application-base.conf) for available configuration options.

Aside from the JVM command line arguments, you may provide two additional configuration files. The first is the
[LogBack](http://logback.qos.ch/) configuration file.  To use a custom logging configuration simply add the following
argument to the command line above:

    -Dlogger.file=/usr/local/lib/artemis/logger.xml

Where */usr/local/lib/artemis/logger.xml* is the path to your logging configuration file. The included
[default logging configuration file](conf/logger.xml) is automatically applied if one is not specified. Please refer
to [LogBack](http://logback.qos.ch/) documentation for more information on how to author a configuration file.

The second configuration is for the application. To use a custom configuration simply add the following argument to the command line above:

    -Dconfig.file=/usr/local/lib/artemis/application.custom.conf

Where */usr/local/lib/artemis/application.custom.conf* is the path to your application configuration file.  The included
[default application configuration file](conf/application-base.conf) in the project documents and demonstrates many of the
configuration options available. To use the default application configuration file it needs to be specified on start-up:

    -Dconfig.resource=conf/application-base.conf

To author a custom application configuration it is recommended you inherit from the default application configuration file
and provide any desired configuration as overrides. Please refer to
[Play Framework](https://www.playframework.com/documentation/2.4.x/ProductionConfiguration) documentation for more information
on how to author a configuration file.

Extension
=========
Artemis intentionally uses a custom default application configuration and custom default routes specification. This allows projects
extending it to supplement functionality more easily with the standard default application configuration and routes. To use these files as
 extensions rather than replacements you should make the following changes.

First, add dependencies on Artemis code and assets in __project/Build.scala__:

    "com.groupon" %% "artemis" % "VERSION",
    "com.groupon" %% "artemis" % "VERSION" classifier "assets",

Second, your extending project's application configuration should include the custom default configuration in __conf/application.conf__:

    include "artemis.application.conf"

Third, your extending project's application configuration should restore the default router in __conf/application.conf__:

    application.router = null

Finally, your extending project's routes specification should include the custom default routes in __conf/routes__:

    -> / artemis.Routes

License
=======
Published under Apache Software License 2.0, see LICENSE

(c) Groupon, Inc., 2015
