FROM discoenv/javabase

ADD target/monkey-standalone.jar /home/iplant/
ADD conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:monkey-standalone.jar", "monkey.core"]
CMD ["--help"]
