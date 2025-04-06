package redis

import (
	"context"
	"fmt"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"time"

	"github.com/redis/go-redis/v9"
)

// RedisServer 表示Redis服务器
type RedisServer struct {
	client *redis.Client
	cmd    *exec.Cmd
}

// findRedisServer 查找redis-server可执行文件
func findRedisServer() (string, error) {
	// 首先检查本地编译的redis-server
	localRedisServer := "redis-src/src/redis-server"
	if absPath, err := filepath.Abs(localRedisServer); err == nil {
		if _, err := os.Stat(absPath); err == nil {
			log.Println("findRedisServer use localRedisServer")
			return absPath, nil
		}
	}

	// 常见的Redis安装路径
	paths := []string{
		"/usr/local/bin/redis-server",
		"/usr/bin/redis-server",
		"/opt/homebrew/bin/redis-server",        // Homebrew on Apple Silicon
		"/usr/local/opt/redis/bin/redis-server", // Homebrew on Intel Mac
	}

	// 检查PATH中是否有redis-server
	if path, err := exec.LookPath("redis-server"); err == nil {
		return path, nil
	}

	// 检查常见安装路径
	for _, path := range paths {
		if _, err := os.Stat(path); err == nil {
			return path, nil
		}
	}

	return "", fmt.Errorf("找不到redis-server，请先安装Redis。\n" +
		"您可以通过以下方式安装：\n" +
		"1. 使用Homebrew：brew install redis\n" +
		"2. 或从源码安装：\n" +
		"   curl -O http://download.redis.io/redis-stable.tar.gz\n" +
		"   tar xvzf redis-stable.tar.gz\n" +
		"   cd redis-stable\n" +
		"   make\n" +
		"   sudo make install")
}

// NewRedisServer 创建并启动一个新的Redis服务器实例
func NewRedisServer() (*RedisServer, error) {
	// 查找redis-server
	redisServerPath, err := findRedisServer()
	if err != nil {
		return nil, err
	}

	// 获取当前工作目录
	workDir, err := os.Getwd()
	if err != nil {
		return nil, fmt.Errorf("获取工作目录失败: %v", err)
	}

	// 创建Redis数据目录
	dataDir := filepath.Join(workDir, "redis-data")
	if err := os.MkdirAll(dataDir, 0755); err != nil {
		return nil, fmt.Errorf("创建数据目录失败: %v", err)
	}

	// 准备Redis配置
	configPath := filepath.Join(workDir, "redis.conf")
	config := []byte(`
		port 6379
		dir ./redis-data
		dbfilename dump.rdb
		appendonly yes
		appendfilename "appendonly.aof"
		
		# 调试选项
		loglevel debug
		logfile "redis.log"
		slowlog-log-slower-than 0
		latency-monitor-threshold 0
	`)

	// 写入Redis配置文件
	if err := os.WriteFile(configPath, config, 0644); err != nil {
		return nil, fmt.Errorf("写入配置文件失败: %v", err)
	}

	// 启动Redis服务器
	cmd := exec.Command(redisServerPath, configPath)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Start(); err != nil {
		return nil, fmt.Errorf("启动Redis服务器失败: %v", err)
	}

	// 创建Redis客户端
	client := redis.NewClient(&redis.Options{
		Addr:     "localhost:6379",
		Password: "",
		DB:       0,
	})

	// 等待Redis服务器启动
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	// 尝试ping服务器
	for {
		if err := client.Ping(ctx).Err(); err == nil {
			break
		}
		select {
		case <-ctx.Done():
			cmd.Process.Kill()
			return nil, fmt.Errorf("Redis服务器启动超时")
		default:
			time.Sleep(100 * time.Millisecond)
		}
	}

	log.Printf("Redis服务器(%s)已启动，数据目录：%s", redisServerPath, dataDir)
	return &RedisServer{
		client: client,
		cmd:    cmd,
	}, nil
}

// Close 关闭Redis服务器
func (s *RedisServer) Close() error {
	if s.client != nil {
		s.client.Close()
	}
	if s.cmd != nil && s.cmd.Process != nil {
		return s.cmd.Process.Kill()
	}
	return nil
}

// Client 返回Redis客户端
func (s *RedisServer) Client() *redis.Client {
	return s.client
}

// GetDebugInfo 获取Redis服务器的调试信息
func (s *RedisServer) GetDebugInfo(ctx context.Context) (map[string]string, error) {
	info := make(map[string]string)

	// 获取慢查询日志
	slowLogs, err := s.client.SlowLogGet(ctx, 10).Result()
	if err == nil {
		for i, log := range slowLogs {
			info[fmt.Sprintf("slowlog_%d", i)] = fmt.Sprintf(
				"id=%d duration=%v args=%v",
				log.ID, log.Duration, log.Args,
			)
		}
	}

	// 获取内存使用情况
	memory, err := s.client.Info(ctx, "memory").Result()
	if err == nil {
		info["memory"] = memory
	}

	return info, nil
}

// GetRedisLog 读取Redis日志文件
func (s *RedisServer) GetRedisLog() (string, error) {
	workDir, err := os.Getwd()
	if err != nil {
		return "", fmt.Errorf("获取工作目录失败: %v", err)
	}

	logPath := filepath.Join(workDir, "redis.log")
	content, err := os.ReadFile(logPath)
	if err != nil {
		return "", fmt.Errorf("读取日志文件失败: %v", err)
	}

	return string(content), nil
}

// MonitorCommands 开始监控Redis命令
func (s *RedisServer) MonitorCommands(ctx context.Context) (<-chan string, error) {
	cmdChan := make(chan string, 100)

	go func() {
		defer close(cmdChan)

		// 创建一个新的连接用于MONITOR命令
		monitorClient := redis.NewClient(&redis.Options{
			Addr:     "localhost:6379",
			Password: "",
			DB:       0,
		})
		defer monitorClient.Close()

		// 执行MONITOR命令
		cmd := monitorClient.Do(ctx, "MONITOR")
		if cmd.Err() != nil {
			log.Printf("执行MONITOR命令失败: %v", cmd.Err())
			return
		}

		// 持续读取监控数据
		for {
			select {
			case <-ctx.Done():
				return
			default:
				cmd := monitorClient.Do(ctx, "")
				if cmd.Err() != nil {
					if cmd.Err() != redis.Nil {
						log.Printf("读取监控数据时出错: %v", cmd.Err())
					}
					return
				}
				if str, err := cmd.Text(); err == nil {
					cmdChan <- str
				}
			}
		}
	}()

	return cmdChan, nil
}
