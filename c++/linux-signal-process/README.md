## linux-signal-process

### build

```cmake
cmake -S . -B cmake-build-debug
cmake --build cmake-build-debug -j
```

### problems

* Clion's analyzer may also sometimes not synchronize the project structure correctly. just `Tools -> CMake -> Reload CMake Project`