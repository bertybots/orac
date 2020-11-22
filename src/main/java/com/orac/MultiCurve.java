package com.orac;

import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.loader.csv.QuotesCsvLoader;
import com.opengamma.strata.loader.csv.RatesCalibrationCsvLoader;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.curve.CalibrationMeasures;
import com.opengamma.strata.pricer.curve.RatesCurveCalibrator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapTradePricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;

public class MultiCurve {
    /*
     * Reference data contains calendar. Here we use build-in holiday calendar. It
     * is possible to override them with customized versions.
     */
    private static final ReferenceData REF_DATA = ReferenceData.standard();

    private static final LocalDate VALUATION_DATE = LocalDate.of(2021, 11, 22);

    private static final String CONFIG_STR = "EUR-DSCONOIS-E3BS-E6IRS";
    private static final CurveGroupName CONFIG_NAME = CurveGroupName.of(CONFIG_STR);

    /* Swap description. */
    private static final Period SWAP_TENOR = Period.ofYears(7);
    private static final Period SWAP_PERIOD_TO_START = Period.ofMonths(3);
    private static final double SWAP_COUPON = 0.025;
    private static final double SWAP_NOTIONAL = 10_000_000;

    /* Files utilities */
    private static final String SUFFIX_CSV = ".csv";
    private static final String GROUPS_SUFFIX = "-group";
    private static final String NODES_SUFFIX = "-nodes";
    private static final String SETTINGS_SUFFIX = "-settings";
    private static final String QUOTES_SUFFIX = "-quotes";

    private static final ResourceLocator GROUP_RESOURCE = ResourceLocator.of(CONFIG_STR + GROUPS_SUFFIX + SUFFIX_CSV);
    private static final ResourceLocator SETTINGS_RESOURCE = ResourceLocator.of(CONFIG_STR + SETTINGS_SUFFIX + SUFFIX_CSV);
    private static final ResourceLocator NODES_RESOURCE = ResourceLocator.of(CONFIG_STR + NODES_SUFFIX + SUFFIX_CSV);

    /* Raw data */
    private static final String QUOTES_FILE = CONFIG_STR + QUOTES_SUFFIX + SUFFIX_CSV;

    private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.PAR_SPREAD;
    private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);

    private static final DiscountingSwapTradePricer PRICER_SWAP = DiscountingSwapTradePricer.DEFAULT;
    private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

    private static final double BP1 = 1.0E-4; // Scaling by 1 bp.

    public static void main(String[] arg) {
        MultiCurve mc = new MultiCurve();
        mc.loadCurveDefiniations();
        mc.loadStaticMarketData();
        //mc.liveMD();
        mc.calibrate();
        mc.valueSwap();
    }
    
    Map<CurveGroupName, RatesCurveGroupDefinition> configs;
    ImmutableMarketData quotes;
    ImmutableRatesProvider curves;

    public void loadCurveDefiniations() {
        configs = RatesCalibrationCsvLoader.load(GROUP_RESOURCE, SETTINGS_RESOURCE, NODES_RESOURCE);
    }

    public void loadStaticMarketData() {
        Map<QuoteId, Double> MAP_MQ = QuotesCsvLoader.load(VALUATION_DATE, ResourceLocator.of(QUOTES_FILE));
        quotes = ImmutableMarketData.builder(VALUATION_DATE).values(MAP_MQ).build();
    }

    public void setLiveMD(ImmutableMap<QuoteId, Double> map) {
        quotes = ImmutableMarketData.builder(VALUATION_DATE).values(map).build();
    }

    public void calibrate() {
        long lastTime = System.currentTimeMillis();
        curves = CALIBRATOR.calibrate(configs.get(CONFIG_NAME), quotes, REF_DATA);
        System.out.println("Calibration Time: " + (System.currentTimeMillis() - lastTime));
    }

    public void valueSwap() {
        ResolvedSwapTrade swap = EUR_FIXED_1Y_EURIBOR_3M.createTrade(VALUATION_DATE, SWAP_PERIOD_TO_START,
                Tenor.of(SWAP_TENOR), BuySell.BUY, SWAP_NOTIONAL, SWAP_COUPON, REF_DATA).resolve(REF_DATA);

        /* Computes PV and bucketed PV01 */
        MultiCurrencyAmount pv = PRICER_SWAP.presentValue(swap, curves);
        PointSensitivities pts = PRICER_SWAP.presentValueSensitivity(swap, curves);
        CurrencyParameterSensitivities ps = curves.parameterSensitivity(pts);
        CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, curves);

        /* Export to csv files. */
        //ExportUtils.export(mqs, BP1, CONFIG_STR + "-delta" + SUFFIX_CSV);
        //ExportUtils.export(pv, CONFIG_STR + "-pv" + SUFFIX_CSV);
    }
}
