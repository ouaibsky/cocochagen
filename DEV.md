##Java 17

### upload to nexus

Due to nexus plugin deo, set this option before deploying

> export MAVEN_OPTS="--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
> --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.desktop/java.awt.font=ALL-UNNAMED"

Then:

    > ./mvnw deploy

