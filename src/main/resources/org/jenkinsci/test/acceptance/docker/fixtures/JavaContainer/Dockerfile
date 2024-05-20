#
# Container for running Java processes
#

# sha1sum ../SshdContainer/Dockerfile | cut -c 1-12
FROM jenkins/sshd:23e678bc2c56

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
        software-properties-common \
        openjdk-17-jdk-headless \
        curl \
        ant \
        maven
