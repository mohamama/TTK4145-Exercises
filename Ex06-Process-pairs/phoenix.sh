#!/bin/bash

DELAY=1

# You need to make sure that this directory exists and that you have write access
STATEFILE=/var/run/phoenix/state
COMPFILE=/var/run/phoenix/backup$(date +%s)

echo "Phoenix started, assuming backup"
touch $COMPFILE
sleep $(( 2*DELAY ))

# First, check for output file and its age - while the main process is alive, wait
while [ $STATEFILE -nt $COMPFILE ];do
	touch $COMPFILE
	sleep $DELAY
done

# If nothing is found when checking N seconds after start, assume master and: spawn backup process, write to file (start where file left off
echo "No main process found, I'm taking over..."
touch $STATEFILE

# Start a new copy
gnome-terminal -x $0

# Read the existing state from the file
COUNT=$(cat $STATEFILE)
# Start main loop
while true;do
	sleep $DELAY
	(( COUNT++ ))
	# Overwrite the statefile with the new count value
	echo $COUNT > $STATEFILE
	echo "Count value: $COUNT"
done
