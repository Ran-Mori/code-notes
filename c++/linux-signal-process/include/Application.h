#ifndef APPLICATION_H
#define APPLICATION_H

#include <memory>
#include <chrono>
#include "SignalHandler.h"
#include "SignalManager.h"

/**
 * @brief Main application class
 * 
 * This class represents the main application logic and coordinates
 * between different components like signal handling and the main loop.
 */
class Application {
public:
    /**
     * @brief Constructor
     * @param maxIterations Maximum number of iterations to run
     * @param sleepDuration Sleep duration between iterations in seconds
     */
    explicit Application(int maxIterations = 1000, int sleepDuration = 1);
    
    /**
     * @brief Destructor
     */
    ~Application() = default;
    
    /**
     * @brief Initialize the application
     * @return true if initialization was successful, false otherwise
     */
    bool initialize();
    
    /**
     * @brief Run the main application loop
     * @return Exit code (0 for success, non-zero for error)
     */
    int run() const;
    
    /**
     * @brief Shutdown the application gracefully
     */
    void shutdown() const;
    
    /**
     * @brief Check if the application should exit
     * @return true if exit was requested, false otherwise
     */
    bool shouldExit() const;

private:
    int m_maxIterations;
    int m_sleepDurationSeconds;
    std::shared_ptr<SignalHandler> m_signalHandler;
    SignalManager& m_signalManager;
    
    /**
     * @brief Setup signal handlers
     * @return true if setup was successful, false otherwise
     */
    bool setupSignalHandlers() const;
    
    /**
     * @brief Execute the main application loop
     */
    void executeMainLoop() const;
    
    /**
     * @brief Perform cleanup operations
     */
    static void performCleanup();
    
    /**
     * @brief Custom signal callback
     * @param signal The signal number received
     */
    static void onSignalReceived(int signal);
};

#endif // APPLICATION_H
