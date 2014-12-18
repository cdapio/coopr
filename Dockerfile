# Copyright © 2012-2014 Cask Data, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Cask is a trademark of Cask Data, Inc. All rights reserved.
###############################################################################################
# Please visit Docker.com and follow instructions to download Docker SW in your environment.
# This Dockerfile will build a Coopr image from scratch utilizing ubuntu 12.04 as a base image.
# The assumption is that you are running this from the root of the coopr directory structure.
#
FROM ubuntu:12.04
MAINTAINER Cask Data <ops@cask.co>
RUN apt-get update && \
    apt-get install -y software-properties-common python-software-properties && \
    add-apt-repository ppa:chris-lea/node.js && \
    apt-get update && \
    apt-get install -y curl nodejs maven unzip ruby1.9.3 libxml2-dev libxslt-dev build-essential && \
    apt-get install -y --no-install-recommends openjdk-7-jdk

RUN gem install fog --version 1.21.0 && gem install sinatra --version 1.4.5 && \
    gem install thin --version 1.6.2 && gem install rest_client --version 1.7.3 && \
    gem install google-api-client --version 0.7.1
 
# create Software directory
RUN mkdir /Build /Software

# copy the minimum needed software (to build it) to the container
COPY *xml /Build/
COPY coopr-cli /Build/coopr-cli
COPY coopr-provisioner /Build/coopr-provisioner
COPY coopr-server /Build/coopr-server
COPY coopr-standalone /Build/coopr-standalone
COPY coopr-templates /Build/coopr-templates
COPY coopr-ui /Build/coopr-ui

# build coopr-standalone zip file, copy it to container and extract it
RUN cd Build/coopr-standalone && \
    MAVEN_OPTS="-Xmx512m" mvn clean package assembly:single -DskipTests && \
    unzip target/coopr-[0-9]*.[0-9]*.[0-9]*-standalone.zip -d /Software && \
    cd /Software && \
    rm -rf /Build /root/.m2 /var/cache/debconf/*-old /usr/share/{doc,man} \
    /usr/share/locale/{a,b,c,d,e{l,o,s,t,u},f,g,h,i,j,k,lt,lv,m,n,o,p,r,s,t,u,v,w,x,z}*

# Expose Ports (8100 for UI, 55054 for API)
EXPOSE 8100
EXPOSE 55054

# Clean UP (reduce space usage of container as much as possible)
RUN apt-get purge -y maven unzip && \
    apt-get autoclean && \
    apt-get -y autoremove 

# start CDAP in the background and tail in the foreground
ENTRYPOINT /Software/coopr-[0-9]*.[0-9]*.[0-9]*-standalone/bin/coopr.sh start && \
    /usr/bin/tail -F /Software/coopr-[0-9]*.[0-9]*.[0-9]*-standalone/logs/*.log
