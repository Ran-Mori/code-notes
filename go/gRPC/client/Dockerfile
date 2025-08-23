FROM golang:1.25

WORKDIR /app

COPY . .

RUN go mod tidy
RUN go build -o client .

EXPOSE 8080

CMD ["./client"] 