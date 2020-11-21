mvn clean compile assembly:single

java -cp CurveBuilder-1.0-SNAPSHOT-jar-with-dependencies.jar com.orac.SwapPricer

pm2 start curve-builder.json