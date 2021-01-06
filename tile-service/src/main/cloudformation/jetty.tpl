#!/bin/sh
#  /etc/init.d/jetty

### BEGIN INIT INFO
# chkconfig:         234 90 20
# Provides:          jetty
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Description:       Jetty Tile Service
### END INIT INFO

NAME="jetty"
DESC="Jetty Tile Server"

# The path to Jsvc
EXEC="/usr/bin/jsvc"

# The path to the folder containing image-ingest.jar
FILE_PATH="/usr/share/$NAME"

# The path to the folder containing the java runtime
JAVA_HOME="/usr/lib/jvm/jre"

# Our classpath including our jar
CLASS_PATH="$FILE_PATH/bin/tileserver.jar"

# The fully qualified name of the class to execute
CLASS="com.leidoslabs.holeshot.tileserver.TileServer"

JETTY_PERCENT_MEM=80
JETTY_MEM_IN_MB="$(( $(free -m | awk '/^Mem/ { print $7; }') * $JETTY_PERCENT_MEM/100 ))m"
JETTY_PERCENT_CORES=100
JETTY_CORES=$(( $(grep -c ^processor /proc/cpuinfo) * $JETTY_PERCENT_CORES/100))

# Any command line arguments to be passed to the our Java Daemon implementations init() method
ARGS="-Dlog_dir=/var/log/$NAME -Dlog4j.configuration=$FILE_PATH/bin/log4j.properties -Dleidos.catalog.url={{CatalogURL}} -Xmx$JETTY_MEM_IN_MB -Djava.util.concurrent.ForkJoinPool.common.parallelism=$JETTY_CORES -Dfile.encoding=UTF-8"

# The file that will contain our process identification number (pid) for other scripts/programs that need to access it.
PID="/var/run/$NAME.pid"

# System.out writes to this file...
LOG_OUT="/var/log/$NAME/$NAME.out"

# System.err writes to this file...
LOG_ERR="/var/log/$NAME/$NAME.err"

# Source the init function library
. /etc/rc.d/init.d/functions

jsvc_exec()
{
    cd $FILE_PATH
    $EXEC -home $JAVA_HOME -cp $CLASS_PATH -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $PID $1 $ARGS $CLASS --tilebucket={{TileBucketName}} --tilebucketregion={{TileBucketRegion}}
}

case "$1" in
    start)
        echo "Starting the $DESC..."

        # Start the service
        jsvc_exec

        echo "The $DESC has started."
    ;;
    stop)
        echo "Stopping the $DESC..."

        # Stop the service
        jsvc_exec "-stop"

        echo "The $DESC has stopped."
    ;;
    status)
      status $NAME
        exit $?
    ;;
    restart)
        if [ -f "$PID" ]; then

            echo "Restarting the $DESC..."

            # Stop the service
            jsvc_exec "-stop"

            # Start the service
            jsvc_exec

            echo "The $DESC has restarted."
        else
            echo "Daemon not running, no action taken"
            exit 1
        fi
            ;;
    *)
    echo "Usage: /etc/init.d/$NAME {start|stop|status|restart}" >&2
    exit 3
    ;;
esac