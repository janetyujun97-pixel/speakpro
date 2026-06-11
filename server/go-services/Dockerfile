FROM golang:1.22-alpine AS builder
WORKDIR /app
# 国内服务器构建：使用 goproxy.cn 镜像，避免 proxy.golang.org 超时
ENV GOPROXY=https://goproxy.cn,direct
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o /server cmd/server/main.go

FROM alpine:3.19
WORKDIR /app
COPY --from=builder /server .
EXPOSE 8080
CMD ["./server"]
