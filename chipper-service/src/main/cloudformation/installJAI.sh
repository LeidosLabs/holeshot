#!/bin/bash

cd /tmp

wget http://download.java.net/media/jai/builds/release/1_1_3/jai-1_1_3-lib-linux-amd64.tar.gz
gzip -dc /tmp/jai-1_1_3-lib-linux-amd64.tar.gz | tar -xvf -
mv -f /tmp/jai-1_1_3/lib/*.jar /etc/alternatives/jre/lib/ext/
mv -f /tmp/jai-1_1_3/lib/*.so /etc/alternatives/jre/lib/amd64/
rm -f /tmp/jai-1_1_3-lib-linux-amd64.tar.gz
rm -fR /tmp/jai-1_1_3

wget http://download.java.net/media/jai-imageio/builds/release/1.1/jai_imageio-1_1-lib-linux-amd64.tar.gz
gzip -dc /tmp/jai_imageio-1_1-lib-linux-amd64.tar.gz | tar -xvf -
mv -f /tmp/jai_imageio-1_1/lib/*.so /etc/alternatives/jre/lib/amd64
mv -f /tmp/jai_imageio-1_1/lib/*.jar /etc/alternatives/jre/lib/ext
rm -f /tmp/jai_imageio-1_1-lib-linux-amd64.tar.gz
rm -fR /tmp/jai_imageio-1_1
