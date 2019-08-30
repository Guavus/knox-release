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
# If this is an erase and not an upgrade
#if [ "$1" = "0" ]; then
    #/usr/bin/rm -rf /var/log/knox
    #/usr/bin/rm -rf /var/run/knox
    #/usr/bin/rm -rf %{home_dir}
    #/usr/bin/rm -rf %{ext_dir}
    #/usr/bin/rm -rf /usr/hdp/3.1.0.0-78/knox
    #/usr/sbin/userdel --force knox 2> /dev/null; true
    #/usr/sbin/groupdel knox 2> /dev/null; true
#fi


##############################
