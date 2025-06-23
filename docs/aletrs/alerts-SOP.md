This SOP provides a step-by-step guide on how to set up alerts in ServerSage using Prometheus queries, including configuring email and Telegram as notification endpoints.

## Standard Operating Procedure: Setting Up Alerts in ServerSage (ServerSage)

### 1\. Prerequisites

  * **ServerSage Instance:** A running ServerSage instance (version 8.0+ recommended for unified alerting).
  * **Prometheus Data Source:** Prometheus configured as a data source in ServerSage, and it is actively collecting metrics from your servers.
      * **To add Prometheus as a data source:**
        1.  In ServerSage, navigate to **Configuration (gear icon)** \> **Data Sources**.
        2.  Click **Add data source** and select **Prometheus**.
        3.  Enter the URL of your Prometheus server (e.g., `http://localhost:9090`).
        4.  Click **Save & Test** to ensure the connection is successful.
  * **Access Credentials:** Ensure you have appropriate ServerSage user permissions to create and manage alert rules and contact points.

### 2\. Understanding ServerSage Alerts

The following alerts are used in ServerSage. We will use these as examples for setting up in ServerSage.

1.  **High CPU Utilization**

      * **Prom Query:** `100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[30s])))`
      * **Threshold:** `60 < X`
      * **Summary:** High CPU usage detected on instance `{{ $labels.instance }}`
      * **Description:** The CPU utilisation of instance `{{ $labels.instance }}` has been above 60 % for the last 1 minutes, current value is `{{ $values.A }} %`.

2.  **High System Load**

      * **Prom Query:** `node_load5`
      * **Threshold:** `60 < X`
      * **Summary:** The System Load of instance has been above 60 for the last 5 minutes in Demo environment. Current Value: `{{ $values.A }}`
      * **Description:** System Load has exceeded 10 for the last 5 minutes.

3.  **HighDiskUsage**

      * **Prom Query:** `(max(100 - ((node_filesystem_avail_bytes * 100) / node_filesystem_size_bytes)) by (instance))`
      * **Threshold:** `90 < X`
      * **Summary:** High Disk Usage on `{{ $labels.instance }}`. Critical: High Disk Usage on `{{ $labels.instance }}`
      * **Description:** Disk usage on `{{ $labels.instance }}` is `{{ $value.h }}%`. This is above the threshold of 90%.

4.  **High Memory Usage**

      * **Prom Query:** `(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes)/ (node_memory_MemTotal_bytes ) * 100`
      * **Threshold:** `85 < X`
      * **Summary:** Suggested Actions:
        1.  Check the processes consuming the most memory using tools like `top` or `htop`.
        2.  Review application logs for memory leaks or excessive memory allocation.
        3.  Consider restarting the application or service if necessary.
        4.  If the problem persists, evaluate the need for additional memory resources.
            Troubleshooting:
        <!-- end list -->
          * Connect to the instance: `ssh {{ $labels.instance }}`
          * Check memory usage: `free -m` or `htop`
            Please take immediate action to resolve this issue. Instance: `{{ $labels.instance }}`
      * **Description:** The memory usage on this node has exceeded the defined threshold above 85%. Please investigate immediately. on: `{{ $labels.instance }}`

5.  **PostgreSQL Database Down**

      * **Prom Query:** `pg_up`
      * **Threshold:** `0.5 > X`
      * **Summary:** Database is down for instance `{{ $labels.instance }}`
      * **Description:** Instance: `{{ $labels.instance }}`, job: `{{ $labels.job }}`, Timestamp: `{{ $time }}`, Details: The PostgreSQL instance is currently not reachable. The `pg_up` metric has reported a value of 0, indicating that the database is down.

6.  **Node Down**

      * **Prom Query:** `group by(instance, job) (up)`
      * **Threshold:** `0.5 > X`
      * **Summary:** Triggered when the node is unreachable (`up{}` == 0 for 1 minutes)
      * **Description:** Triggered when the node is unreachable (`up{}` == 0 for 1 minutes)

7.  **System Reboot Detected**

      * **Prom Query:** `(time() - node_boot_time_seconds)`
      * **Threshold:** `300 > X`
      * **Summary:** 🔥 ALERT\! System Rebooted: `{{ $labels.instance }}`, Machine `{{ $labels.instance }}` has recently rebooted. Current uptime: `{{ printf "%.0f" (time() - $values.A.Value) }}` seconds
      * **Description:** Triggered when the node is unreachable (`up{}` == 0 for 1 minutes)

