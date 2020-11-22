package com.orac;

import java.util.Iterator;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.*;

public class IoClient {
    public static void main(String[] args) {
        System.out.println("Starting Server");

        Thread ts = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runServer();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        ts.start();

        boolean running = true;
        while (running) {
            try {
                Thread.sleep(1000);
                System.out.println("Still here...");
            } catch (Exception ex) {
            }
        }
    }

    public static void runServer() {
        MultiCurve mc = new MultiCurve();
        mc.loadCurveDefiniations();

        try {
            var socket = IO.socket("http://localhost:8200");
            socket.on(Socket.EVENT_CONNECT, args -> {
                System.out.println("Connectd to Server");
                socket.emit("listen", "curveInputs");
            });
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                System.out.println("Disconnected from Server");
            });
            socket.on("curveInputs", args -> {
                System.out.println("Curve Build...");
                JSONObject jsonObject = (JSONObject) args[0];

                ImmutableMap.Builder<QuoteId, Double> builder = ImmutableMap.builder();

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    try {
                        JSONObject obj = (JSONObject) jsonObject.get(key);
                        String name = (String) obj.get("name");
                        Object inputValue = obj.get("value");
                        Double value;
                        if (inputValue instanceof Double) {
                            value = (Double)inputValue;
                        } else {
                            value = ((Integer)inputValue).doubleValue();
                        }
                        System.out.println("Got Name: " + name + ", Value: " + value);

                        StandardId id = StandardId.of("OG-Ticker", name);
                        builder.put(QuoteId.of(id, FieldName.MARKET_VALUE), value);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                mc.setLiveMD(builder.build());
                var curves = mc.calibrate();

                var dt = (System.currentTimeMillis() / 86_400_000.0) + 25569;

                JSONObject dfs = new JSONObject();
                if (curves != null) {
                    try {
                        ZeroRateDiscountFactors df = (ZeroRateDiscountFactors) curves.discountFactors(Currency.EUR);
                        for (int x = 1; x <= 50; x++) {
                            double y = df.getCurve().yValue(x);
                            String label = x + "Y";
                            JSONObject value = new JSONObject();
                            value.put("name", label);
                            value.put("value", y);
                            value.put("excelDate", dt);
                            value.put("username", "orac");
                            dfs.put(label, value);
                        }
                        System.out.println("Got some discount factors...");
                    } catch (JSONException e) {
                        System.out.println("Error getting factors: " + e.toString());
                    }
                }
                socket.emit("cooked", dfs);
                System.out.println("Curve Done.");
            });
            socket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
