#!/bin/bash
INSTALL_DIR=/usr/share/imageingest
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk.x86_64

cd $INSTALL_DIR
yum install -y unzip
unzip jp2k-deps.zip
cd jp2k-deps && unzip v8_0_2-01968E.zip
cd v8_0_2-01968E/make
cp Makefile-Linux-x86-64-gcc Makefile
yum install -y gcc gcc-c++
make &> $INSTALL_DIR/jp2k-deps/kakadu_make.out
if [ $? -eq 0 ]
then
	mkdir $INSTALL_DIR/kak-build-libs
	cp $INSTALL_DIR/jp2k-deps/v8_0_2-01968E/lib/Linux-x86-64-gcc/*.so $INSTALL_DIR/kak-build-libs
	echo "Kakadu Built successfully. Installing other dependencies to \$JAVA_HOME"
	export LD_LIBRARY_PATH=$INSTALL_DIR/kak-build-libs
	cp $INSTALL_DIR/jp2k-deps/jars/* $JAVA_HOME/jre/lib/ext
else
	echo "Error: Kakadu Build Failed"
fi