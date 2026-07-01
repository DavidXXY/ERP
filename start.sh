#!/bin/bash
set -e

echo "=== 启动 ERP 前后端 ==="

# Kill old processes
pkill -f 'spring-boot' 2>/dev/null || true
pkill -f 'vite' 2>/dev/null || true
sleep 1

# Clean H2
rm -f /private/tmp/ops-erp-h2/ops_erp.* 2>/dev/null || true
mkdir -p /private/tmp/ops-erp-h2

# Setup Java/Maven
export JAVA_HOME="/Users/davidxi/Documents/ERP/.dev/tools/jdk-17.0.19+10/Contents/Home"
export PATH="$JAVA_HOME/bin:/Users/davidxi/Documents/ERP/.dev/tools/apache-maven-3.9.16/bin:$PATH"
export SPRING_PROFILES_ACTIVE=local

cd /Users/davidxi/Documents/ERP

echo "启动后端 (Spring Boot)..."
mvn -f services/api/pom.xml spring-boot:run &

echo "启动前端 (Vite)..."
cd apps/admin && npx vite --host localhost --port 5174 &

sleep 5
echo ""
echo "✅ 后端: http://localhost:8080"
echo "✅ 前端: http://localhost:5174"
echo ""
echo "按 Ctrl+C 停止所有服务"
wait
