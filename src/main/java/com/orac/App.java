package com.orac;

import io.socket.client.*;

import java.time.LocalDate;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.DataListener;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Hello world!
 *
 */
public class App {
    static final int PORT = 9292;
    static SocketIOServer server;

    public static Swap swap() {
        SwapLeg payLeg = RateCalculationSwapLeg.builder().payReceive(PayReceive.PAY)
                .accrualSchedule(PeriodicSchedule.builder().startDate(LocalDate.of(2014, 9, 12))
                        .endDate(LocalDate.of(2021, 9, 12)).frequency(Frequency.P6M)
                        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendarIds.USNY))
                        .build())
                .paymentSchedule(PaymentSchedule.builder().paymentFrequency(Frequency.P6M)
                        .paymentDateOffset(DaysAdjustment.NONE).build())
                .notionalSchedule(NotionalSchedule.of(Currency.USD, 100_000_000))
                .calculation(FixedRateCalculation.of(0.015, DayCounts.THIRTY_U_360)).build();

        SwapLeg receiveLeg = RateCalculationSwapLeg.builder().payReceive(PayReceive.RECEIVE)
                .accrualSchedule(PeriodicSchedule.builder().startDate(LocalDate.of(2014, 9, 12))
                        .endDate(LocalDate.of(2021, 9, 12)).frequency(Frequency.P3M)
                        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendarIds.USNY))
                        .build())
                .paymentSchedule(PaymentSchedule.builder().paymentFrequency(Frequency.P3M)
                        .paymentDateOffset(DaysAdjustment.NONE).build())
                .notionalSchedule(NotionalSchedule.of(Currency.USD, 100_000_000))
                .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M)).build();

        return Swap.of(payLeg, receiveLeg);
    }

    public static void main(String[] args) {
        System.out.println("Starting Server");

        Swap swap = swap();

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
    }

    public static void runServer() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(PORT);
        server = new SocketIOServer(config);
        server.addEventListener("toServer", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                client.sendEvent("toClient", "message from server");
            }
        });
        server.start();
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        server.stop();
    }
}
