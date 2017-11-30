#
# Container for running Java processes
#

# cf. SshdContainer/Dockerfile
FROM jenkins/sshd:ff2527313f5c

ENV JDK_VERSION 8u144-b01-2
RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        software-properties-common=0.96.24.17 \
        openjdk-8-jre-headless=$JDK_VERSION \
        openjdk-8-jdk-headless=$JDK_VERSION \
        curl=7.55.1-1ubuntu2.2 \
        ant=1.9.9-4 \
        maven=3.5.0-6
