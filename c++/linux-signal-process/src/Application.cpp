#include "Application.h"
#include <iostream>
#include <thread>
#include <csignal>

Application::Application(int maxIterations, int sleepDuration)
    : m_maxIterations(maxIterations)
    , m_sleepDurationSeconds(sleepDuration)
    , m_signalManager(SignalManager::getInstance()) {
}

bool Application::initialize() {
    std::cout << "=== Linux Signal Handling Example Program ===" << std::endl;
    
    // Create signal handler with custom callback
    m_signalHandler = std::make_shared<SignalHandler>(
        [this](int signal) { this->onSignalReceived(signal); }
    );
    
    // Setup signal handlers
    if (!setupSignalHandlers()) {
        std::cerr << "Failed to setup signal handlers" << std::endl;
        return false;
    }
    
    std::cout << "Application initialized successfully" << std::endl;
    return true;
}

int Application::run() const {
    std::cout << "Program started. Press Ctrl+C to send SIGINT signal for graceful exit." << std::endl;
    std::cout << "Or use 'kill -TERM <pid>' to send SIGTERM signal." << std::endl;
    
    try {
        executeMainLoop();
    } catch (const std::exception& e) {
        std::cerr << "Exception caught in main loop: " << e.what() << std::endl;
        return 1;
    }
    
    return 0;
}

void Application::shutdown() const {
    std::cout << "Shutting down application..." << std::endl;
    performCleanup();
    m_signalManager.clearAllSignals();
    std::cout << "Application shutdown complete" << std::endl;
}

bool Application::shouldExit() const {
    return m_signalHandler && m_signalHandler->shouldExit();
}

bool Application::setupSignalHandlers() const {
    // Register SIGINT (Ctrl+C)
    if (!m_signalManager.registerSignal(SIGINT, m_signalHandler)) {
        std::cerr << "Failed to register SIGINT handler" << std::endl;
        return false;
    }
    
    // Register SIGTERM
    if (!m_signalManager.registerSignal(SIGTERM, m_signalHandler)) {
        std::cerr << "Failed to register SIGTERM handler" << std::endl;
        return false;
    }
    
    return true;
}

void Application::executeMainLoop() const {
    for (int iteration = 0; iteration < m_maxIterations && !shouldExit(); ++iteration) {
        std::cout << "Starting sleep for " << (iteration + 1) << " second(s)" << std::endl;
        std::this_thread::sleep_for(std::chrono::seconds(m_sleepDurationSeconds));
    }
    
    if (shouldExit()) {
        std::cout << "Program received exit signal, shutting down gracefully..." << std::endl;
    } else {
        std::cout << "Program completed all iterations normally." << std::endl;
    }
}

void Application::performCleanup() {
    std::cout << "Performing application cleanup..." << std::endl;
    // Add any additional cleanup logic here
}

void Application::onSignalReceived(int signal) {
    std::cout << "Application received signal: " << signal << std::endl;
    // Add any application-specific signal handling logic here
}
