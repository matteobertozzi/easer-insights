/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.matteobertozzi.easerinsights.aws.cloudwatch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.matteobertozzi.easerinsights.DatumBuffer;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferEntry;
import io.github.matteobertozzi.easerinsights.DatumBuffer.DatumBufferReader;
import io.github.matteobertozzi.easerinsights.DatumUnit;
import io.github.matteobertozzi.easerinsights.exporters.AbstractEaserInsightsDatumExporter;
import io.github.matteobertozzi.easerinsights.metrics.MetricCollector;
import io.github.matteobertozzi.easerinsights.metrics.collectors.MaxAvgTimeRangeGauge;
import io.github.matteobertozzi.easerinsights.metrics.collectors.TimeRangeCounter;
import io.github.matteobertozzi.easerinsights.util.ThreadUtil;
import io.github.matteobertozzi.easerinsights.util.TimeUtil;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;

public class AwsCloudWatchExporter extends AbstractEaserInsightsDatumExporter {
  private final MetricCollector cloudWatchPutTime = new MetricCollector.Builder()
    .unit(DatumUnit.NANOSECONDS)
    .name("aws_cloudwatch_exporter_put_metric_data_time")
    .label("CloudWatch Put Metric Data Time")
    .register(MaxAvgTimeRangeGauge.newSingleThreaded(60, 1, TimeUnit.MINUTES));

  private final MetricCollector cloudWatchPutCount = new MetricCollector.Builder()
    .unit(DatumUnit.COUNT)
    .name("aws_cloudwatch_exporter_put_metric_data_count")
    .label("CloudWatch Put Metric Data Count")
    .register(TimeRangeCounter.newSingleThreaded(60, 1, TimeUnit.MINUTES));

  private final MetricCollector cloudWatchPutFailed = new MetricCollector.Builder()
    .unit(DatumUnit.COUNT)
    .name("aws_cloudwatch_exporter_put_metric_data_failed")
    .label("CloudWatch Put Metric Data Failed")
    .register(TimeRangeCounter.newSingleThreaded(60, 1, TimeUnit.MINUTES));

  private static final Logger LOGGER = Logger.getLogger("AwsCloudWatchExporter");

  private final ArrayList<Dimension> dimensions = new ArrayList<>();
  private final CloudWatchClient cloudWatch;

  private String namespace;

  public AwsCloudWatchExporter(final CloudWatchClient cloudWatch) {
    this.cloudWatch = cloudWatch;
  }

  public static AwsCloudWatchExporter newAwsCloudWatchExporter() {
    return newAwsCloudWatchExporter(System.getenv("AWS_REGION"));
  }

  public static AwsCloudWatchExporter newAwsCloudWatchExporter(final String regionName) {
    return new AwsCloudWatchExporter(CloudWatchClient.builder().region(Region.of(regionName)).build());
  }

  public AwsCloudWatchExporter addDefaultDimension(final String name, final String value) {
    return addDefaultDimension(Dimension.builder().name(name).value(value).build());
  }

  public AwsCloudWatchExporter addDefaultDimension(final Dimension dimension) {
    this.dimensions.add(dimension);
    return this;
  }

  public AwsCloudWatchExporter setNamespace(final String namespace) {
    this.namespace = namespace;
    return this;
  }

  @Override
  public String name() {
    return "AWS-CloudWatch";
  }

  @Override
  public void close() throws IOException {
    super.close();
    ThreadUtil.ignoreException("cloudwatch", "closeing", cloudWatch::close);
  }

  @Override
  protected void datumBufferProcessor() {
    final int AWS_MAX_DATUM_BATCH_SIZE = 1000;
    final ArrayList<MetricDatum> datumBatch = new ArrayList<>(AWS_MAX_DATUM_BATCH_SIZE);
    final DatumBufferReader datumBufferReader = DatumBuffer.newReader();
    while (isRunning()) {
      processDatumBufferAsBatch(datumBufferReader, datumBatch,
        this::metricDatumFromEntry, AWS_MAX_DATUM_BATCH_SIZE,
        this::cloudWatchPutMetricData);
    }
  }

  private void cloudWatchPutMetricData(final Collection<MetricDatum> datumBatch) {
    final PutMetricDataRequest request = PutMetricDataRequest.builder()
          .namespace(this.namespace)
          .metricData(datumBatch)     // 1000/PutMetricData request.
          .build();

    final long now = TimeUtil.currentEpochMillis();
    final long startTime = System.nanoTime();
    try {
      cloudWatch.putMetricData(request);
      cloudWatchPutCount.update(now, 1);
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "AWS CloudWatch failure, discarding metric data: " + e.getMessage(), e);
      cloudWatchPutFailed.update(now, 1);
    } finally {
      final long elapsedNs = System.nanoTime() - startTime;
      cloudWatchPutTime.update(now, elapsedNs);
    }
  }

  private MetricDatum metricDatumFromEntry(final DatumBufferEntry entry) {
    return AwsCloudWatchUtil.metricDatumFromEntry(entry, dimensions);
  }
}
