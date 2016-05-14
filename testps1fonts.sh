#!/bin/bash
cd /usr/share/consolefonts
for i in *
do
  setfont
  echo "testing >> $i << font"
  setfont $i
  echo -e "\0342\0234\0227"
  echo -e "\0342\0234\0223"
  read
  clear
done
