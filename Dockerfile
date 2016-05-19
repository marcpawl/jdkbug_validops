FROM fedora:23
RUN dnf update  -y
RUN dnf install -y java-1.8.0-openjdk-devel 
RUN dnf install -y wget unzip
RUN cd /root && wget https://services.gradle.org/distributions/gradle-2.13-bin.zip
RUN cd /root && unzip gradle-2.13-bin.zip && rm gradle-2.13-bin.zip 
COPY build.gradle /root/build.gradle
COPY settings.gradle /root/settings.gradle
COPY src /root/src
WORKDIR /root
ENV JAVA_HOME=/usr/lib/jvm/java-openjdk/
RUN java -version
