/*
 * Copyright (c) 2019 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.service;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.nike.cerberus.metrics.CallbackLongGauge;
import com.nike.riposte.metrics.codahale.CodahaleMetricsCollector;
import com.signalfx.codahale.metrics.MetricBuilder;
import com.signalfx.codahale.metrics.SettableDoubleGauge;
import com.signalfx.codahale.metrics.SettableLongGauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Supplier;

/**
 * TODO this class, I gutted out the sfx logic
 */
@Component
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final CodahaleMetricsCollector metricsCollector;


    @Autowired
    public MetricsService() {
        metricsCollector = new CodahaleMetricsCollector();
    }

    /**
     * Find gauge with the given name and set it's value.
     *
     * Create a gauge with the given name if one does not already exist.
     *
     * @param name  Name of the gauge
     * @param value  Value of the gauge
     */
    public void setGaugeValue(String name, long value) {
        boolean isGaugeAlreadyRegistered = metricsCollector.getMetricRegistry().getGauges().containsKey(name);

        final SettableLongGauge gauge;
        try {
            gauge = isGaugeAlreadyRegistered ?
                (SettableLongGauge) metricsCollector.getMetricRegistry().getGauges().get(name) :
                metricsCollector.getMetricRegistry().register(name, new SettableLongGauge());

            gauge.setValue(value);
        } catch (IllegalArgumentException e) {
            log.error("Failed to get or create settable gauge, a non-gauge metric with name: {} is probably registered", name);
        }
    }

    public SettableDoubleGauge getOrCreateDoubleGauge(String metricName, Map<String, String> dimensions) {
        return getOrCreate(SettableDoubleGauge.Builder.INSTANCE, metricName, dimensions);
    }

    public SettableLongGauge getOrCreateLongGauge(String metricName, Map<String, String> dimensions) {
        return getOrCreate(SettableLongGauge.Builder.INSTANCE, metricName, dimensions);
    }

    public void setDoubleGaugeValue(String name, double value, Map<String, String> dimensions) {
        SettableDoubleGauge gauge = getOrCreateDoubleGauge(name, dimensions);
        gauge.setValue(value);
    }

    public void setLongGaugeValue(String name, long value, Map<String, String> dimensions) {
        SettableLongGauge gauge = getOrCreateLongGauge(name, dimensions);
        gauge.setValue(value);
    }

    public Counter getOrCreateCounter(String name, Map<String, String> dimensions) {
        return getOrCreate(MetricBuilder.COUNTERS, name, dimensions);
    }

    public Gauge getOrCreateLongCallbackGauge(String name, Supplier<Long> supplier, Map<String, String> dimensions) {
        return getOrCreate(CallbackLongGauge.Builder.getInstance(supplier), name, dimensions);
    }

    private <M extends Metric> M getOrCreate(MetricBuilder<M> builder, String metricName, Map<String, String> dimensions) {
        if (metricsCollector.getMetricRegistry().getMetrics().containsKey(metricName)) {
            return (M) metricsCollector.getMetricRegistry().getMetrics().get(metricName);
        } else {
            M metric = builder.newMetric();
            return metricsCollector.getMetricRegistry().register(metricName, metric);
        }
    }
}
