package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"redis-demo/redis"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

var redisServer *redis.RedisServer

func init() {
	// 初始化Redis服务器
	var err error
	redisServer, err = redis.NewRedisServer()
	if err != nil {
		log.Fatalf("启动Redis服务器失败: %v", err)
	}
}

func main() {
	r := gin.Default()

	// 配置CORS中间件
	config := cors.DefaultConfig()
	config.AllowOrigins = []string{"http://localhost:5173"}
	config.AllowMethods = []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"}
	config.AllowHeaders = []string{"Origin", "Content-Type"}
	r.Use(cors.New(config))

	// 设置路由
	r.GET("/api/keys", getAllKeys)
	r.GET("/api/key/:key", getValueByKey)
	r.DELETE("/api/keys", deleteAllKeys)
	r.DELETE("/api/key/:key", deleteKeyByKey)
	r.POST("/api/key", setKeyValue)

	// 优雅关闭
	go func() {
		// 监听中断信号
		quit := make(chan os.Signal, 1)
		signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
		<-quit

		log.Println("正在关闭Redis服务器...")
		if err := redisServer.Close(); err != nil {
			log.Printf("关闭Redis服务器时出错: %v", err)
		}
		os.Exit(0)
	}()

	// 启动服务器
	log.Println("后端服务器启动在 :8080 端口")
	r.Run(":8080")
}

// 获取所有键值对
func getAllKeys(c *gin.Context) {
	ctx := context.Background()
	client := redisServer.Client()

	// 获取所有键
	keys, err := client.Keys(ctx, "*").Result()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	// 获取所有键值对
	result := make(map[string]string)
	for _, key := range keys {
		val, err := client.Get(ctx, key).Result()
		if err != nil {
			continue
		}
		result[key] = val
	}

	c.JSON(http.StatusOK, result)
}

// 根据键获取值
func getValueByKey(c *gin.Context) {
	key := c.Param("key")
	ctx := context.Background()
	client := redisServer.Client()

	val, err := client.Get(ctx, key).Result()
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "key not found"})
		return
	}

	c.JSON(http.StatusOK, gin.H{key: val})
}

// 删除所有键值对
func deleteAllKeys(c *gin.Context) {
	ctx := context.Background()
	client := redisServer.Client()

	err := client.FlushDB(ctx).Err()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "all keys deleted"})
}

// 根据键删除值
func deleteKeyByKey(c *gin.Context) {
	key := c.Param("key")
	ctx := context.Background()
	client := redisServer.Client()

	result, err := client.Del(ctx, key).Result()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}
	if result == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "key not found"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "key deleted"})
}

// 设置键值对
func setKeyValue(c *gin.Context) {
	var data struct {
		Key   string `json:"key" binding:"required"`
		Value string `json:"value" binding:"required"`
	}

	if err := c.ShouldBindJSON(&data); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	ctx := context.Background()
	client := redisServer.Client()

	err := client.Set(ctx, data.Key, data.Value, 0).Err()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "key set successfully"})
}
