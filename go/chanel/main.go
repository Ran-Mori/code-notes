package main

import (
	"fmt"
	"time"
)

func main() {
	done := make(chan bool, 2)

	go func() {
		fmt.Println("go coroutine 1 start")
		time.Sleep(time.Second * 2)
		fmt.Println("go coroutine 1 sleep over")
		done <- true
		fmt.Println("go coroutine 1 done <- true run")
	}()

	go func() {
		fmt.Println("go coroutine 2 start")
		time.Sleep(time.Second * 5)
		fmt.Println("go coroutine 2 sleep over")
		done <- true
		fmt.Println("go coroutine 2 done <- true run")
	}()

	var timeRemain = 4 * time.Second

	for i := 0; i < 2; i++ {
		fmt.Printf("start to selcet select index = %d, timeRemain = %d\n", i, timeRemain)
		var startTime = time.Now().UnixNano()
		select {
		case <-done:
			fmt.Printf("select happen index = %d, <-done\n", i)
		case <-time.After(timeRemain):
			fmt.Printf("select happen index = %d, time.After\n", i)
		}
		timeRemain -= time.Duration(time.Now().UnixNano() - startTime)
	}

	fmt.Printf("do other things, timeRemain = %d\n", timeRemain)

	time.Sleep(8 * time.Second)
}
