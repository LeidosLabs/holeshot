#!/bin/bash 

export currentDir="$(pwd)"
export scriptDir="$(dirname $0)"
cd $scriptDir

function buildModule {
   pushd $1
   shift
   $MAVEN_HOME/bin/mvn $*
   popd
}

buildModule ../../imageop $*
buildModule ../../photogrammetry $*
buildModule ../../leidos-geoint-services -N $*
buildModule ../../leidos-geoint-services $*

cd $currentDir
