package main

import (
	"context"
	"log"
	"net"
	"time"

	pb "server/proto"

	"google.golang.org/grpc"
)

type server struct {
	pb.UnimplementedTimeServiceServer
}

func (s *server) GetCurrentTime(ctx context.Context, req *pb.TimeRequest) (*pb.TimeResponse, error) {
	log.Println("Received GetCurrentTime request")
	unixTime := time.Now().Unix()
	log.Printf("Returning time: %d", unixTime)
	return &pb.TimeResponse{
		UnixTime: unixTime,
	}, nil
}

func main() {
	log.Println("Starting server...")
	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterTimeServiceServer(s, &server{})
	log.Printf("server listening at %v", lis.Addr())
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
