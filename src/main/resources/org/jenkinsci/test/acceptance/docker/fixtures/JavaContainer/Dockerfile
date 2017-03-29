#
# Container for running jenkins.war
#

# cf. SshdContainer/Dockerfile
FROM jenkins/sshd:f2c9927f0a08

# JDK is from Universe
RUN echo deb http://archive.ubuntu.com/ubuntu precise universe >> /etc/apt/sources.list
RUN apt-get update -y && apt-get install --no-install-recommends -y software-properties-common
RUN add-apt-repository ppa:openjdk-r/ppa
RUN apt-get update
RUN apt-get install --no-install-recommends -y openjdk-8-jdk openjdk-7-jdk curl wget ant maven git
