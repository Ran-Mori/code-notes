#ifndef SIGNALMANAGER_H
#define SIGNALMANAGER_H

#include <csignal>
#include <memory>
#include <vector>
#include <unordered_map>
#include "SignalHandler.h"

/**
 * @brief Manages signal registration and handling
 * 
 * This class is responsible for registering signal handlers
 * and managing the relationship between signals and their handlers.
 */
class SignalManager {
public:
    /**
     * @brief Get the singleton instance
     * @return Reference to the SignalManager instance
     */
    static SignalManager& getInstance();
    
    /**
     * @brief Register a signal handler
     * @param signal The signal number to handle
     * @param handler Shared pointer to the signal handler
     * @return true if registration was successful, false otherwise
     */
    bool registerSignal(int signal, std::shared_ptr<SignalHandler> handler);
    
    /**
     * @brief Unregister a signal handler
     * @param signal The signal number to unregister
     * @return true if unregistration was successful, false otherwise
     */
    bool unregisterSignal(int signal);
    
    /**
     * @brief Check if a signal is registered
     * @param signal The signal number to check
     * @return true if signal is registered, false otherwise
     */
    bool isSignalRegistered(int signal) const;
    
    /**
     * @brief Get the handler for a specific signal
     * @param signal The signal number
     * @return Shared pointer to the handler, or nullptr if not found
     */
    std::shared_ptr<SignalHandler> getHandler(int signal) const;
    
    /**
     * @brief Get all registered signals
     * @return Vector of registered signal numbers
     */
    std::vector<int> getRegisteredSignals() const;
    
    /**
     * @brief Clear all registered signals
     */
    void clearAllSignals();

private:
    SignalManager() = default;
    ~SignalManager() = default;
    SignalManager(const SignalManager&) = delete;
    SignalManager& operator=(const SignalManager&) = delete;
    
    std::unordered_map<int, std::shared_ptr<SignalHandler>> m_handlers;
    
    /**
     * @brief Internal signal handler function
     * @param signal The signal number received
     */
    static void internalSignalHandler(int signal);
};

#endif // SIGNALMANAGER_H
