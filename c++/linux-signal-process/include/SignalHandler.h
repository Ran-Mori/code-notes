#ifndef SIGNALHANDLER_H
#define SIGNALHANDLER_H

#include <csignal>
#include <atomic>
#include <functional>
#include <memory>

/**
 * @brief Abstract base class for signal handling
 * 
 * This class provides the interface for handling system signals
 * in a thread-safe manner using atomic operations.
 */
class SignalHandler {
public:
    using SignalCallback = std::function<void(int)>;
    
    /**
     * @brief Constructor
     * @param callback Function to call when signal is received
     */
    explicit SignalHandler(SignalCallback callback);
    
    /**
     * @brief Destructor
     */
    virtual ~SignalHandler() = default;
    
    /**
     * @brief Handle the received signal
     * @param signal The signal number received
     */
    void handleSignal(int signal);
    
    /**
     * @brief Check if exit has been requested
     * @return true if exit was requested, false otherwise
     */
    bool shouldExit() const;
    
    /**
     * @brief Reset the exit flag
     */
    void resetExitFlag();

private:
    std::atomic<bool> m_shouldExit{false};
    SignalCallback m_callback;
};

#endif // SIGNALHANDLER_H
