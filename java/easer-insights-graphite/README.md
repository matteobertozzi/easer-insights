# Graphite Exporter

You can use the Graphite Json Exporter to export your data. *Grafana Cloud* has direct support for it.
```java
try (EaserInsights insights = EaserInsights.INSTANCE.open()) {
  final String url = "https://graphite-{...}.grafana.net/graphite/metrics";
  final String userId = "123456";
  final String token = "glc_xyz...";
  insights.addExporter(GraphiteExporter.newGraphiteExporter(url, userId, token));
  ...
}
```