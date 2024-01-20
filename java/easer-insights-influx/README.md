# Influx Exporter

You can use the Influx [Line Protocol](https://docs.influxdata.com/influxdb/cloud/reference/syntax/line-protocol/) Exporter to export your data.

```java
// Grafana Cloud with influx protocol support
try (EaserInsights insights = EaserInsights.INSTANCE.open()) {
  final String url = "https://influx-{...}.grafana.net/api/v1/push/influx/write";
  final String userId = "123456";
  final String token = "glc_xyz...";
  insights.addExporter(InfluxLineExporter.newInfluxExporter(url, userId, token));
  ...
}
```

```java
// Influx instance with bucket and orgID
try (EaserInsights insights = EaserInsights.INSTANCE.open()) {
  final String url = "http://localhost:8086/api/v2/write?precision=ns&bucket=AAAA&orgID=00000";
  final String token = "abcdef...";
  insights.addExporter(InfluxLineExporter.newInfluxExporter(url, token));
  ...
}
```