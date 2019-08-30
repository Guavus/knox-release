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
getent group knox 2>&1 > /dev/null || /usr/sbin/groupadd -r knox
getent group hadoop 2>&1 > /dev/null || /usr/sbin/groupadd -r hadoop
getent passwd knox 2>&1 > /dev/null || /usr/sbin/useradd -c "KNOX" -s /bin/bash -g knox -G hadoop -r -d /var/lib/knox knox 2> /dev/null || :

if [[ ! -e "/var/run/knox" ]]; then
        /usr/bin/install -d -o knox -g knox -m 0755  /var/run/knox
fi

if [[ ! -e "/var/log/knox" ]]; then
        /usr/bin/install -d -o knox -g knox -m 0755  /var/log/knox
fi

##############################
