# The build context is the repository root, because the build checks types against the contract.
# Keep the Node version equal to .nvmrc; CI fails if they differ.
FROM node:26.13.0-alpine AS build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json frontend/.npmrc ./
RUN npm ci --no-audit --no-fund
COPY contracts/openapi.yaml ../contracts/openapi.yaml
COPY frontend/ ./
RUN npm run build

# This image runs nginx as a non-root user, listening on 8080.
# It stays on nginx's stable line (even minor version) on purpose, not mainline.
FROM nginxinc/nginx-unprivileged:1.30.6-alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/dist /usr/share/nginx/html
EXPOSE 8080
