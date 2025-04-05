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

5. **Netcat (for health checks)**
   ```bash
   brew install netcat
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
   curl http://localhost:8080/time
   ```

   You should receive a response with the current Unix timestamp.

## Service Details

### gRPC Server
- Runs on port 50051
- Implements the TimeService defined in proto/time.proto
- Returns the current Unix timestamp when requested
- Includes health check to ensure service availability

### HTTP Client
- Runs on port 8080
- Exposes an HTTP endpoint at `/time`
- Forwards requests to the gRPC server
- Returns the Unix timestamp as a string
- Waits for server to be healthy before starting

## Environment Configuration

The services are configured with the following environment variables:
- `GOPROXY=https://goproxy.cn,direct`: Ensures reliable Go module downloads

## Troubleshooting

1. **If you encounter network issues:**
   - Make sure Docker is running
   - Check if ports 50051 and 8080 are available
   - Verify that the containers are running with `docker ps`
   - Check the health status with `docker-compose ps`

2. **If you encounter build issues:**
   - Make sure all prerequisites are installed
   - Check that the GOPATH is set correctly
   - Ensure protoc and its plugins are in your PATH
   - Verify that netcat is installed for health checks

3. **If the service is not responding:**
   - Check the Docker logs with `docker-compose logs`
   - Verify that both containers are running
   - Make sure there are no firewall issues
   - Check the health check status with `docker inspect <container_id>`

## Cleanup

To stop and remove all containers:

```bash
docker-compose down
```

## License

This project is open source and available under the MIT License. 