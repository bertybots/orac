Build Jar before check in

mvn clean compile assembly:single
cp .\target\CurveBuilder-1.0-SNAPSHOT-jar-with-dependencies.jar CurveBuilder.jar

Run Manually

java -cp CurveBuilder.jar com.orac.SwapPricer

Run in pm2

pm2 start curve-builder.json