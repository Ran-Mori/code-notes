# C++

## linux-signal-process

### souce code

```c++
void(*signal(int, void (*)(int)))(int);
```

```c++
int main() {
  std::signal(SIGSYS, signalHandler);
}
```

### operation process

1. run the program

   ```bash
   clang++ main.cpp -o main.out;./main.out 
   ```

2. find the pid of the program

   ```bash
   ps -ef | grep main.out
   # output like below
   # UID   PID  PPID
   # 501  6830 94578   0  2:11PM ttys000    0:00.01 ./main.out
   ```

3. send signal

   ```bash
   kill -s SIGSYS 6830
   ```

***

