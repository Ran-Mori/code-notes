package main

import (
	"context"
	"fmt"
	"google.golang.org/grpc/credentials/insecure"
	"log"
	"net/http"
	"time"

	pb "client/proto"

	"google.golang.org/grpc"
)

func main() {
	log.Println("Starting client...")

	// 當不使用docker時，target要改成"127.0.0.1:50051"，因為只有在docker damon創建的虛擬網絡中才能找到"server:50051"
	conn, err := grpc.NewClient("server:50051", grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("did not connect: %v", err)
	}
	defer func(conn *grpc.ClientConn) {
		err := conn.Close()
		if err != nil {
			log.Fatalf("could not close connection: %v", err)
		}
	}(conn)

	c := pb.NewTimeServiceClient(conn)

	http.HandleFunc("/time", func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received time request")
		ctx, cancel := context.WithTimeout(context.Background(), time.Second)
		defer cancel()
		resp, err := c.GetCurrentTime(ctx, &pb.TimeRequest{})
		if err != nil {
			log.Printf("Error getting time: %v", err)
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		log.Printf("Got time response: %d", resp.UnixTime)
		w.Write([]byte(fmt.Sprintf("%d", resp.UnixTime)))
	})

	log.Printf("client listening at :8080")
	if err := http.ListenAndServe(":8080", nil); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
