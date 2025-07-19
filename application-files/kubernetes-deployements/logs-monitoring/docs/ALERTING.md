# Alerting Guide

This guide explains how to set up alerts in Grafana based on log data from Loki.

## Alert Types

Grafana supports two types of alerts with Loki:

1. **Metric Alerts**: Based on log volume or other metrics derived from logs
2. **Log Content Alerts**: Based on specific patterns or content in logs

## Setting Up Metric Alerts

### Log Volume Alerts

To create an alert for high log volume:

1. In Grafana, navigate to Alerting > Alert rules > New alert rule
2. Select "Loki" as the data source
3. Use a query like: `sum(count_over_time({job="fluentbit", namespace="production"}[5m])) > 1000`
4. Set the evaluation interval and duration
5. Configure notification channels
6. Save the alert

### Error Rate Alerts

To alert on high error rates:

1. Create a new alert rule
2. Use a query like: `sum(count_over_time({job="fluentbit", level="error"}[5m])) / sum(count_over_time({job="fluentbit"}[5m])) > 0.05`
3. This alerts when error logs exceed 5% of total logs
4. Configure the remaining settings and save

## Setting Up Log Content Alerts

### Specific Error Pattern Alerts

To alert on specific error patterns:

1. Create a new alert rule
2. Use a query like: `count_over_time({job="fluentbit"} |~ "OutOfMemory"[5m]) > 0`
3. This alerts when "OutOfMemory" appears in any log
4. Configure the remaining settings and save

### Multiple Pattern Alerts

For more complex patterns:

1. Create a new alert rule
2. Use a query like: `count_over_time({job="fluentbit"} |~ "error|exception|fail" !~ "expected|handled"[5m]) > 10`
3. This alerts when error-related terms appear but aren't marked as expected or handled
4. Configure the remaining settings and save

## Notification Channels

To receive alert notifications:

1. In Grafana, navigate to Alerting > Contact points
2. Click "New contact point"
3. Select the type (Email, Slack, PagerDuty, etc.)
4. Configure the settings for your notification channel
5. Save the contact point

## Alert Templates

You can customize alert notifications using templates:

```
{{ define "custom_message" }}
Alert: {{ .Alert.Name }}
Severity: {{ .Alert.Labels.severity }}
Summary: {{ .Alert.Annotations.summary }}
{{ end }}
```

## Testing Alerts

To test your alerts:

1. Deploy the test application: `kubectl apply -f manifests/test-apps/test-logger.yaml`
2. The application generates INFO, DEBUG, and ERROR logs
3. Verify that your alerts trigger appropriately

## Best Practices

1. **Reduce noise**: Set appropriate thresholds to avoid alert fatigue
2. **Group related alerts**: Use labels to group related alerts
3. **Include context**: Add helpful information in alert messages
4. **Set severity levels**: Differentiate between critical and non-critical alerts
5. **Test thoroughly**: Ensure alerts trigger when expected and don't trigger when not expected
