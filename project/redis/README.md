# Redis Server Implementation

This package provides a wrapper around Redis server that allows for easy startup, management, and debugging of a Redis instance directly from Go code.

## Overview

The implementation includes:
- Automatic Redis server discovery and startup
- Configuration management
- Debug capabilities
- Monitoring tools
- Data persistence

## Prerequisites

You need either:
1. Redis installed on your system (via Homebrew or package manager)
2. Or use the included Redis source code in `redis-src/` directory (recommended for debugging)

## Directory Structure

```
redis/
├── redis-src/          # Redis source code for debugging
├── redis-data/         # Redis data directory (created automatically)
├── redis.conf         # Redis configuration file (created automatically)
├── redis.log          # Redis debug log file (created automatically)
├── server.go          # Main implementation
└── README.md          # This file
```

## Redis Configuration

The Redis server is configured with the following settings:

```conf
port 6379              # Default Redis port
dir ./redis-data       # Data directory for persistence
dbfilename dump.rdb    # RDB file for point-in-time snapshots
appendonly yes         # Enable AOF persistence
appendfilename "appendonly.aof"  # AOF file name

# Debug options
loglevel debug         # Maximum log verbosity
logfile "redis.log"    # Log file location
slowlog-log-slower-than 0       # Log all commands for debugging
latency-monitor-threshold 0     # Monitor all latency events
```

### Configuration Details

1. **Persistence Configuration**
   - RDB (Redis Database): Point-in-time snapshots of your dataset
   - AOF (Append Only File): Logs every write operation received by the server
   - Both mechanisms are enabled for maximum data safety

2. **Debug Configuration**
   - `loglevel debug`: Provides maximum logging information
   - `slowlog-log-slower-than 0`: Logs all commands regardless of execution time
   - `latency-monitor-threshold 0`: Monitors all operations for latency analysis

## Advanced Debugging with Source Code

### Modifying Redis Source Code for Debugging

You can add custom debug logs to Redis source code for deeper insights:

1. **Locate the Source File**
   ```bash
   cd redis-src/src
   ```
   Common files for modification:
   - `t_string.c`: String operations
   - `t_hash.c`: Hash operations
   - `t_list.c`: List operations
   - `t_set.c`: Set operations
   - `t_zset.c`: Sorted Set operations

2. **Add Debug Logs**
   Example of adding logs to string operations (`t_string.c`):
   ```c
   void setCommand(client *c) {
       // Add debug log
       printf("Debug: Setting key=%s, value=%s\n", 
           (char*)c->argv[1]->ptr, 
           (char*)c->argv[2]->ptr);
       
       // Original code continues...
   }
   ```

3. **Recompile Redis**
   ```bash
   cd redis-src
   make clean              # Clean previous build
   make CFLAGS="-g -O0"   # Compile with debug info
   ```

4. **Verify the Build**
   - New executable will be at `redis-src/src/redis-server`
   - The server will automatically use this version when started

5. **View Debug Output**
   - Logs will appear in both console and `redis.log`
   - Console output is handled by:
     ```go
     cmd.Stdout = os.Stdout
     cmd.Stderr = os.Stderr
     ```
   - File logging is configured in `redis.conf`:
     ```conf
     logfile "redis.log"
     ```

### Tips for Source Code Debugging

1. **Best Practices**
   - Add clear markers to debug logs (e.g., "Debug:", "Custom:")
   - Keep logs focused and meaningful
   - Consider performance impact of excessive logging
   - Clean up debug logs after investigation

2. **Useful Places for Logs**
   - Command processing functions
   - Data structure operations
   - Memory management
   - Client connections
   - Persistence operations

3. **Example Debug Points**
   ```c
   // Debug key expiration (expire.c)
   printf("Debug: Key=%s expired at %ld\n", key, time);

   // Debug memory allocation (zmalloc.c)
   printf("Debug: Allocated %zu bytes\n", size);

   // Debug command processing (server.c)
   printf("Debug: Processing command=%s, args=%d\n", cmd, argc);
   ```

## Usage

[... rest of the original README content ...] 