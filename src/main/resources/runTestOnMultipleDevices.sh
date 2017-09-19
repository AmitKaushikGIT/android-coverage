#!/bin/bash
devices=( $(adb devices | grep "device$" | awk '{ print $1}') )
testApk=$1
targetApk=$2
appPackageName=$3
testPackageName=$4
testRunner=$5
targetCoveragePath=$6
pids=()
for (( i=0; i<${#devices[@]}; i++ ))
do
   adb -s ${devices[i]} root
   echo "Installing test and app apks in ${devices[i]}"
   adb -s ${devices[i]} install -r $testApk
   adb -s ${devices[i]} install -r $targetApk
done
echo "Running tests on ${#devices[@]} devices"
for (( i=0; i<${#devices[@]}; i++ ))
do
   adb -s ${devices[i]} shell am instrument -w -e coverage true -e emma true -e numShards ${#devices[@]} -e shardIndex $i  ${testPackageName}/${testRunner} > ${devices[i]}.log 2>&1 & disown
   pids[$i]=$!
   echo ${pids[$i]}
done
for (( i=0; i<${#pids[@]}; i++ ))
do
   echo "waiting for process ${pids[i]} to finish"
   ps -p ${pids[i]} > /dev/null
   while [ $? != 1 ]
   do
      ps -p ${pids[i]} > /dev/null
   done
   echo "process finished pulling coverage file"
   adb -s ${devices[i]}  pull "/data/data/${appPackageName}/files/coverage.ec" "${targetCoveragePath}${devices[i]}.ec"
done
