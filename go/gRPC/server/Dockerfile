FROM golang:1.22-alpine

WORKDIR /app

#ENV GOPROXY=https://goproxy.io,direct

COPY . .

RUN go mod tidy
RUN go build -o server .

EXPOSE 50051

CMD ["./server"] 