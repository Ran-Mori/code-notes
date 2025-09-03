#include <iostream>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <queue>
#include <chrono>
#include <csignal>
#include <unistd.h>

// --- Hardware Interrupt Simulation ---

// A buffer to hold scan codes from the "keyboard"
std::queue<int> keyboard_buffer;
std::mutex buffer_mutex;
std::condition_variable buffer_cv;

// "Top Half" - The Interrupt Service Routine (ISR) for the keyboard
void keyboard_isr(int scancode) {
    {
        std::lock_guard<std::mutex> lock(buffer_mutex);
        std::cout << "\n>> [ISR - Top Half] Hardware interrupt received! Scancode: " << scancode << ". Placing in buffer." << std::endl;
        keyboard_buffer.push(scancode);
    }
    buffer_cv.notify_one(); // Notify the "bottom half"
}

// Simulates a hardware device (keyboard) that generates interrupts
void hardware_device_simulator() {
    int scancode = 0;
    while (true) {
        // Simulate waiting for a key press
        std::this_thread::sleep_for(std::chrono::seconds(3));
        
        // Issue an "interrupt"
        keyboard_isr(scancode++);
    }
}

// "Bottom Half" - A kernel thread that processes the keyboard buffer
void bottom_half_handler() {
    while (true) {
        std::unique_lock<std::mutex> lock(buffer_mutex);
        buffer_cv.wait(lock, [] { return !keyboard_buffer.empty(); });

        int scancode = keyboard_buffer.front();
        keyboard_buffer.pop();
        lock.unlock();

        std::cout << "<< [Bottom Half] Processing scancode: " << scancode << " -> Passing to foreground app." << std::endl;
    }
}

// --- Software Interrupt Simulation ---

// The "kernel's" handler for the software interrupt (system call)
void software_interrupt_handler(int signum) {
    std::cout << "\n== [Kernel] Software interrupt received (System Call)! Performing requested service..." << std::endl;
    // Simulate doing some kernel work, like file I/O
    std::cout << "== [Kernel] Service complete. Returning to user mode." << std::endl;
}


int main() {
    std::cout << "CPU starting..." << std::endl;

    // --- Setup Interrupt Handlers ---
    // Register the software interrupt handler for SIGUSR1
    signal(SIGUSR1, software_interrupt_handler);

    // --- Start Kernel/Hardware Threads ---
    std::thread hardware_thread(hardware_device_simulator);
    hardware_thread.detach();

    std::thread bottom_half_thread(bottom_half_handler);
    bottom_half_thread.detach();

    // Main CPU loop - representing a user-space process
    int instruction_count = 0;
    while (true) {
        std::cout << "User space: Executing instruction " << ++instruction_count << "..." << std::endl;
        std::this_thread::sleep_for(std::chrono::milliseconds(500));

        // Every 5 instructions, make a "system call"
        if (instruction_count % 5 == 0) {
            std::cout << "User space: Making a system call to the kernel..." << std::endl;
            raise(SIGUSR1); // Trigger the software interrupt
        }
    }

    return 0;
}