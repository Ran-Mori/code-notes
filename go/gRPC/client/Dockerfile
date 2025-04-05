FROM golang:1.22-alpine

WORKDIR /app

#ENV GOPROXY=https://goproxy.cn,direct

COPY . .

RUN go mod tidy
RUN go build -o client .

EXPOSE 8080

CMD ["./client"] 