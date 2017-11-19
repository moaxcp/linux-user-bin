make silentoldconfig
genkernel --menuconfig --luks --lvm --mdadm --makeopts="-j9 -l8" all
emerge -v @module-rebuild
