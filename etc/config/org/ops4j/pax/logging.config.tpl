log4j.rootCategory="INFO, LOGFILE"
log4j.appender.CONSOLE.layout.ConversionPattern="%d{HH:mm:ss.SSS} - %-5p - [%t]  %-10c - %m%n"
log4j.appender.CONSOLE.layout="org.apache.log4j.PatternLayout"
log4j.appender.CONSOLE="org.apache.log4j.ConsoleAppender"
service.pid="org.ops4j.pax.logging"
log4j.logger.it="DEBUG, CONSOLE"
log4j.appender.LOGFILE.layout.ConversionPattern="%d - %-5p - [%t]  %c - %m%n"
log4j.appender.LOGFILE.layout="org.apache.log4j.PatternLayout"
log4j.appender.LOGFILE="org.apache.log4j.DailyRollingFileAppender"
org.ops4j.pax.logging.logback.config.file="_BASEDIR_/etc/logback.xml"
