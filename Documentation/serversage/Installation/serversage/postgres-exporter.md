## 🖥️ Step 5: Install Postgres Exporter

1. Download and extract:

```bash
curl -LO https://github.com/prometheus-community/postgres_exporter/releases/download/v0.17.1/postgres_exporter-0.17.1.linux-amd64.tar.gz
tar xvf postgres_exporter-0.17.1.linux-amd64.tar.gz
cd postgres_exporter-0.17.1.linux-amd64
```

2. Move binary:

```bash
sudo cp postgres_exporter /usr/local/bin/
sudo chown serversage:serversage /usr/local/bin/postgres_exporter
```

3. Create systemd service:

```bash
sudo tee /etc/systemd/system/postgres_exporter.service > /dev/null <<EOF
[Unit]
Description=Postgres Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=serversage
ExecStart=/usr/local/bin/postgres_exporter

[Install]
WantedBy=default.target
EOF
```

Start and enable:

```bash
sudo systemctl daemon-reload
sudo systemctl enable postgres_exporter
sudo systemctl start postgres_exporter
```

---