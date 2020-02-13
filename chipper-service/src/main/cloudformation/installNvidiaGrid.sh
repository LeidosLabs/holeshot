#!/bin/bash

/usr/bin/yum install -y gcc kernel-devel-$(/usr/bin/uname -r)

/usr/bin/cat << EOF | sudo tee --append /etc/modprobe.d/blacklist.conf
blacklist vga16fb
blacklist nouveau
blacklist rivafb
blacklist nvidiafb
blacklist rivatv
EOF


/usr/bin/cat >> /etc/default/grub <<EOF
GRUB_CMDLINE_LINUX="rdblacklist=nouveau"
EOF


/usr/sbin/grub2-mkconfig -o /boot/grub2/grub.cfg

export INSTALLDIR=/root/nvidia
/usr/bin/mkdir -p $INSTALLDIR
/usr/bin/aws s3 cp --recursive s3://ec2-linux-nvidia-drivers/latest/ $INSTALLDIR/.

/bin/sh $INSTALLDIR/NVIDIA-Linux-x86_64*.run -s
