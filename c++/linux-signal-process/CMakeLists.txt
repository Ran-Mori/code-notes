cmake_minimum_required(VERSION 3.26)
project(linux-signal-process)

set(CMAKE_CXX_STANDARD 17)

# Set C++ standard features
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Add include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# Add source files
file(GLOB SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/src/*.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/main.cpp
)

# Add header files for IDE support
file(GLOB HEADERS
    ${CMAKE_CURRENT_SOURCE_DIR}/include/*.h
)

# Create executable
add_executable(linux-signal-process ${SOURCES} ${HEADERS})

# Set target properties
target_compile_features(linux-signal-process PRIVATE cxx_std_17)

# Enable all warnings
target_compile_options(linux-signal-process PRIVATE
    $<$<CXX_COMPILER_ID:GNU>:-Wall -Wextra -Wpedantic>
    $<$<CXX_COMPILER_ID:Clang>:-Wall -Wextra -Wpedantic>
    $<$<CXX_COMPILER_ID:MSVC>:/W4>
)
