del .\bootstrap.jar
jar cvf0 ./bootstrap.jar -C ./target/classes ./server/Bootstrap.class -C ./target/classes ./server/classloader/CommonClassLoader.class
del .\lib\JavaServer-Alpha.jar
jar cvf0 ./lib/JavaServer-Alpha.jar -C ./target/classes .
java -cp ./bootstrap.jar server.Bootstrap