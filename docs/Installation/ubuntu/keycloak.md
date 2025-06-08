Here's a **step-by-step guide to install Keycloak 25.0.1 as a systemd service on Ubuntu** with the specified environment configuration.

---

## ✅ Prerequisites

* Ubuntu 22.04 or later
* Java 17+ installed
* A non-root user with `sudo` privileges
* [Keycloak 25.0.1 ZIP distribution](https://github.com/keycloak/keycloak/releases/tag/25.0.1)

---

## 📦 Step 1: Install Java 17

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
java -version
```

---

## 📁 Step 2: Create a Keycloak User

```bash
sudo useradd --system --create-home --home-dir /opt/keycloak --shell /bin/false keycloak
```

---

## ⬇️ Step 3: Download and Install Keycloak 25.0.1

```bash
cd /tmp
wget https://github.com/keycloak/keycloak/releases/download/25.0.1/keycloak-25.0.1.zip
sudo apt install unzip -y
unzip keycloak-25.0.1.zip
sudo mv keycloak-25.0.1 /opt/keycloak/keycloak-25.0.1
sudo ln -s /opt/keycloak/keycloak-25.0.1 /opt/keycloak/current
sudo chown -R keycloak:keycloak /opt/keycloak
```

---

## ⚙️ Step 4: Create the Configuration Script

Create a startup environment script:

```bash
sudo nano /opt/keycloak/keycloak.env
```

Add the following content (replace `YOUR_PUBLIC_URL`):

```env
KC_HOSTNAME=YOUR_PUBLIC_URL (e.g. localhost)
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KC_HOSTNAME_STRICT=false
KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true
KC_HTTP_ENABLED=true
KC_METRICS_ENABLED=true
KC_HTTP_METRICS_HISTOGRAMS_ENABLED=true
KC_CACHE_METRICS_HISTOGRAMS_ENABLED=true
KC_HTTP_METRICS_SLOS=5,10,25,50,250,500,1000,2500,5000,10000
KC_EVENT_METRICS_USER_ENABLED=true
KC_EVENT_METRICS_USER_EVENTS=login,logout,register,update_profile,reset_password,user_disabled_by_temporary_lockout,user_disabled_by_permanent_lockout
KC_EVENT_METRICS_USER_TAGS=realm,clientId,idp
```

Set permissions:

```bash
sudo chown keycloak:keycloak /opt/keycloak/keycloak.env
chmod 600 /opt/keycloak/keycloak.env
```

---

## 🛠 Step 5: Create Systemd Service File

```bash
sudo nano /etc/systemd/system/keycloak.service
```

Paste this content:

```ini
[Unit]
Description=Keycloak 25.0.1
After=network.target

[Service]
Type=simple
User=keycloak
Group=keycloak
EnvironmentFile=/opt/keycloak/keycloak.env
WorkingDirectory=/opt/keycloak/current
ExecStart=/opt/keycloak/current/bin/kc.sh start
Restart=on-failure
TimeoutStartSec=60

[Install]
WantedBy=multi-user.target
```

---

## 🚀 Step 6: Enable and Start Keycloak

```bash
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl enable keycloak
sudo systemctl start keycloak
```

Check the service:

```bash
sudo systemctl status keycloak
```

Logs:

```bash
journalctl -u keycloak -f
```

---

## 🌐 Step 7: Access Keycloak

Visit:

```text
http://YOUR_PUBLIC_URL:8080/ (e.g. http://localhost:8080)
```

Login with:

* **Username**: `admin`
* **Password**: `admin`

---

