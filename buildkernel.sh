make silentoldconfig
genkernel --menuconfig --lvm --mdadm --makeopts="-j9 -l8" all
emerge -v @module-rebuild
