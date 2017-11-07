#
# Container for running Java processes
#

# cf. SshdContainer/Dockerfile
FROM jenkins/sshd:ff2527313f5c

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        software-properties-common=0.96.24.17 \
        openjdk-8-jdk=8u144-b01-2 \
        curl=7.55.1-1ubuntu2.1 \
        wget=1.19.1-3ubuntu1.1 \
        ant=1.9.9-4 \
        maven=3.5.0-6
