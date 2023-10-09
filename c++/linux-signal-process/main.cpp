#include <iostream>
#include <csignal>
#include <chrono>
#include <thread>

// Signal handler function
void signalHandler(int signalNumber) {
    std::cout << "Signal received: " << signalNumber << std::endl;
    // Perform necessary actions or cleanup here
    // ...
    // Terminate the program
    std::exit(signalNumber);
}

int main() {
    std::cout << "Hello, World!" << std::endl;

    // Register signal handler for SIGSYS
    std::signal(SIGSYS, signalHandler);

    std::cout << "Signal handling example. Press Ctrl+C to generate SIGINT." << std::endl;

    // Infinite loop to keep the program running until a signal is received
    for (int i = 0; i < 1000; ++i) {
        std::cout << "start sleep " << i + 1 << " second" << std::endl;
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }
    return 0;
}