8.  **Pod Reboot Detected**

      * **Prom Query:** `sum by (namespace, pod, container) (increase(kube_pod_container_status_restarts_total[1m]))`
      * **Threshold:** `0 < X`
      * **Summary:** Pod `{{ $labels.namespace }}/{{ $labels.pod }}` container(s) restarted
      * **Description:** Container(s) in pod `{{ $labels.pod }}` in namespace `{{ $labels.namespace }}` have restarted recently. Current restart count: `{{ $value }}`

9.  **kafka lag**

      * **Prom Query:** `max by (consumergroup, topic) (kafka_consumergroup_lag) > 1000`
      * **Threshold:** `1000 < X`
      * **Summary:** High Kafka consumer lag detected for group '{{ $labels.consumergroup }}' on topic '{{ $labels.topic }}'
      * **Description:** The Kafka consumer group '{{ $labels.group }}' is experiencing high lag on topic '{{ $labels.topic }}' (partition {{ $labels.partition }}). Current lag is {{ $values.A }}. This indicates messages are not being processed quickly enough.


### 3\. Setting up Notification Endpoints (Contact Points)

ServerSage uses "Contact Points" to define where alert notifications are sent.

#### 3.1. Email Configuration

To send email notifications, you need to configure SMTP settings in ServerSage's `ServerSage.ini` configuration file (for ServerSage OSS).

1.  **Access ServerSage Configuration File:**

      * Locate the `ServerSage.ini` file. It's typically found in the `conf` directory of your ServerSage installation (e.g., `/etc/ServerSage/ServerSage.ini` on Linux).

2.  **Edit SMTP Settings:**

      * Open the `ServerSage.ini` file with a text editor.
      * Find the `[smtp]` section.
      * Uncomment and configure the following parameters:
        ```ini
        [smtp]
        enabled = true
        host = your_smtp_server:587  ; e.g., smtp.gmail.com:587 or your_mail_server_ip:25
        user = your_smtp_username    ; e.g., your_email@example.com
        password = your_smtp_password
        ; If you are using Gmail, you might need an App Password.
        from_address = ServerSage@yourdomain.com
        from_name = ServerSage Alerts
        ; skip_verify = true        ; Uncomment for testing, but not recommended for production.
        ; starttls_policy = opportunistic ; Set to 'mandatory' for strict TLS, 'opportunistic' for flexible.
        ```

3.  **Save and Restart ServerSage:**

      * Save the `ServerSage.ini` file.
      * Restart the ServerSage service to apply the changes. The command depends on your OS (e.g., `sudo systemctl restart ServerSage-server` for systemd-based systems).

4.  **Create Email Contact Point in ServerSage:**

      * In ServerSage, navigate to **Alerts & IRM (bell icon)** \> **Contact points**.
      * Click **+ Add contact point**.
      * **Name:** `Email Alerts` (or a descriptive name)
      * **Integration:** Select `Email`.
      * **Addresses:** Enter the email addresses to receive notifications, separated by semicolons (e.g., `user1@example.com;user2@example.com`).
      * (Optional) **Optional Email Settings:** You can customize subject and message using templates if desired.
      * Click **Test** and then **Send test notification** to verify the setup.
      * Click **Save contact point**.

#### 3.2. Telegram Configuration

1.  **Create a Telegram Bot:**

      * Open the Telegram app and search for `@BotFather`.
      * Start a chat with `BotFather` and type `/newbot`.
      * Follow the prompts to choose a name and username for your bot. The username must end in `bot` or `_bot` (e.g., `MyServerSageAlertsBot`).
      * `BotFather` will provide you with an **HTTP API Token**. Copy this token; you'll need it for ServerSage.

2.  **Get your Chat ID:**

      * Add your newly created bot to a group chat where you want to receive alerts. (You can also send a message directly to your bot for a personal chat ID).
      * Send a dummy message to the bot (e.g., `/start`).
      * Open the following URL in your web browser, replacing `{YOUR_BOT_API_TOKEN}` with the token you got from `BotFather`:
        `https://api.telegram.org/bot{YOUR_BOT_API_TOKEN}/getUpdates`
      * In the JSON response, look for the `"chat"` object and find the `"id"` value. This is your **Chat ID**. It will likely be a negative number if it's a group chat (e.g., `-123456789`).

3.  **Create Telegram Contact Point in ServerSage:**

      * In ServerSage, navigate to **Alerts & IRM (bell icon)** \> **Contact points**.
      * Click **+ Add contact point**.
      * **Name:** `Telegram Alerts` (or a descriptive name)
      * **Integration:** Select `Telegram`.
      * **BOT API Token:** Paste the API token you obtained from `BotFather`.
      * **Chat ID:** Paste the Chat ID you obtained.
      * (Optional) You can customize the message, `parse_mode` (e.g., `HTML`, `MarkdownV2`), and `disable_web_page_preview`.
      * Click **Test** and then **Send test notification** to verify the setup.
      * Click **Save contact point**.

