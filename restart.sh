#!/bin/bash
#git pull
kill -9 $(cat target/universal/sudobot-1.0/RUNNING_PID )
rm -r target/universal/sudobot-1.0
sbt dist
unzip target/universal/sudobot-1.0.zip
mv tradehubbot-1.0 target/universal/sudobot-1.0
cd target/universal/
nohup tradehubbot-1.0/bin/sudobot -Dhttps.port=9002 -Dhttps.port=9003 -J-Xms2240M -J-Xmx16000M -Dconfig.resource=production.conf -J-server&
tail -f --lines=100 nohup.out