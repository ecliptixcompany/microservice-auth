# Microservice Auth System - Makefile
# Docker-based production-ready authentication microservices
# No local Maven/Java required - everything runs in Docker

.PHONY: all build clean start stop restart status logs \
        start-infra stop-infra start-all stop-all \
        build-services logs-auth logs-mail logs-errors \
        test help

# Renkler
GREEN := $(shell printf "\033[0;32m")
RED := $(shell printf "\033[0;31m")
YELLOW := $(shell printf "\033[0;33m")
BLUE := $(shell printf "\033[0;34m")
NC := $(shell printf "\033[0m")

SHELL := /bin/bash

# Portlar
DISCOVERY_PORT := 8761
GATEWAY_PORT := 8080
AUTH_PORT := 8081
MAIL_PORT := 8082

# ============================================
# ANA KOMUTLAR
# ============================================

help: ## Yardım menüsü
	@echo ""
	@echo "$(BLUE)Microservice Auth System - Docker Komut Listesi$(NC)"
	@echo "$(BLUE)=================================================$(NC)"
	@echo "$(YELLOW)Not: Tüm servisler Docker ile çalışır - Maven/Java gerektirmez!$(NC)"
	@echo ""
	@echo "$(GREEN)Ana Komutlar (Hızlı Başlangıç):$(NC)"
	@echo "  make start          - Tüm sistemi başlat (infra + services)"
	@echo "  make stop           - Tüm sistemi durdur"
	@echo "  make restart        - Tüm sistemi yeniden başlat"
	@echo "  make status         - Servis durumlarını göster"
	@echo ""
	@echo "$(GREEN)Build & Deploy Komutları:$(NC)"
	@echo "  make build          - Tüm Docker image'larını build et"
	@echo "  make rebuild        - Cache kullanmadan yeniden build et"
	@echo "  make clean          - Tüm container ve image'ları temizle"
	@echo ""
	@echo "$(GREEN)Altyapı Komutları:$(NC)"
	@echo "  make start-infra    - Altyapı başlat (DB, RabbitMQ, Redis, MailHog)"
	@echo "  make stop-infra     - Altyapı durdur"
	@echo ""
	@echo "$(GREEN)Monitoring Komutları:$(NC)"
	@echo "  make start-monitoring - Grafana + Loki başlat"
	@echo "  make stop-monitoring  - Grafana + Loki durdur"
	@echo ""
	@echo "$(GREEN)Log Komutları:$(NC)"
	@echo "  make logs           - Tüm container loglarını göster"
	@echo "  make logs-auth      - Auth Service loglarını takip et"
	@echo "  make logs-mail      - Mail Service loglarını takip et"
	@echo "  make logs-discovery - Discovery Server loglarını takip et"
	@echo "  make logs-gateway   - API Gateway loglarını takip et"
	@echo "  make logs-errors    - Error loglarını göster"
	@echo ""
	@echo "$(YELLOW)Erişim URL'leri:$(NC)"
	@echo "  Eureka Dashboard:   http://localhost:8761"
	@echo "  API Gateway:        http://localhost:8080"
	@echo "  Auth Service:       http://localhost:8081"
	@echo "  Mail Service:       http://localhost:8082"
	@echo "  RabbitMQ:           http://localhost:15672 (guest/guest)"
	@echo "  MailHog:            http://localhost:8025"
	@echo "  Grafana:            http://localhost:3001 (admin/admin123)"
	@echo ""

all: build ## Build et

# ============================================
# BUILD KOMUTLARI
# ============================================

build: ## Docker image'larını build et
	@echo "$(BLUE)► Docker image'ları build ediliyor...$(NC)"
	@mkdir -p logs
	@docker-compose build discovery-server api-gateway auth-service mail-service
	@echo "$(GREEN)✓ Build tamamlandı$(NC)"

rebuild: ## Cache kullanmadan yeniden build et
	@echo "$(BLUE)► Docker image'ları cache olmadan build ediliyor...$(NC)"
	@mkdir -p logs
	@docker-compose build --no-cache discovery-server api-gateway auth-service mail-service
	@echo "$(GREEN)✓ Rebuild tamamlandı$(NC)"

