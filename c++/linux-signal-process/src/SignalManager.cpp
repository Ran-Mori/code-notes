#include "SignalManager.h"
#include <iostream>
#include <algorithm>

SignalManager& SignalManager::getInstance() {
    static SignalManager instance;
    return instance;
}

bool SignalManager::registerSignal(int signal, std::shared_ptr<SignalHandler> handler) {
    if (!handler) {
        std::cerr << "Error: Cannot register null signal handler for signal " << signal << std::endl;
        return false;
    }
    
    // Store the handler
    m_handlers[signal] = handler;
    
    // Register with the system
    void (*result)(int) = std::signal(signal, internalSignalHandler);
    if (result == SIG_ERR) {
        std::cerr << "Error: Failed to register signal handler for signal " << signal << std::endl;
        m_handlers.erase(signal);
        return false;
    }
    
    std::cout << "Successfully registered signal handler: " << signal << std::endl;
    return true;
}

bool SignalManager::unregisterSignal(int signal) {
    auto it = m_handlers.find(signal);
    if (it == m_handlers.end()) {
        std::cerr << "Warning: Signal " << signal << " is not registered" << std::endl;
        return false;
    }
    
    // Unregister from the system
    void (*result)(int) = std::signal(signal, SIG_DFL);
    if (result == SIG_ERR) {
        std::cerr << "Error: Failed to unregister signal handler for signal " << signal << std::endl;
        return false;
    }
    
    m_handlers.erase(it);
    std::cout << "Successfully unregistered signal handler: " << signal << std::endl;
    return true;
}

bool SignalManager::isSignalRegistered(int signal) const {
    return m_handlers.find(signal) != m_handlers.end();
}

std::shared_ptr<SignalHandler> SignalManager::getHandler(int signal) const {
    auto it = m_handlers.find(signal);
    return (it != m_handlers.end()) ? it->second : nullptr;
}

std::vector<int> SignalManager::getRegisteredSignals() const {
    std::vector<int> signals;
    signals.reserve(m_handlers.size());
    
    for (const auto& pair : m_handlers) {
        signals.push_back(pair.first);
    }
    
    return signals;
}

void SignalManager::clearAllSignals() {
    for (const auto& pair : m_handlers) {
        std::signal(pair.first, SIG_DFL);
    }
    m_handlers.clear();
    std::cout << "Cleared all registered signal handlers" << std::endl;
}

void SignalManager::internalSignalHandler(int signal) {
    SignalManager& manager = getInstance();
    auto handler = manager.getHandler(signal);
    
    if (handler) {
        handler->handleSignal(signal);
    } else {
        std::cerr << "Warning: Received signal " << signal << " but no handler is registered" << std::endl;
    }
}
