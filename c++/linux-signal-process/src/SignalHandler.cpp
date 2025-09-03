#include "SignalHandler.h"
#include <iostream>

SignalHandler::SignalHandler(SignalCallback callback) 
    : m_callback(std::move(callback)) {
}

void SignalHandler::handleSignal(int signal) {
    std::cout << "Signal received: " << signal << " (" << strsignal(signal) << ")" << std::endl;
    
    // Perform necessary cleanup operations
    std::cout << "Performing cleanup operations..." << std::endl;
    
    // Set exit flag instead of directly terminating the program
    m_shouldExit = true;
    
    // Call the user-provided callback if available
    if (m_callback) {
        m_callback(signal);
    }
}

bool SignalHandler::shouldExit() const {
    return m_shouldExit.load();
}

void SignalHandler::resetExitFlag() {
    m_shouldExit = false;
}