### 4\. Setting up Alert Rules

Now, let's create the alert rules in ServerSage based on the ServerSage alerts.

1.  **Navigate to Alert Rules:**

      * In ServerSage, navigate to **Alerts & IRM (bell icon)** \> **Alert rules**.
      * Click **+ New alert rule**.

2.  **Configure Alert Details (for each alert type):**

    For **High CPU Utilization (Example):**

      * **Rule name:** `High CPU Utilization`

      * **Folder:** Choose an existing folder or create a new one (e.g., `ServerSage Alerts`).

      * **Rule type:** `ServerSage managed alert` (recommended for querying multiple data sources, though here we're only using Prometheus).

      * **Data source:** Select your Prometheus data source.

      * **Query (Section A):**

          * Enter the Prom Query: `100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[30s])))`
          * **Legend:** `{{instance}} CPU Usage` (optional, for better visualization in ServerSage panels)

      * **Condition (Section B):**

          * **Expression:** `A` (refers to the query result from Section A)
          * **WHEN:** `is above`
          * **Threshold:** `60`

      * **Evaluation behavior:**

          * **Evaluation group:** Create a new group or use an existing one (e.g., `ServerSage Evaluation`).
          * **Evaluate every:** `1m` (as the description mentions "last 1 minutes")
          * **For:** `1m` (the condition must be true for 1 minute before firing)

      * **No Data and Error Handling:**

          * **If no data or an error occurs when evaluating:** `No Data` (e.g., `Alerting`) - configure as per your preference. `Keep last state` is often a good default to avoid flapping.

      * **Annotations and labels:**

          * **Summary:** `High CPU usage detected on instance {{ $labels.instance }}`
          * **Description:** The CPU utilisation of instance `{{ $labels.instance }}` has been above 60 % for the last 1 minutes, current value is `{{ $values.A }} %`.
          * **Labels:**
              * `severity: critical` (or `warning` based on your internal classification)
              * `service: server`
              * `alert_type: cpu`

      * **Notifications:**

          * Under **Notifications**, click **Select contact point**.
          * Choose your previously created contact points (e.g., `Email Alerts`, `Telegram Alerts`). You can select multiple.

      * Click **Save rule and exit**.

    **Repeat these steps for all ServerSage alerts, adjusting the Prom Query, Threshold, Summary, Description, and Labels as per the provided details.**

    **Important Considerations for each Alert:**

      * **High System Load:**

          * **Prom Query:** `node_load5`
          * **Condition:** `is above` `60`
          * **Evaluation:** `Evaluate every: 1m`, `For: 5m` (as per the description "for the last 5 minutes")
          * **Summary:** The System Load of instance has been above 60 for the last 5 minutes in Demo environment. Current Value: `{{ $values.A }}`
          * **Description:** System Load has exceeded 10 for the last 5 minutes.
          * **Labels:** `severity: warning`, `service: server`, `alert_type: load`

      * **HighDiskUsage:**

          * **Prom Query:** `(max(100 - ((node_filesystem_avail_bytes * 100) / node_filesystem_size_bytes)) by (instance))`
          * **Condition:** `is above` `90`
          * **Summary:** High Disk Usage on `{{ $labels.instance }}`. Critical: High Disk Usage on `{{ $labels.instance }}`
          * **Description:** Disk usage on `{{ $labels.instance }}` is `{{ $values.A }}%`. This is above the threshold of 90%. (Note: changed `{{ $value.h }}` to `{{ $values.A }}` as `A` is the default output from the query section).
          * **Labels:** `severity: critical`, `service: server`, `alert_type: disk`

      * **High Memory Usage:**

          * **Prom Query:** `(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes)/ (node_memory_MemTotal_bytes ) * 100`
          * **Condition:** `is above` `85`
          * **Summary:** (Copy the entire suggested actions and troubleshooting from the provided summary).
          * **Description:** The memory usage on this node has exceeded the defined threshold above 85%. Please investigate immediately. on: `{{ $labels.instance }}`
          * **Labels:** `severity: critical`, `service: server`, `alert_type: memory`

      * **PostgreSQL Database Down:**

          * **Prom Query:** `pg_up`
          * **Condition:** `is below` `0.5`
          * **Evaluation:** `Evaluate every: 1m`, `For: 1m` (to ensure it's truly down, not a momentary glitch)
          * **Summary:** Database is down for instance `{{ $labels.instance }}`
          * **Description:** Instance: `{{ $labels.instance }}`, job: `{{ $labels.job }}`, Timestamp: `{{ $time }}`, Details: The PostgreSQL instance is currently not reachable. The `pg_up` metric has reported a value of 0, indicating that the database is down.
          * **Labels:** `severity: critical`, `service: database`, `db_type: postgresql`

      * **Node Down:**

          * **Prom Query:** `group by(instance, job) (up)`
          * **Condition:** `is below` `0.5`
          * **Evaluation:** `Evaluate every: 1m`, `For: 1m` (as per the summary "for 1 minutes")
          * **Summary:** Triggered when the node is unreachable (`up{}` == 0 for 1 minutes)
          * **Description:** Triggered when the node is unreachable (`up{}` == 0 for 1 minutes)
          * **Labels:** `severity: critical`, `service: infrastructure`, `alert_type: node_status`

      * **System Reboot Detected:**

          * **Prom Query:** `(time() - node_boot_time_seconds)`
          * **Condition:** `is below` `300` (meaning uptime is less than 300 seconds or 5 minutes)
          * **Evaluation:** `Evaluate every: 1m`, `For: 1m`
          * **Summary:** 🔥 ALERT\! System Rebooted: `{{ $labels.instance }}`, Machine `{{ $labels.instance }}` has recently rebooted. Current uptime: `{{ printf "%.0f" (time() - $values.A.Value) }}` seconds
          * **Description:** Triggered when the node is unreachable (`up{}` == 0 for 1 minutes)
          * **Labels:** `severity: informational`, `service: infrastructure`, `alert_type: reboot`

      * **Pod Reboot Detected:**

          * **Prom Query:** `sum by (namespace, pod, container) (increase(kube_pod_container_status_restarts_total[1m]))`
          * **Condition:** `is above` `0`
          * **Evaluation:** `Evaluate every: 1m`, `For: 1m`
          * **Summary:** Pod `{{ $labels.namespace }}/{{ $labels.pod }}` container(s) restarted
          * **Description:** Container(s) in pod `{{ $labels.pod }}` in namespace `{{ $labels.namespace }}` have restarted recently. Current restart count: `{{ $values.A }}` (Note: Changed `$value` to `$values.A` for consistency with ServerSage templating)
          * **Labels:** `severity: warning`, `service: kubernetes`, `alert_type: pod_restart`

### 5\. Notification Policies (Optional but Recommended)

Notification policies define how alerts are routed to contact points based on labels. This is useful for sending specific alerts to different teams or channels.

1.  **Navigate to Notification Policies:**

      * In ServerSage, navigate to **Alerts & IRM (bell icon)** \> **Notification policies**.

2.  **Edit Default Policy or Create New:**

      * You can edit the **Default policy** to send all alerts to a common contact point, or create new **Specific routing** policies.

3.  **Configure Specific Routing (Example for High CPU Utilization):**

      * Click **+ New specific routing**.
      * **Matching Labels:**
          * `severity = critical`
          * `alert_type = cpu`
      * **Contact Point:** Select `Email Alerts` (or your preferred contact point for critical CPU alerts).
      * Click **Save policy**.

    You can create similar specific routings for other alerts, directing them to the most relevant contact points (e.g., send database alerts to a "DBA Team" Telegram channel).

### 6\. Monitoring and Management

  * **View Active Alerts:** Navigate to **Alerts & IRM (bell icon)** \> **Alerts** to see the current state of your alerts (Firing, Pending, Inactive).
  * **Silence Alerts:** If you are performing maintenance or aware of an ongoing issue, you can create a **Silence** to temporarily prevent notifications for specific alerts. Navigate to **Alerts & IRM (bell icon)** \> **Silences**.
  * **Review Alert History:** ServerSage stores alert state changes, which can be viewed in the alert details.
  * **Dashboard Integration:** You can add an "Alert list" panel to your dashboards to display active alerts related to the dashboard's data.

### 7\. Troubleshooting

  * **No Alerts Firing:**
      * Check your Prometheus data source connection in ServerSage.
      * Verify your Prom Query in a ServerSage dashboard panel to ensure it returns data.
      * Double-check the threshold and evaluation interval in the alert rule.
      * Ensure the Prometheus metrics are being collected correctly.
  * **Notifications Not Received:**
      * Verify your SMTP settings in `ServerSage.ini` and restart ServerSage.
      * Test the contact point directly from ServerSage's Contact Points page.
      * For Telegram, ensure the bot is in the correct group and the Chat ID is accurate.
      * Check your email spam folder.
      * Review ServerSage logs for any errors related to sending notifications.
  * **Alert Flapping:**
      * Adjust the "For" duration in your alert rule to require the condition to be true for a longer period before firing. This reduces sensitivity to momentary spikes.

By following this SOP, you can effectively set up and manage alerts in ServerSage for your ServerSage monitoring, ensuring timely notifications for critical system events.