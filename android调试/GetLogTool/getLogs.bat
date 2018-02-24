@title Get Logs
@echo off

:: get root permission
adb root
IF ERRORLEVEL 1 GOTO ERROR_ROOT
adb remount
IF ERRORLEVEL 1 GOTO ERROR_REMOUNT

:: push alog.sh to device
adb push .\suptool\alog.sh /data/
IF ERRORLEVEL 1 GOTO ERROR_PUSH

:: excute alog.sh in device
echo "--------------------------"
echo "It might spend long time."
echo "Please wait."
echo "--------------------------"
adb shell sh /data/alog.sh
IF ERRORLEVEL 1 GOTO ERROR_SH

:: pull logs from device
adb pull /data/logs/ .\
IF ERRORLEVEL 1 GOTO ERROR_PULL

:: parse bugreport by chkbugreport.jar
rename .\logs out_logs
java -jar .\suptool\chkbugreport-0.5-215.jar .\out_logs\bugreport.txt
IF ERRORLEVEL 1 GOTO ERROR_JAR

:: errors might occur
GOTO END
:ERROR_ROOT
echo "ERROR : get root permission error!"
GOTO END
:ERROR_REMOUNT
echo "ERROR : remount error!"
GOTO END
:ERROR_PUSH
echo "ERROR : push error!"
GOTO END
:ERROR_SH
echo "ERROR : sh file excute error!"
GOTO END
:ERROR_PUll
echo "ERROR : pull error!"
GOTO END
:ERROR_JAR
echo "ERROR : prase bugreport error!"
GOTO END
:END
PAUSE
