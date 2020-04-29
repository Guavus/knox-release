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

##############################
if [ !  -e "/etc/knox/conf" ]; then
    rm -f /etc/knox/conf
    mkdir -p /etc/knox/conf
    cp -rp  /usr/hdp/3.1.4.0-315/etc/knox/conf/* /etc/knox/conf
fi
/usr/bin/hdp-select --rpm-mode set knox-server 3.1.4.0-315
#if [ -e /usr/hdp/current//knox-server//etc/rc.d/init.d/knox-gateway-server  ]
#then
#    if ! readlink -e /etc/rc.d/init.d/knox-gateway-server     > /dev/null 2>&1
#    then
#        ln -sf   /usr/hdp/current//knox-server//etc/rc.d/init.d/knox-gateway-server    /etc/rc.d/init.d/knox-gateway-server
#    fi
#fi
#chkconfig --add  knox-gateway-server
#chkconfig   knox-gateway-server off

##############################
#alternatives --install /etc/knox/conf knox_3_1_4_0_315-conf /usr/hdp/3.1.4.0-315/etc/knox/conf 30
# Generate a random master secret.
# su -l knox -c "/usr/hdp/3.1.4.0-315/knox/bin/knoxcli.sh create-master --generate"
# Generate a self-signed SSL identity certificate.
# su -l knox -c "/usr/hdp/3.1.4.0-315/knox/bin/knoxcli.sh create-cert --hostname $(hostname -f)"

##############################
