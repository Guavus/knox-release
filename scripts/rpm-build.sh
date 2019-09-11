#!/bin/sh
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

#set -e 

#pushd "$(dirname "$0")"

TARGET_DIR="target"
POM_VERSION=$1
RPM_VERSION=$(echo $POM_VERSION | awk -F'-' '{print $1}' | sed 's/3.1.0/3.1.0.0/g' | awk 'BEGIN{FS=OFS="."}{NF--; print}')
REL_VERSION=$(echo $POM_VERSION | sed 's/1.0.0.//g' | awk -F'-' '{print $1}' | sed 's/3.1.0/3.1.0.0/g' | awk 'BEGIN{FS=OFS="."}{NF--; print}')
REL_NUMBER=$(echo $POM_VERSION | sed 's/1.0.0.//g' | awk -F'-' '{print $1}' | sed 's/3.1.0/3.1.0.0/g' | awk -F'.' '{print $NF}')
#################################
#
# POM_VERSTION = 1.0.0.3.1.0.78-4
# RPM_VERSION = 1.0.0.3.1.0.0
# REL_VERSION = 3.1.0.0
# REL_NUMBER = 78
#
#################################

TEMP_PACKAGE_DIR="target/dists"
RPM_DIR="target/dists/rpm"

extract_tar() {
   mkdir -p ${TEMP_PACKAGE_DIR}
   tar xzf ${TARGET_DIR}/${POM_VERSION}/knox-${POM_VERSION}.tar.gz -C ${TEMP_PACKAGE_DIR}/
}

create_directory_structure() {
   mkdir -p ${RPM_DIR}

   mkdir -p ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/etc/knox/conf
   mkdir -p ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/etc/rc.d/init.d/
   cp -r ${TEMP_PACKAGE_DIR}/knox-${POM_VERSION}/conf/* ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/etc/knox/conf/
   cp -r ${TEMP_PACKAGE_DIR}/knox-${POM_VERSION}/{bin,CHANGES,dep,ext,ISSUES,lib,LICENSE,NOTICE,README,samples,templates} ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/
   ln -fs /etc/knox/conf ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/conf
   ln -fs /var/lib/knox/data-3.1.0.0-78 ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/data
   ln -fs /var/log/knox ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/logs
   ln -fs /var/run/knox ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/pids
   cp scripts/knox-gateway-server ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/etc/rc.d/init.d/ && chmod +x ${RPM_DIR}/usr/hdp/${REL_VERSION}-${REL_NUMBER}/knox/etc/rc.d/init.d/knox-gateway-server

   mkdir -p ${RPM_DIR}/var/lib/knox/data-${REL_VERSION}-${REL_NUMBER}/deployments ${RPM_DIR}/var/lib/knox/data-${REL_VERSION}-${REL_NUMBER}/security
   chmod 700 ${RPM_DIR}/var/lib/knox/data-${REL_VERSION}-${REL_NUMBER}/security
   cp -r ${TEMP_PACKAGE_DIR}/knox-${POM_VERSION}/data/{applications,services} ${RPM_DIR}/var/lib/knox/data-${REL_VERSION}-${REL_NUMBER}/
}

jar_names_update() {
   for i in `find ${RPM_DIR} -name "*${POM_VERSION}*"`
   do
      oldFile=${i}
      newFile=$(echo ${oldFile} | sed "s/${POM_VERSION}/${RPM_VERSION}-${REL_NUMBER}/g")
      mv ${oldFile} ${newFile}
   done
}

generate_rpm() {
   PACKAGE_NAME=$(echo "knox.${REL_VERSION}.${REL_NUMBER}" | sed 's/\./_/g')
   fpm -s dir -t rpm  -d '/usr/bin/env' -d '/bin/bash' -d 'rpmlib(FileDigests) <= 4.6.0-1' -d 'rpmlib(PayloadIsXz) <= 5.2-1' -d 'config(knox_3_1_0_0_78) = 1.0.0.3.1.0.0-78' -d 'ranger_3_1_0_0_78-knox-plugin' -d 'hdp-select >= 3.1.0.0-78' --before-install scripts/pre-install.sh --after-install scripts/post-install.sh --before-remove scripts/pre-uninstall.sh --after-remove scripts/post-uninstall.sh -v ${RPM_VERSION} --iteration ${REL_NUMBER}_${BUILD_NUMBER} -a noarch -C ${RPM_DIR} -p ${RPM_DIR} -n ${PACKAGE_NAME} usr/ var/
}

cleanup() {
   rm -rf ${TEMP_PACKAGE_DIR}
   rm -rf ${RPM_DIR}
}

cleanup
extract_tar
create_directory_structure
jar_names_update
generate_rpm
