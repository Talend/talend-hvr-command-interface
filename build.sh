mvn validate
mvn test
mvn clean compile assembly:single

cp ./target/hvr-tcli-1.0.0-jar-with-dependencies.jar ./tcli.jar
cat stub.sh tcli.jar > tcli && chmod 777 tcli
~/launch4j/launch4j ./tcli.xml

cp tcli ~/IntelliJ/tcli/
cp tcli.exe ~/IntelliJ/tcli/