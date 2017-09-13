#!/bin/sh
git diff-index --quiet HEAD -- || { echo "Uncommmitted changes detected- please run this script from a clean working directory."; exit; }
mvn clean package
mkdir -p target/orm/library
cp library.properties target/orm
mv target/orm.jar target/orm/library
mv target/dependency/* target/orm/library
cd target
zip -r orm.zip orm
