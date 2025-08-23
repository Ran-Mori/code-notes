# gRPC Time Service Example

This project demonstrates a simple gRPC service that provides the current Unix timestamp. It consists of two components:
- A gRPC server that provides the time service
- A gRPC client that exposes an HTTP endpoint to access the time service

## Prerequisites

Before running this project, you need to install the following dependencies on your macOS:

1. **Go**
   ```bash
   brew install go
   ```

2. **Protocol Buffers Compiler (protoc)**
   ```bash
   brew install protobuf
   ```

3. **Go Protocol Buffers Plugin**
   ```bash
   go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
   go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
   ```

4. **Docker and Docker Compose**
   ```bash
   brew install docker docker-compose
   ```

## Project Structure

```
gRPC/
├── client/                 # gRPC client service
│   ├── main.go            # Client implementation
│   ├── Dockerfile         # Client Docker configuration
│   ├── go.mod             # Client Go module
│   └── proto/             # Protocol buffer definitions
├── server/                # gRPC server service
│   ├── main.go            # Server implementation
│   ├── Dockerfile         # Server Docker configuration
│   ├── go.mod             # Server Go module
│   └── proto/             # Protocol buffer definitions
└── docker-compose.yml     # Docker Compose configuration
```

## Building and Running

1. **Generate Protocol Buffer Files**

   First, generate the protocol buffer files for both client and server:

   ```bash
   # Generate server proto files
   cd server
   protoc --go_out=. --go_opt=paths=source_relative --go-grpc_out=. --go-grpc_opt=paths=source_relative proto/time.proto

   # Generate client proto files
   cd ../client
   protoc --go_out=. --go_opt=paths=source_relative --go-grpc_out=. --go-grpc_opt=paths=source_relative proto/time.proto
   ```

2. **Build and Run with Docker Compose**

   ```bash
   # From the project root directory
   docker-compose up --build
   ```

   This will:
   - Build the server and client Docker images
   - Start the gRPC server on port 50051
   - Wait for the server to be healthy
   - Start the HTTP client on port 8080

   Note: The client service will only start after the server is fully ready and healthy.

3. **Test the Service**

   In a new terminal, you can test the service using curl:

   ```bash
   curl http://127.0.0.1:8080/time
   ```

   You should receive a response with the current Unix timestamp.

## Environment Configuration

The services are configured with the following environment variables:
- `GOPROXY=https://goproxy.cn,direct`: Ensures reliable Go module downloads

## Cleanup

To stop and remove all containers:

```bash
docker-compose down
```

## License

This project is open source and available under the MIT License. 