clean: ## Tüm container, image ve volume'ları temizle
	@echo "$(BLUE)► Temizleniyor...$(NC)"
	@docker-compose down -v --remove-orphans
	@docker system prune -f
	@rm -rf logs/*.log
	@echo "$(GREEN)✓ Temizlendi$(NC)"

# ============================================
# ALTYAPI KOMUTLARI
# ============================================

start-infra: ## Docker altyapısını başlat
	@echo "$(BLUE)► Docker altyapısı başlatılıyor...$(NC)"
	@mkdir -p logs
	@docker-compose up -d auth-db rabbitmq mailhog redis
	@echo "$(GREEN)✓ PostgreSQL, RabbitMQ, MailHog, Redis başlatıldı$(NC)"
	@sleep 5
	@make infra-status

stop-infra: ## Docker altyapısını durdur
	@echo "$(BLUE)► Docker altyapısı durduruluyor...$(NC)"
	@docker-compose stop auth-db rabbitmq mailhog redis
	@echo "$(GREEN)✓ Altyapı durduruldu$(NC)"

# ============================================
# MONITORING KOMUTLARI (Grafana + Loki)
# ============================================

start-monitoring: ## Grafana + Loki monitoring başlat
	@echo "$(BLUE)► Monitoring stack başlatılıyor...$(NC)"
	@mkdir -p logs
	@docker-compose up -d loki promtail grafana
	@echo "$(GREEN)✓ Loki, Promtail, Grafana başlatıldı$(NC)"
	@echo ""
	@echo "$(YELLOW)Grafana: http://localhost:3001$(NC)"
	@echo "$(YELLOW)Kullanıcı: admin / Şifre: admin123$(NC)"

stop-monitoring: ## Grafana + Loki monitoring durdur
	@echo "$(BLUE)► Monitoring stack durduruluyor...$(NC)"
	@docker stop grafana promtail loki 2>/dev/null || true
	@docker rm grafana promtail loki 2>/dev/null || true
	@echo "$(GREEN)✓ Monitoring durduruldu$(NC)"

monitoring-status: ## Monitoring durumunu göster
	@echo ""
	@echo "$(BLUE)Monitoring Durumu:$(NC)"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "loki|promtail|grafana|NAMES" || echo "  Monitoring çalışmıyor"
	@echo ""

infra-status: ## Docker container durumları
	@echo ""
	@echo "$(BLUE)Docker Container Durumları:$(NC)"
	@docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "auth-db|rabbitmq|mailhog|redis|NAMES" || echo "  Çalışan container yok"
	@echo ""

# ============================================
# SERVİS KOMUTLARI (Docker-based)
# ============================================

start: start-infra ## Tüm sistemi başlat (infra + services)
	@echo ""
	@echo "$(BLUE)► Mikroservisler başlatılıyor...$(NC)"
	@mkdir -p logs
	@docker-compose up -d discovery-server api-gateway auth-service mail-service
	@echo "$(GREEN)✓ Tüm servisler başlatıldı$(NC)"
	@echo ""
	@echo "$(YELLOW)Servisler hazır olması bekleniyor (yaklaşık 60 saniye)...$(NC)"
	@sleep 10
	@make status

stop: ## Tüm servisleri durdur
	@echo "$(BLUE)► Servisler durduruluyor...$(NC)"
	@docker-compose stop discovery-server api-gateway auth-service mail-service
	@echo "$(GREEN)✓ Tüm servisler durduruldu$(NC)"

stop-all: ## Tüm sistemi durdur (servisler + altyapı + monitoring)
	@echo "$(BLUE)► Tüm sistem durduruluyor...$(NC)"
	@docker-compose down
	@echo "$(GREEN)✓ Tüm sistem durduruldu$(NC)"

restart: ## Tüm servisleri yeniden başlat
	@make stop
	@sleep 2
	@make start

# ============================================
# DURUM KONTROLÜ
# ============================================

status: ## Servis durumlarını göster
	@echo ""
	@echo "$(BLUE)SERVICE STATUS$(NC)"
	@echo "$(BLUE)=================================================================================$(NC)"
	@echo ""
	@printf "  %-20s " "Discovery Server:"; \
	if curl -s http://localhost:$(DISCOVERY_PORT) > /dev/null 2>&1; then \
		echo "$(GREEN)[OK]$(NC) http://localhost:$(DISCOVERY_PORT)"; \
	else \
		echo "$(RED)[DOWN]$(NC)"; \
	fi
	@printf "  %-20s " "API Gateway:"; \
	if curl -s http://localhost:$(GATEWAY_PORT)/actuator/health > /dev/null 2>&1; then \
		echo "$(GREEN)[OK]$(NC) http://localhost:$(GATEWAY_PORT)"; \
	else \
		echo "$(RED)[DOWN]$(NC)"; \
	fi
	@printf "  %-20s " "Auth Service:"; \
	if curl -s http://localhost:$(AUTH_PORT)/actuator/health > /dev/null 2>&1; then \
		echo "$(GREEN)[OK]$(NC) http://localhost:$(AUTH_PORT)"; \
	else \
		echo "$(RED)[DOWN]$(NC)"; \
	fi
	@printf "  %-20s " "Mail Service:"; \
	if curl -s http://localhost:$(MAIL_PORT)/actuator/health > /dev/null 2>&1; then \
		echo "$(GREEN)[OK]$(NC) http://localhost:$(MAIL_PORT)"; \
	else \
		echo "$(RED)[DOWN]$(NC)"; \
	fi
	@echo ""
	@make infra-status
	@make monitoring-status

# ============================================
# LOG KOMUTLARI
# ============================================

logs: ## Tüm container loglarını göster
	@echo "$(BLUE)► Container logları:$(NC)"
	@docker-compose logs --tail=50

logs-discovery: ## Discovery Server loglarını takip et
	@docker-compose logs -f discovery-server

logs-gateway: ## API Gateway loglarını takip et
	@docker-compose logs -f api-gateway

logs-auth: ## Auth Service loglarını takip et
	@docker-compose logs -f auth-service

logs-mail: ## Mail Service loglarını takip et
	@docker-compose logs -f mail-service

logs-errors: ## Tüm error loglarını göster (dosya bazlı)
	@echo "$(RED)=== Error Logs ===$(NC)"
	@echo ""
	@echo "$(YELLOW)--- Discovery Server Errors ---$(NC)"
	@tail -20 logs/discovery-server-error.log 2>/dev/null || echo "Error log yok"
	@echo ""
	@echo "$(YELLOW)--- API Gateway Errors ---$(NC)"
	@tail -20 logs/api-gateway-error.log 2>/dev/null || echo "Error log yok"
	@echo ""
	@echo "$(YELLOW)--- Auth Service Errors ---$(NC)"
	@tail -20 logs/auth-service-error.log 2>/dev/null || echo "Error log yok"
	@echo ""
	@echo "$(YELLOW)--- Mail Service Errors ---$(NC)"
	@tail -20 logs/mail-service-error.log 2>/dev/null || echo "Error log yok"

logs-clean: ## Eski log dosyalarını temizle
	@echo "$(BLUE)► Log dosyaları temizleniyor...$(NC)"
	@rm -f logs/*.log logs/*.log.gz
	@echo "$(GREEN)✓ Log dosyaları temizlendi$(NC)"
