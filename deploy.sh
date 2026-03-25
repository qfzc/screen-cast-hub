#!/bin/bash

# Screen Cast Hub - Docker Deployment Script
# Usage: ./deploy.sh [command]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored message
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check if Docker is installed
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi

    if ! command -v docker &> /dev/null || ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
}

# Check if .env file exists
check_env() {
    if [ ! -f .env ]; then
        log_warn ".env file not found. Creating from .env.example..."
        cp .env.example .env
        log_info "Please edit .env file with your configuration."
        read -p "Press Enter to continue after editing .env..."
    fi
}

# Build all images
build() {
    log_info "Building Docker images..."
    docker compose build --no-cache
    log_success "Build completed!"
}

# Start all services
start() {
    check_env
    log_info "Starting services..."
    docker compose up -d
    log_success "Services started!"
    show_status
}

# Start with optional services (MinIO, Gotenberg)
start_full() {
    check_env
    log_info "Starting all services including optional ones..."
    docker compose --profile minio --profile gotenberg up -d
    log_success "All services started!"
    show_status
}

# Stop all services
stop() {
    log_info "Stopping services..."
    docker compose down
    log_success "Services stopped!"
}

# Restart all services
restart() {
    stop
    start
}

# Show status of all services
show_status() {
    log_info "Service Status:"
    echo ""
    docker compose ps
    echo ""
    log_info "Access URLs:"
    echo "  - Web Admin:     http://localhost:${WEB_ADMIN_PORT:-80}"
    echo "  - Backend API:   http://localhost:${BACKEND_PORT:-8080}"
    echo "  - API Docs:      http://localhost:${WEB_ADMIN_PORT:-80}/swagger-ui/index.html"
    echo "  - MySQL:         localhost:${MYSQL_PORT:-3306}"
    echo "  - Redis:         localhost:${REDIS_PORT:-6379}"
}

# Show logs
logs() {
    local service=$1
    if [ -z "$service" ]; then
        docker compose logs -f --tail=100
    else
        docker compose logs -f --tail=100 "$service"
    fi
}

# Clean up (remove volumes)
clean() {
    log_warn "This will remove all containers, images, and volumes!"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker compose down -v --rmi local
        log_success "Cleanup completed!"
    else
        log_info "Cleanup cancelled."
    fi
}

# Pull latest images
pull() {
    log_info "Pulling latest images..."
    docker compose pull
    log_success "Pull completed!"
}

# Production deployment
deploy_prod() {
    check_env
    log_info "Deploying in production mode..."
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
    log_success "Production deployment completed!"
    show_status
}

# Backup database
backup_db() {
    local backup_file="backup_$(date +%Y%m%d_%H%M%S).sql"
    log_info "Creating database backup: $backup_file"
    docker compose exec mysql mysqldump -u root -p"${DB_PASSWORD:-screencast123}" screen_cast_hub > "$backup_file"
    log_success "Backup created: $backup_file"
}

# Restore database
restore_db() {
    local backup_file=$1
    if [ -z "$backup_file" ]; then
        log_error "Please specify backup file: ./deploy.sh restore-db <backup_file>"
        exit 1
    fi
    log_info "Restoring database from: $backup_file"
    docker compose exec -T mysql mysql -u root -p"${DB_PASSWORD:-screencast123}" screen_cast_hub < "$backup_file"
    log_success "Database restored!"
}

# Show help
show_help() {
    echo "Screen Cast Hub - Docker Deployment Script"
    echo ""
    echo "Usage: ./deploy.sh [command]"
    echo ""
    echo "Commands:"
    echo "  build        Build all Docker images"
    echo "  start        Start all services"
    echo "  start-full   Start all services including optional (MinIO, Gotenberg)"
    echo "  stop         Stop all services"
    echo "  restart      Restart all services"
    echo "  status       Show status of all services"
    echo "  logs [svc]   Show logs (optionally for specific service)"
    echo "  clean        Remove all containers, images, and volumes"
    echo "  pull         Pull latest base images"
    echo "  deploy-prod  Deploy in production mode"
    echo "  backup-db    Create database backup"
    echo "  restore-db   Restore database from backup file"
    echo "  help         Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./deploy.sh start"
    echo "  ./deploy.sh logs backend"
    echo "  ./deploy.sh backup-db"
}

# Main
check_docker

case "$1" in
    build)
        build
        ;;
    start)
        start
        ;;
    start-full)
        start_full
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        show_status
        ;;
    logs)
        logs "$2"
        ;;
    clean)
        clean
        ;;
    pull)
        pull
        ;;
    deploy-prod)
        deploy_prod
        ;;
    backup-db)
        backup_db
        ;;
    restore-db)
        restore_db "$2"
        ;;
    help|--help|-h|"")
        show_help
        ;;
    *)
        log_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac
