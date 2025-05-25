#!/bin/bash
# Apply all serversage chnges

##################################################################
# VISUAL CUSTOMIZATION - Replace branding assets
##################################################################

cp -f img/fav32.png /usr/share/grafana/public/img/fav32.png
cp -f img/apple-touch-icon.png /usr/share/grafana/public/img/apple-touch-icon.png
cp -f img/grafana_icon.svg /usr/share/grafana/public/img/grafana_icon.svg
cp img/g8_login_dark.svg /usr/share/grafana/public/img/g8_login_dark.svg
cp img/g8_login_light.svg /usr/share/grafana/public/img/g8_login_light.svg
