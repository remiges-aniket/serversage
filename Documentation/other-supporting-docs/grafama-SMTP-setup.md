1. go to path /etc/grafana where grafana is installed
2. open file grafana.ini for edit `nano` or `vi`
3. search for `smtp` in it
4. change the details as per requirement
5. note : if their is `no password` keep `user` part commented
6. after change `save` the file and restart grafana service `systemctl restart grafana-server.service`
7. now in grafana UI, go to `Alerts & IRM` -> `Alerting` -> `Contact points` -> `Edit` mode, and check while sending test mail if you are SMTP configuration if ok or not.


Here is a clear and updated **Standard Operating Procedure (SOP)** for configuring SMTP in Grafana:

---

# **SOP: Configuring SMTP in Grafana for Alert Notifications**

## **Objective**

To configure SMTP settings in Grafana so that email alerts can be sent successfully.

---

## **Prerequisites**

* Root or sudo privileges on the server running Grafana.
* Access to SMTP server details (host, port, user, password, etc.).

---

## **Procedure**

### **Step 1: Navigate to Grafana Configuration Directory**

```bash
cd /etc/grafana
```

---

### **Step 2: Open the Configuration File**

Edit the `grafana.ini` file using a text editor:

```bash
sudo nano grafana.ini
```

*Or*

```bash
sudo vi grafana.ini
```

---

### **Step 3: Locate the SMTP Configuration Section**

Search for the `[smtp]` section in the file. You can use search within the editor (e.g., `Ctrl+W` in `nano`, or `/smtp` in `vi`).

Example:

```ini
[smtp]
enabled = true
host = smtp.example.com:587
user = your-smtp-username
#password = your-smtp-password
from_address = alerts@example.com
from_name = Grafana Alerts
skip_verify = false
```

---

### **Step 4: Modify the SMTP Settings**

Update the fields according to your SMTP provider’s specifications.

* **enabled**: Set to `true`
* **host**: SMTP server and port (e.g., `smtp.gmail.com:587`)
* **user**: Username for SMTP (email address)
* **password**: Password or app password (uncomment only if needed)
* **from\_address**: Email address from which alerts will be sent
* **from\_name**: Display name for sender
* **skip\_verify**: Set to `false` to verify TLS certificate, `true` to skip (based on your setup)

> **Note:** If **no password is required**, leave `user` commented and **do not uncomment or set `password`**.

---

### **Step 5: Save and Close the File**

* In `nano`: Press `Ctrl+O`, then `Enter`, then `Ctrl+X`.
* In `vi`: Press `Esc`, type `:wq`, then press `Enter`.

---

### **Step 6: Restart Grafana Service**

Apply the changes by restarting the Grafana service:

```bash
sudo systemctl restart grafana-server.service
```

---

### **Step 7: Validate SMTP Configuration via Grafana UI**

1. Open Grafana in a web browser.
2. Go to:
   **Alerts & IRM → Alerting → Contact points**
3. Select or create a contact point using **Email**.
4. Enter the recipient address and click **"Send test"**.
5. Confirm whether the test email is received successfully.

---

## **Troubleshooting Tips**

* Check logs for errors:

  ```bash
  sudo journalctl -u grafana-server -f
  ```
* Ensure your firewall allows outbound connections to your SMTP server's port.
* Verify credentials and email settings with your SMTP provider.
* Some email providers (like Gmail) may require an **App Password** or less secure app access.

---

