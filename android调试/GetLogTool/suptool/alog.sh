#!/bin/sh
#----------------------------------------------------------------------------
#  Author:lezh  2016/11/10
#pull logs to user path
#
#----------------------------------------------------------------------------

#the path is always exist
#		contains log massage above
#   bugreport  --  dumpsys  --  dmesg  --  dumstate  --  logcat

  mkdir /data/logs 2 > /dev/null
  chmod 777 /data/logs
  bugreport > "/data/logs/bugreport.txt"
  chmod 777 "/data/logs/bugreport.txt"
  echo "bugreport copy success!"
  dumpsys > "/data/logs/dumpsys.txt"
  chmod 777 "/data/logs/dumpsys.txt"
  echo "dumpsys copy success!"
  dmesg > "/data/logs/dmesg.txt"
  chmod 777 "/data/logs/dmesg.txt"
  echo "dmesg copy success!"
  dumpstate > "/data/logs/dumpstate.txt"
  chmod 777 "/data/logs/dumpstate.txt"
  echo "dumpstate copy success!"
	logcat -d -v threadtime > "/data/logs/logcat.txt"
  chmod 777 "/data/logs/logcat.txt"
  echo "logcat copy success!"
  
#the path exist when error occured
#		contains log massage above
#    trace  --  tombstone

trace="/data/anr/traces.txt"
tombstone="/data/tombstones/"
if [ -e $trace ]
then
  echo " ANR has occured! "
  cat $trace > "/data/logs/trace.txt"
  chmod 777 "/data/logs/trace.txt"
  echo "trace copy success!"
fi

if [ -e $tombstone ]
then 
  echo " TombStone occured! "
  cp -aRf $tombstone /data/logs/tombstones
  chmod 777 "/data/logs/tombstones"
  echo "tombstone copy success!"
fi

