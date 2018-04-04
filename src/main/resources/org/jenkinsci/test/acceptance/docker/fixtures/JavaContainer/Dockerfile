#
# Container for running Java processes
#

# sha1sum ../SshdContainer/Dockerfile | cut -c 1-12
FROM jenkins/sshd:6c5ecc461e88

ENV JDK_VERSION 8u162-b12-0ubuntu0.17.10.2
RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        software-properties-common=0.96.24.17 \
        openjdk-8-jre-headless=$JDK_VERSION \
        openjdk-8-jdk-headless=$JDK_VERSION \
        curl \
        ant=1.9.9-4 \
        maven=3.5.0-6
