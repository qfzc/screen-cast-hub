# Build stage
FROM node:20-alpine AS builder

WORKDIR /build

# Copy package files first for better caching
COPY package*.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# Copy source and build
COPY . .
RUN npm run build

# Runtime stage with nginx
FROM nginx:alpine

# Copy built assets
COPY --from=builder /build/dist /usr/share/nginx/html

# Copy nginx configuration
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/default.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
