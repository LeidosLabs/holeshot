@REM
set ANALYTICS_API_KEY=FILL_ME_IN
set TILESERVER_URL=https://tileserver.leidoslabs.com/tileserver
set INPUT_LOG_LOCATION=%HOLESHOT%\analytics\log-mapreduce\src\test\*

spark-submit --class "com.leidoslabs.holeshot.analytics.emr.JobMain" --master local[2] %HOLESHOT%\analytics\log-mapreduce\target\log-mapreduce-0.2.jar