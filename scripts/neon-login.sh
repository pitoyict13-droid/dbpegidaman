#!/bin/bash
# Login Neon untuk mesin ini (IPv6 rusak di jaringan ini; Node harus pakai IPv4 dulu).
# Setelah dijalankan: buka printed "Auth Url" di browser & klik Authorize dalam 60 dtk.
cd "$(dirname "$0")/.."
export NODE_OPTIONS=--dns-result-order=ipv4first
neon login
