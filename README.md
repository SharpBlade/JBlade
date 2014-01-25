JBlade
======

A Java wrapper/implementation for the SwitchBlade UI API
---------------------------------------------------------

Built using the SwitchBlade UI SDK from Razer.

Based on the [SharpBlade][sharpblade] library.

**This library is under development and not currently usable (as of 2014-01-24).**

Contributing
------------

Contributors are very welcome! If you got code fixes, please [submit a pull request][newpull] here on GitHub.

If you want to join the development team, please contact [Sharparam][sharp] on GitHub.

All authors and contributors are listed in the **AUTHORS** file.

Please read the wiki page about [contributing][contrib] before submitting pull requests.

License
-------

Copyright &copy; 2014 by [Adam Hellberg][sharp] and [Brandon Scott][bs].

This project is licensed under the MIT license, please see the file **LICENSE** for more information.

Images in **res/images** are created by Graham Hough.

Razer is a trademark and/or a registered trademark of Razer USA Ltd.
All other trademarks are property of their respective owners.

This project is in no way endorsed, sponsored or approved by Razer.

Dependencies
------------

JBlade depends on the SwitchBlade UI SDK (RzSwitchbladeSDK2.dll).

SwitchBlade UI SDK is provided by Razer and [can be obtained from their website][rzdev].

JBlade depends on the [JNA][jna] and [jna-platform][jnaplatform] libraries, both version 4.0.0.
JNA and the platform library can be found in Maven repositories
(net.java.dev.jna:jna:4.0.0 and net.java.dev.jna:jna-platform:4.0.0).

JBlade depends on the [log4j][] library, version 2.0-beta9.
log4j can be found in Maven repositories
(org.apache.logging.log4j:log4j-api:2.0-beta9 and org.apache.logging.log4j:log4j-core:2.0-beta9).

Debugging / Logging
-------------------

log4j (the logging library that JBlade uses) outputs log information to the standard output stream by default,
if it fails to detect any log4j config information in the classpath.

If you want log4j to output to a file, you can put the following in your classpath
(working directory of the application should work):

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<Configuration status="warn" name="JBlade">
  <Appenders>
    <File name="File" fileName="app.log" append="false"> <!-- Change filename if desired -->
      <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n" />
    </File>
  </Appenders>
  <Loggers>
    <Root level="info"> <!-- Change "info" to "debug" to make logging more verbose -->
      <AppenderRef ref="File" />
    </root>
  </Loggers>
</Configuration>
```

This will cause log output to be saved in a file named "app.log" in the same directory as the application.

Projects
--------

Current projects utilizing this or modified versions of this library:

 * *None*

(If you want your project listed, just contact [Sharparam][sharp] or [Brandon][bs])

[sharpblade]: https://github.com/SharpBlade/SharpBlade
[newpull]: ../../pull/new/master
[sharp]: https://github.com/Sharparam
[contrib]: ../../wiki/Contributing
[bs]: https://github.com/brandonscott
[rzdev]: http://www.razerzone.com/switchblade-ui/developers
[jna]: https://github.com/twall/jna
[jnaplatform]: https://github.com/twall/jna/blob/master/www/PlatformLibrary.md
[log4j]: http://logging.apache.org/log4j/2.x/index.html
