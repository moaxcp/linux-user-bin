#!/bin/bash
cd /usr/share/consolefonts
for i in *
do
  setfont
  echo "testing >> $i << font"
  setfont $i
  showconsolefont
  sleep 5
  clear
done
